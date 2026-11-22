package restserver;

import battleship.*;
import restserver.Dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * REST Controller — exposes the two endpoints of the Battleship protocol.
 *
 *   POST /register                    ->  m0  register a player
 *   POST /game/{gameId}/shots         ->  m1a receive student shots, fire back (m1b)
 *                                         then call student callback (m2a/m2b) internally
 */
@RestController
public class GameController {

	private static final int MIN_COLUMN = 1;
	private static final int MAX_COLUMN = 10;

	private static final String STATUS_REGISTERED = "REGISTERED";
	private static final String STATUS_ONGOING = "ONGOING";
	private static final String STATUS_GAME_OVER = "GAME_OVER";
	private static final String WINNER_STUDENT = "STUDENT_WINS";

	private static final String OUTCOME_INVALID = "INVALID";
	private static final String OUTCOME_REPEATED = "REPEATED";
	private static final String OUTCOME_MISS = "MISS";
	private static final String OUTCOME_SUNK = "SUNK";
	private static final String OUTCOME_HIT = "HIT";

	private final GameRegistry registry;
	private final CallbackClient callbackClient;
	private final AiShotStrategy aiStrategy;

	public GameController(GameRegistry registry,
						  CallbackClient callbackClient,
						  AiShotStrategy aiStrategy) {
		this.registry       = registry;
		this.callbackClient = callbackClient;
		this.aiStrategy     = aiStrategy;
	}

	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody RegistrationRequest request) {
		if (request.playerName == null || request.playerName.isBlank()) {
			return error(HttpStatus.BAD_REQUEST, "playerName is required");
		}
		if (request.callbackUrl == null || request.callbackUrl.isBlank()) {
			return error(HttpStatus.BAD_REQUEST, "callbackUrl is required");
		}

		GameSession session = registry.createSession(request.playerName, request.callbackUrl);

		RegistrationResponse response = new RegistrationResponse();
		response.gameId       = session.getGameId();
		response.status       = STATUS_REGISTERED;
		response.shotsPerTurn = session.getShotsPerTurn();
		response.message      = "Welcome, " + request.playerName + "! Game ready. You shoot first.";

		System.out.printf("[%s] Registered player '%s' (callback: %s)%n",
				session.getGameId(), request.playerName, request.callbackUrl);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/game/{gameId}/shots")
	public ResponseEntity<?> receiveShots(@PathVariable String gameId,
										  @RequestBody ShotRequest request) {
		GameSession session = registry.getSession(gameId);
		ResponseEntity<ErrorResponse> sessionError = validateSession(gameId, session);
		if (sessionError != null) {
			return sessionError;
		}

		if (request.shots == null || request.shots.size() != Game.NUMBER_SHOTS) {
			return error(HttpStatus.BAD_REQUEST,
					"Exactly " + Game.NUMBER_SHOTS + " shots required per turn");
		}

		List<IPosition> positions = new ArrayList<>();
		ResponseEntity<ErrorResponse> positionError = parseAndValidatePositions(request.shots, positions);
		if (positionError != null) {
			return positionError;
		}

		IGame game = session.getGame();
		ShotResponse m1bResponse = evaluateShots(positions, game);

		if (game.getRemainingShips() == 0) {
			session.markStudentWins();
			m1bResponse.gameStatus = STATUS_GAME_OVER;
			m1bResponse.winner     = WINNER_STUDENT;
			System.out.printf("[%s] GAME OVER — student '%s' wins!%n",
					gameId, session.getPlayerName());
			return ResponseEntity.ok(m1bResponse);
		}

		triggerAiTurn(gameId, session, game);
		return ResponseEntity.ok(m1bResponse);
	}

	private ResponseEntity<ErrorResponse> validateSession(String gameId, GameSession session) {
		if (session == null) {
			return error(HttpStatus.NOT_FOUND, "Game not found: " + gameId);
		}
		if (session.isGameOver()) {
			return error(HttpStatus.GONE, "Game " + gameId + " is already over. Winner: " + session.getWinner());
		}
		return null;
	}

	private ResponseEntity<ErrorResponse> parseAndValidatePositions(List<ShotPosition> shotDtos,
																	List<IPosition> targetPositions) {
		for (ShotPosition sp : shotDtos) {
			if (sp.row == null || sp.row.isBlank() || sp.column < MIN_COLUMN || sp.column > MAX_COLUMN) {
				return error(HttpStatus.BAD_REQUEST,
						"Invalid position: row='" + sp.row + "' column=" + sp.column);
			}
			targetPositions.add(new Position(sp.row.toUpperCase().charAt(0), sp.column));
		}
		return null;
	}

	private void triggerAiTurn(String gameId, GameSession session, IGame game) {
		List<IPosition> aiShots = aiStrategy.computeShots(game);
		boolean callbackOk = callbackClient.sendShots(session, aiShots);

		if (!callbackOk) {
			System.err.printf("[%s] WARNING: callback to '%s' failed. AI shots skipped.%n",
					gameId, session.getCallbackUrl());
		}
	}

	private ShotResponse evaluateShots(List<IPosition> positions, IGame game) {
		List<ShotResult> results = new ArrayList<>();
		List<IPosition> alreadyThisTurn = new ArrayList<>();

		for (IPosition pos : positions) {
			boolean repeatedInTurn = alreadyThisTurn.contains(pos);
			IGame.ShotResult sr = game.fireSingleShot(pos, repeatedInTurn);
			alreadyThisTurn.add(pos);

			results.add(buildShotResultDto(pos, sr));
		}

		ShotResponse response  = new ShotResponse();
		response.results       = results;
		response.shipsRemaining = game.getRemainingShips();
		response.gameStatus    = STATUS_ONGOING;
		response.winner        = null;
		return response;
	}

	private ShotResult buildShotResultDto(IPosition pos, IGame.ShotResult sr) {
		ShotResult dto = new ShotResult();
		dto.row        = String.valueOf(pos.getClassicRow());
		dto.column     = pos.getClassicColumn();

		if (!sr.valid()) {
			dto.outcome = OUTCOME_INVALID;
		} else if (sr.repeated()) {
			dto.outcome = OUTCOME_REPEATED;
		} else if (sr.ship() == null) {
			dto.outcome = OUTCOME_MISS;
		} else if (sr.sunk()) {
			dto.outcome  = OUTCOME_SUNK;
			dto.shipType = sr.ship().getCategory();
		} else {
			dto.outcome  = OUTCOME_HIT;
			dto.shipType = sr.ship().getCategory();
		}
		return dto;
	}

	private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(new ErrorResponse(message));
	}
}