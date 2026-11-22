package restserver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes da classe GameRegistry")
class GameRegistryTest {

    private GameRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new GameRegistry();
    }

    @Test
    @DisplayName("createSession devolve uma sessão não nula com os dados do jogador")
    void createSessionReturnsSessionWithPlayerData() {
        GameSession session = registry.createSession("Rodrigo", "http://localhost:9090");

        assertNotNull(session);
        assertEquals("Rodrigo", session.getPlayerName());
        assertEquals("http://localhost:9090", session.getCallbackUrl());
    }

    @Test
    @DisplayName("createSession gera um gameId que é um UUID válido")
    void createSessionGeneratesValidUuid() {
        GameSession session = registry.createSession("Rodrigo", "http://localhost:9090");

        assertNotNull(session.getGameId());
        assertDoesNotThrow(() -> UUID.fromString(session.getGameId()));
    }

    @Test
    @DisplayName("createSession guarda a sessão no registo")
    void createSessionStoresSession() {
        GameSession session = registry.createSession("Rodrigo", "http://localhost:9090");

        assertSame(session, registry.getSession(session.getGameId()));
    }

    @Test
    @DisplayName("Duas sessões criadas têm gameIds diferentes e ficam ambas guardadas")
    void twoSessionsHaveDifferentIds() {
        GameSession s1 = registry.createSession("Jogador1", "http://a:9090");
        GameSession s2 = registry.createSession("Jogador2", "http://b:9090");

        assertNotEquals(s1.getGameId(), s2.getGameId());
        assertSame(s1, registry.getSession(s1.getGameId()));
        assertSame(s2, registry.getSession(s2.getGameId()));
    }

    @Test
    @DisplayName("getSession devolve null para um gameId que não existe")
    void getSessionUnknownIdReturnsNull() {
        assertNull(registry.getSession("id-que-nao-existe"));
    }

    @Test
    @DisplayName("removeSession retira a sessão do registo")
    void removeSessionRemovesIt() {
        GameSession session = registry.createSession("Rodrigo", "http://localhost:9090");
        String id = session.getGameId();

        registry.removeSession(id);

        assertNull(registry.getSession(id));
    }

    @Test
    @DisplayName("removeSession só retira a sessão indicada")
    void removeSessionKeepsOthers() {
        GameSession s1 = registry.createSession("Jogador1", "http://a:9090");
        GameSession s2 = registry.createSession("Jogador2", "http://b:9090");

        registry.removeSession(s1.getGameId());

        assertNull(registry.getSession(s1.getGameId()));
        assertSame(s2, registry.getSession(s2.getGameId()));
    }

    @Test
    @DisplayName("removeSession com um gameId inexistente não lança exceção")
    void removeSessionUnknownIdDoesNotThrow() {
        assertDoesNotThrow(() -> registry.removeSession("id-que-nao-existe"));
    }
}