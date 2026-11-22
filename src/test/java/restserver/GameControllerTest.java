package restserver;

import battleship.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import restserver.Dtos.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes unitários para GameController")
class GameControllerTest {

    private GameRegistry registry;
    private StubCallbackClient callbackClient;
    private StubAiStrategy aiStrategy;
    private GameController controller;

    // Stub para controlar o retorno do CallbackClient sem fazer chamadas HTTP reais
    static class StubCallbackClient extends CallbackClient {
        boolean shouldSucceed = true;
        boolean called = false;

        @Override
        public boolean sendShots(GameSession session, List<IPosition> aiShots) {
            called = true;
            return shouldSucceed;
        }
    }

    // Stub para controlar os tiros devolvidos pela IA
    static class StubAiStrategy extends AiShotStrategy {
        @Override
        public List<IPosition> computeShots(IGame game) {
            return List.of(new Position('A', 1), new Position('A', 2), new Position('A', 3));
        }
    }

    @BeforeEach
    void setUp() {
        registry = new GameRegistry();
        callbackClient = new StubCallbackClient();
        aiStrategy = new StubAiStrategy();
        controller = new GameController(registry, callbackClient, aiStrategy);
    }

    @Test
    @DisplayName("Registo com sucesso cria sessão e devolve HTTP 200 OK")
    void registerSuccess() {
        RegistrationRequest req = new RegistrationRequest();
        req.playerName = "Miguel";
        req.callbackUrl = "http://localhost:8081";

        ResponseEntity<?> response = controller.register(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(RegistrationResponse.class, response.getBody());
        RegistrationResponse body = (RegistrationResponse) response.getBody();
        assertNotNull(body.gameId);
        assertEquals("REGISTERED", body.status);
        assertEquals(Game.NUMBER_SHOTS, body.shotsPerTurn);
        assertTrue(body.message.contains("Miguel"));
    }

    @Test
    @DisplayName("Registo falha com 400 Bad Request quando playerName é nulo ou vazio")
    void registerInvalidPlayerName() {
        RegistrationRequest reqNull = new RegistrationRequest();
        reqNull.playerName = null;
        reqNull.callbackUrl = "http://localhost:8081";
        assertEquals(HttpStatus.BAD_REQUEST, controller.register(reqNull).getStatusCode());

        RegistrationRequest reqBlank = new RegistrationRequest();
        reqBlank.playerName = "   ";
        reqBlank.callbackUrl = "http://localhost:8081";
        assertEquals(HttpStatus.BAD_REQUEST, controller.register(reqBlank).getStatusCode());
    }

    @Test
    @DisplayName("Registo falha com 400 Bad Request quando callbackUrl é nulo ou vazio")
    void registerInvalidCallbackUrl() {
        RegistrationRequest reqNull = new RegistrationRequest();
        reqNull.playerName = "Miguel";
        reqNull.callbackUrl = null;
        assertEquals(HttpStatus.BAD_REQUEST, controller.register(reqNull).getStatusCode());

        RegistrationRequest reqBlank = new RegistrationRequest();
        reqBlank.playerName = "Miguel";
        reqBlank.callbackUrl = "";
        assertEquals(HttpStatus.BAD_REQUEST, controller.register(reqBlank).getStatusCode());
    }

    @Test
    @DisplayName("Receber tiros falha com 404 Not Found se o gameId não existir")
    void receiveShotsGameNotFound() {
        ShotRequest req = new ShotRequest();
        ResponseEntity<?> response = controller.receiveShots("id-inexistente", req);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("Receber tiros falha com 410 Gone se o jogo já terminou")
    void receiveShotsGameAlreadyOver() {
        GameSession session = registry.createSession("Miguel", "http://localhost:8081");
        session.markStudentWins();

        ShotRequest req = new ShotRequest();
        ResponseEntity<?> response = controller.receiveShots(session.getGameId(), req);
        assertEquals(HttpStatus.GONE, response.getStatusCode());
    }

    @Test
    @DisplayName("Receber tiros falha com 400 Bad Request se lista for nula ou tamanho incorreto")
    void receiveShotsInvalidShotCount() {
        GameSession session = registry.createSession("Miguel", "http://localhost:8081");

        ShotRequest reqNull = new ShotRequest();
        reqNull.shots = null;
        assertEquals(HttpStatus.BAD_REQUEST, controller.receiveShots(session.getGameId(), reqNull).getStatusCode());

        ShotRequest reqEmpty = new ShotRequest();
        reqEmpty.shots = new ArrayList<>();
        assertEquals(HttpStatus.BAD_REQUEST, controller.receiveShots(session.getGameId(), reqEmpty).getStatusCode());
    }

    @Test
    @DisplayName("Receber tiros falha com 400 Bad Request se alguma posição tiver linha ou coluna inválida")
    void receiveShotsInvalidPositions() {
        GameSession session = registry.createSession("Miguel", "http://localhost:8081");
        String id = session.getGameId();

        // Linha nula
        assertEquals(HttpStatus.BAD_REQUEST, controller.receiveShots(id, buildShotRequest(null, 1)).getStatusCode());
        // Linha em branco
        assertEquals(HttpStatus.BAD_REQUEST, controller.receiveShots(id, buildShotRequest(" ", 1)).getStatusCode());
        // Coluna < 1
        assertEquals(HttpStatus.BAD_REQUEST, controller.receiveShots(id, buildShotRequest("A", 0)).getStatusCode());
        // Coluna > 10
        assertEquals(HttpStatus.BAD_REQUEST, controller.receiveShots(id, buildShotRequest("A", 11)).getStatusCode());
    }

    @Test
    @DisplayName("Turno válido avalia tiros (MISS, HIT, SUNK, REPEATED, INVALID) e dispara callback da IA")
    void receiveShotsOngoingTurnAndOutcomes() {
        GameSession session = registry.createSession("Miguel", "http://localhost:8081");
        IGame game = session.getGame();

        // Limpar frota aleatória e colocar apenas uma Barca (tamanho 1) em A1 e uma Caravela (tamanho 2) em C1
        game.getMyFleet().getShips().clear();
        Barge barge = new Barge(Compass.NORTH, new Position('A', 1));
        Caravel caravel = new Caravel(Compass.EAST, new Position('C', 1));
        game.getMyFleet().addShip(barge);
        game.getMyFleet().addShip(caravel);

        // Construir tiros cobrindo vários desfechos (SUNK em A1, HIT em C1, REPEATED em A1, MISS em J10, INVALID em Z1)
        List<ShotPosition> shots = new ArrayList<>();
        shots.add(createShot("A", 1)); // SUNK (Barge)
        shots.add(createShot("C", 1)); // HIT (Caravel)
        if (Game.NUMBER_SHOTS >= 3) shots.add(createShot("A", 1)); // REPEATED
        while (shots.size() < Game.NUMBER_SHOTS) {
            shots.add(createShot("Z", 1)); // INVALID (linha fora do tabuleiro)
        }
        // Garantir pelo menos um MISS substituindo o último se houver espaço
        if (Game.NUMBER_SHOTS >= 4) {
            shots.set(Game.NUMBER_SHOTS - 1, createShot("J", 10)); // MISS
        }

        ShotRequest req = new ShotRequest();
        req.shots = shots;

        ResponseEntity<?> response = controller.receiveShots(session.getGameId(), req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ShotResponse body = (ShotResponse) response.getBody();
        assertNotNull(body);
        assertEquals("ONGOING", body.gameStatus);
        assertNull(body.winner);
        assertTrue(callbackClient.called);
    }

    @Test
    @DisplayName("Turno válido onde callback da IA falha continua a devolver 200 OK sem crashar")
    void receiveShotsCallbackFailsGracefully() {
        callbackClient.shouldSucceed = false;
        GameSession session = registry.createSession("Miguel", "http://localhost:8081");

        ShotRequest req = buildShotRequest("J", 10);
        ResponseEntity<?> response = controller.receiveShots(session.getGameId(), req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(callbackClient.called);
    }

    @Test
    @DisplayName("Estudante vence o jogo quando afunda o último navio da IA")
    void receiveShotsStudentWins() {
        GameSession session = registry.createSession("Miguel", "http://localhost:8081");
        IGame game = session.getGame();

        // Deixar apenas uma Barca de tamanho 1 em B2
        game.getMyFleet().getShips().clear();
        game.getMyFleet().addShip(new Barge(Compass.NORTH, new Position('B', 2)));

        List<ShotPosition> shots = new ArrayList<>();
        shots.add(createShot("B", 2)); // Afunda o único navio
        while (shots.size() < Game.NUMBER_SHOTS) {
            shots.add(createShot("H", 8));
        }

        ShotRequest req = new ShotRequest();
        req.shots = shots;

        ResponseEntity<?> response = controller.receiveShots(session.getGameId(), req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ShotResponse body = (ShotResponse) response.getBody();
        assertNotNull(body);
        assertEquals(0, body.shipsRemaining);
        assertEquals("GAME_OVER", body.gameStatus);
        assertEquals("STUDENT_WINS", body.winner);
        assertTrue(session.isGameOver());
        assertFalse(callbackClient.called); // IA não dispara se já perdeu
    }

    private ShotPosition createShot(String row, int col) {
        ShotPosition sp = new ShotPosition();
        sp.row = row;
        sp.column = col;
        return sp;
    }

    private ShotRequest buildShotRequest(String row, int col) {
        ShotRequest req = new ShotRequest();
        req.shots = new ArrayList<>();
        for (int i = 0; i < Game.NUMBER_SHOTS; i++) {
            req.shots.add(createShot(row, col));
        }
        return req;
    }
}