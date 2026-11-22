package restserver;

import battleship.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import restserver.Dtos.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes unitários para CallbackClient")
class CallbackClientTest {

    private HttpServer server;
    private String baseUrl;
    private CallbackClient client;
    private GameRegistry registry;
    private final ObjectMapper mapper = new ObjectMapper();

    private int responseStatus = 200;
    private String responseBody = "";

    @BeforeEach
    void setUp() throws IOException {
        client = new CallbackClient();
        registry = new GameRegistry();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Envio com sucesso processa resultados (HIT, SUNK, REPEATED, INVALID) e regista jogada da IA")
    void sendShotsSuccessOngoing() throws Exception {
        GameSession session = registry.createSession("Miguel", baseUrl);
        List<IPosition> aiShots = List.of(
                new Position('A', 1),
                new Position('A', 2),
                new Position('A', 3),
                new Position('Z', 9)
        );

        ShotResponse m2b = new ShotResponse();
        m2b.shipsRemaining = 3;
        m2b.gameStatus = "ONGOING";
        m2b.results = List.of(
                createResult("A", 1, "HIT"),
                createResult("A", 2, "SUNK"),
                createResult("A", 3, "REPEATED"),
                createResult("Z", 9, "INVALID")
        );

        responseStatus = 200;
        responseBody = mapper.writeValueAsString(m2b);

        boolean ok = client.sendShots(session, aiShots);

        assertTrue(ok);
        assertFalse(session.isGameOver());
        assertEquals(1, session.getGame().getAlienMoves().size());
    }

    @Test
    @DisplayName("IA vence quando shipsRemaining é 0 ou gameStatus é GAME_OVER")
    void sendShotsAiWins() throws Exception {
        // Caso 1: shipsRemaining == 0
        GameSession session1 = registry.createSession("Miguel", baseUrl);
        ShotResponse m2bZeroShips = new ShotResponse();
        m2bZeroShips.shipsRemaining = 0;
        m2bZeroShips.gameStatus = "ONGOING";
        m2bZeroShips.results = new ArrayList<>();

        responseStatus = 200;
        responseBody = mapper.writeValueAsString(m2bZeroShips);

        assertTrue(client.sendShots(session1, List.of(new Position('B', 2))));
        assertTrue(session1.isGameOver());
        assertEquals("AI_WINS", session1.getWinner());

        // Caso 2: gameStatus == "GAME_OVER" com shipsRemaining > 0 e results == null
        GameSession session2 = registry.createSession("Miguel", baseUrl);
        ShotResponse m2bGameOver = new ShotResponse();
        m2bGameOver.shipsRemaining = 2;
        m2bGameOver.gameStatus = "GAME_OVER";
        m2bGameOver.results = null;

        responseBody = mapper.writeValueAsString(m2bGameOver);

        assertTrue(client.sendShots(session2, List.of(new Position('C', 3))));
        assertTrue(session2.isGameOver());
        assertEquals("AI_WINS", session2.getWinner());
    }

    @Test
    @DisplayName("Devolve false quando o servidor do aluno responde com erro HTTP (ex: 500)")
    void sendShotsHttpErrorStatus() {
        GameSession session = registry.createSession("Miguel", baseUrl);
        responseStatus = 500;
        responseBody = "Internal Server Error";

        boolean ok = client.sendShots(session, List.of(new Position('A', 1)));

        assertFalse(ok);
        assertFalse(session.isGameOver());
    }

    @Test
    @DisplayName("Devolve false quando a resposta JSON do aluno é inválida")
    void sendShotsInvalidJsonResponse() {
        GameSession session = registry.createSession("Miguel", baseUrl);
        responseStatus = 200;
        responseBody = "{ json-invalido }";

        boolean ok = client.sendShots(session, List.of(new Position('A', 1)));

        assertFalse(ok);
    }

    @Test
    @DisplayName("Devolve false quando não é possível ligar ao servidor do aluno (porta fechada)")
    void sendShotsConnectionFailure() {
        server.stop(0);
        GameSession session = registry.createSession("Miguel", baseUrl);

        boolean ok = client.sendShots(session, List.of(new Position('A', 1)));

        assertFalse(ok);
    }

    private ShotResult createResult(String row, int col, String outcome) {
        ShotResult r = new ShotResult();
        r.row = row;
        r.column = col;
        r.outcome = outcome;
        return r;
    }
}