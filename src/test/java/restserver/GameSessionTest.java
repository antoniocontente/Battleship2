package restserver;

import battleship.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes da classe GameSession")
class GameSessionTest {

    private GameSession session;

    @BeforeEach
    void setUp() {
        session = new GameSession("id-123", "Rodrigo", "http://localhost:9090");
    }

    @Test
    @DisplayName("O construtor guarda o gameId, o nome do jogador e o callbackUrl")
    void constructorStoresFields() {
        assertEquals("id-123", session.getGameId());
        assertEquals("Rodrigo", session.getPlayerName());
        assertEquals("http://localhost:9090", session.getCallbackUrl());
    }

    @Test
    @DisplayName("O construtor cria um jogo (Game) não nulo")
    void constructorCreatesGame() {
        assertNotNull(session.getGame());
        assertInstanceOf(Game.class, session.getGame());
    }

    @Test
    @DisplayName("O número de tiros por turno é igual a Game.NUMBER_SHOTS")
    void shotsPerTurnMatchesGameConstant() {
        assertEquals(Game.NUMBER_SHOTS, session.getShotsPerTurn());
    }

    @Test
    @DisplayName("Uma sessão nova não está terminada e não tem vencedor")
    void newSessionIsNotOver() {
        assertFalse(session.isGameOver());
        assertNull(session.getWinner());
    }

    @Test
    @DisplayName("Cada sessão tem o seu próprio objeto Game")
    void eachSessionHasOwnGame() {
        GameSession other = new GameSession("id-456", "Outro", "http://outro:9090");

        assertNotSame(session.getGame(), other.getGame());
    }

    @Test
    @DisplayName("markAiWins termina o jogo com vencedor AI_WINS")
    void markAiWinsSetsWinner() {
        session.markAiWins();

        assertTrue(session.isGameOver());
        assertEquals("AI_WINS", session.getWinner());
    }

    @Test
    @DisplayName("markStudentWins termina o jogo com vencedor STUDENT_WINS")
    void markStudentWinsSetsWinner() {
        session.markStudentWins();

        assertTrue(session.isGameOver());
        assertEquals("STUDENT_WINS", session.getWinner());
    }

    @Test
    @DisplayName("Aceita callbackUrl nulo sem lançar exceção")
    void constructorAcceptsNullCallback() {
        GameSession s = assertDoesNotThrow(() -> new GameSession("id-789", "Rodrigo", null));

        assertNull(s.getCallbackUrl());
    }
}