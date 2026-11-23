package restserver;

import battleship.*;
import restserver.Dtos.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Sends the AI's shots to the student's server (m2a) and reads back the results (m2b).
 */
@Component
public class CallbackClient {

	private static final int CONNECT_TIMEOUT_SECONDS = 5;
	private static final int REQUEST_TIMEOUT_SECONDS = 10;
	private static final int HTTP_OK_MIN = 200;
	private static final int HTTP_OK_MAX = 300;

	private static final String STATUS_GAME_OVER = "GAME_OVER";
	private static final String OUTCOME_INVALID = "INVALID";
	private static final String OUTCOME_REPEATED = "REPEATED";
	private static final String OUTCOME_SUNK = "SUNK";

	private final HttpClient   http;
	private final ObjectMapper mapper;

	public CallbackClient() {
		this.http   = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
				.build();
		this.mapper = new ObjectMapper();
	}

	public boolean sendShots(GameSession session, List<IPosition> aiShots) {
		String body = serializeShots(aiShots);
		if (body == null) {
			return false;
		}

		String url = session.getCallbackUrl() + "/game/" + session.getGameId() + "/shots";
		String responseBody = postCallbackRequest(url, body);
		if (responseBody == null) {
			return false;
		}

		ShotResponse m2b = parseResponse(responseBody);
		if (m2b == null) {
			return false;
		}

		recordAiMove(session.getGame(), aiShots, m2b);

		if (m2b.shipsRemaining == 0 || STATUS_GAME_OVER.equals(m2b.gameStatus)) {
			session.markAiWins();
			System.out.printf("[%s] GAME OVER — AI wins against '%s'!%n",
					session.getGameId(), session.getPlayerName());
		}

		return true;
	}

	private String serializeShots(List<IPosition> aiShots) {
		ShotRequest dto = new ShotRequest();
		dto.shots = new ArrayList<>();
		for (IPosition pos : aiShots) {
			ShotPosition sp = new ShotPosition();
			sp.row    = String.valueOf(pos.getClassicRow());
			sp.column = pos.getClassicColumn();
			dto.shots.add(sp);
		}

		try {
			return mapper.writeValueAsString(dto);
		} catch (IOException e) {
			System.err.println("[Callback] Failed to serialize AI shots: " + e.getMessage());
			return null;
		}
	}

	private String postCallbackRequest(String url, String body) {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Content-Type", "application/json")
				.timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();

		HttpResponse<String> response;
		try {
			response = http.send(request, HttpResponse.BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			System.err.println("[Callback] HTTP call to student failed: " + e.getMessage());
			return null;
		}

		if (response.statusCode() < HTTP_OK_MIN || response.statusCode() >= HTTP_OK_MAX) {
			System.err.printf("[Callback] Student returned HTTP %d: %s%n",
					response.statusCode(), response.body());
			return null;
		}

		return response.body();
	}

	private ShotResponse parseResponse(String responseBody) {
		try {
			return mapper.readValue(responseBody, ShotResponse.class);
		} catch (IOException e) {
			System.err.println("[Callback] Failed to parse student m2b response: " + e.getMessage());
			return null;
		}
	}

	private void recordAiMove(IGame game, List<IPosition> shots, ShotResponse m2b) {
		if (m2b.results == null) {
			return;
		}

		List<IGame.ShotResult> shotResults = new ArrayList<>();
		for (int i = 0; i < shots.size() && i < m2b.results.size(); i++) {
			ShotResult r = m2b.results.get(i);

			boolean valid    = !OUTCOME_INVALID.equals(r.outcome);
			boolean repeated = OUTCOME_REPEATED.equals(r.outcome);
			boolean sunk     = OUTCOME_SUNK.equals(r.outcome);

			shotResults.add(new IGame.ShotResult(valid, repeated, null, sunk));
		}

		Move move = new Move(game.getAlienMoves().size() + 1, shots, shotResults);
		game.getAlienMoves().add(move);
	}
}