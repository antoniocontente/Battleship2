package battleship;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes da classe SaveGameManager")
class SaveGameManagerTest {

    private static final Path FILE = Path.of(SaveGameManager.SAVE_DIR, SaveGameManager.DEFAULT_FILE);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Cópia do savegame.json que já existia, para o repor no fim de cada teste. */
    private byte[] backup;

    @BeforeEach
    void backupSaveFile() throws IOException {
        backup = Files.exists(FILE) ? Files.readAllBytes(FILE) : null;
    }

    @AfterEach
    void restoreSaveFile() throws IOException {
        if (backup != null) {
            Files.createDirectories(FILE.getParent());
            Files.write(FILE, backup);
        } else {
            Files.deleteIfExists(FILE);
        }
    }

    // ── Auxiliares ──────────────────────────────────────────────────────────

    private List<IPosition> threeShots(int row) {
        List<IPosition> shots = new ArrayList<>();
        shots.add(new Position(row, 0));
        shots.add(new Position(row, 1));
        shots.add(new Position(row, 2));
        return shots;
    }

    private Game newGameWithTwoMoves() {
        Game game = new Game(Fleet.createRandom());
        game.fireShots(threeShots(0));
        game.fireShots(threeShots(5));
        return game;
    }

    private SaveGameManager.GameState readState() throws IOException {
        return MAPPER.readValue(FILE.toFile(), SaveGameManager.GameState.class);
    }

    private void writeState(SaveGameManager.GameState state) throws IOException {
        Files.createDirectories(FILE.getParent());
        MAPPER.writeValue(FILE.toFile(), state);
    }

    /** Grava um jogo válido e devolve o estado com os navios substituídos. */
    private void writeStateWithShips(List<SaveGameManager.ShipData> ships) throws IOException {
        SaveGameManager.save(newGameWithTwoMoves());
        SaveGameManager.GameState s = readState();
        writeState(new SaveGameManager.GameState(ships, s.moves(), s.totalMoves(),
                s.hits(), s.sunkShips(), s.repeatedShots(), s.invalidShots()));
    }

    private SaveGameManager.ShipData firstSavedShip() throws IOException {
        SaveGameManager.save(newGameWithTwoMoves());
        return readState().ships().get(0);
    }

    // ── save ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save cria o ficheiro saves/savegame.json e devolve o seu caminho")
    void saveCreatesFile() throws IOException {
        Path result = SaveGameManager.save(newGameWithTwoMoves());

        assertEquals(FILE, result);
        assertTrue(Files.exists(result));
    }

    @Test
    @DisplayName("save cria a pasta saves se ela não existir")
    void saveCreatesDirectory() throws IOException {
        Files.deleteIfExists(FILE);
        try {
            Files.deleteIfExists(FILE.getParent());
        } catch (DirectoryNotEmptyException ignored) {
            // a pasta tem outros ficheiros: o teste continua válido
        }

        SaveGameManager.save(newGameWithTwoMoves());

        assertTrue(Files.exists(FILE));
    }

    @Test
    @DisplayName("save grava todos os navios da frota e todas as jogadas com os seus tiros")
    void saveWritesShipsAndMoves() throws IOException {
        Game game = newGameWithTwoMoves();

        SaveGameManager.save(game);
        SaveGameManager.GameState state = readState();

        assertEquals(game.getMyFleet().getShips().size(), state.ships().size());
        assertEquals(2, state.moves().size());
        assertEquals(2, state.totalMoves());
        assertEquals(1, state.moves().get(0).number());
        assertEquals(2, state.moves().get(1).number());
        assertEquals(3, state.moves().get(0).shots().size());
    }

    @Test
    @DisplayName("save grava os contadores do jogo (acertos, afundados, repetidos, inválidos)")
    void saveWritesCounters() throws IOException {
        Game game = newGameWithTwoMoves();

        SaveGameManager.save(game);
        SaveGameManager.GameState state = readState();

        assertEquals(game.getHits(), state.hits());
        assertEquals(game.getSunkShips(), state.sunkShips());
        assertEquals(game.getRepeatedShots(), state.repeatedShots());
        assertEquals(game.getInvalidShots(), state.invalidShots());
    }

    @Test
    @DisplayName("save de um jogo sem jogadas grava a lista de jogadas vazia")
    void saveGameWithoutMoves() throws IOException {
        SaveGameManager.save(new Game(Fleet.createRandom()));
        SaveGameManager.GameState state = readState();

        assertTrue(state.moves().isEmpty());
        assertEquals(0, state.totalMoves());
    }

    // ── load: casos de sucesso ──────────────────────────────────────────────

    @Test
    @DisplayName("load reconstrói o jogo guardado com a mesma frota, jogadas e contadores")
    void loadRestoresSavedGame() throws IOException {
        Game original = newGameWithTwoMoves();
        SaveGameManager.save(original);

        Game loaded = SaveGameManager.load();

        assertNotNull(loaded);
        assertEquals(original.getMyFleet().getShips().size(), loaded.getMyFleet().getShips().size());
        assertEquals(original.getAlienMoves().size(), loaded.getAlienMoves().size());
        assertEquals(original.getHits(), loaded.getHits());
        assertEquals(original.getSunkShips(), loaded.getSunkShips());
        assertEquals(original.getRepeatedShots(), loaded.getRepeatedShots());
        assertEquals(original.getInvalidShots(), loaded.getInvalidShots());
    }

    @Test
    @DisplayName("load repõe o System.out original depois de repetir as jogadas")
    void loadRestoresSystemOut() throws IOException {
        SaveGameManager.save(newGameWithTwoMoves());
        PrintStream before = System.out;

        SaveGameManager.load();

        assertSame(before, System.out);
    }

    // ── load: casos de erro ─────────────────────────────────────────────────

    @Test
    @DisplayName("load lança IOException quando não existe jogo guardado")
    void loadWithoutFileThrows() throws IOException {
        Files.deleteIfExists(FILE);

        IOException e = assertThrows(IOException.class, SaveGameManager::load);
        assertTrue(e.getMessage().contains("Não existe nenhum jogo guardado"));
    }

    @Test
    @DisplayName("load lança IOException quando o ficheiro não é JSON válido")
    void loadMalformedJsonThrows() throws IOException {
        Files.createDirectories(FILE.getParent());
        Files.writeString(FILE, "isto não é json");

        assertThrows(IOException.class, SaveGameManager::load);
    }

    @Test
    @DisplayName("load lança IOException quando faltam os navios no ficheiro")
    void loadWithoutShipsThrows() throws IOException {
        writeState(new SaveGameManager.GameState(null, List.of(), 0, 0, 0, 0, 0));

        IOException e = assertThrows(IOException.class, SaveGameManager::load);
        assertTrue(e.getMessage().contains("Ficheiro de jogo inválido"));
    }

    @Test
    @DisplayName("load lança IOException quando faltam as jogadas no ficheiro")
    void loadWithoutMovesThrows() throws IOException {
        writeState(new SaveGameManager.GameState(List.of(), null, 0, 0, 0, 0, 0));

        IOException e = assertThrows(IOException.class, SaveGameManager::load);
        assertTrue(e.getMessage().contains("Ficheiro de jogo inválido"));
    }

    @Test
    @DisplayName("load lança IOException quando um navio tem uma orientação inválida")
    void loadInvalidBearingThrows() throws IOException {
        SaveGameManager.ShipData ship = firstSavedShip();
        writeStateWithShips(List.of(
                new SaveGameManager.ShipData(ship.category(), "XPTO", ship.row(), ship.column())));

        IOException e = assertThrows(IOException.class, SaveGameManager::load);
        assertTrue(e.getMessage().contains("Orientação inválida"));
    }

    @Test
    @DisplayName("load lança IOException quando um navio tem um tipo desconhecido")
    void loadUnknownCategoryThrows() throws IOException {
        SaveGameManager.ShipData ship = firstSavedShip();
        writeStateWithShips(List.of(
                new SaveGameManager.ShipData("Submarino", ship.bearing(), ship.row(), ship.column())));

        IOException e = assertThrows(IOException.class, SaveGameManager::load);
        assertTrue(e.getMessage().contains("Tipo de navio desconhecido"));
    }

    @Test
    @DisplayName("load lança IOException quando dois navios estão em colisão")
    void loadCollidingShipsThrows() throws IOException {
        SaveGameManager.ShipData ship = firstSavedShip();
        writeStateWithShips(List.of(ship, ship));

        IOException e = assertThrows(IOException.class, SaveGameManager::load);
        assertTrue(e.getMessage().contains("Navio inválido ou em colisão"));
    }

    @Test
    @DisplayName("load lança IOException quando uma rajada é inválida e repõe o System.out")
    void loadInvalidMoveThrows() throws IOException {
        SaveGameManager.save(newGameWithTwoMoves());
        SaveGameManager.GameState s = readState();
        List<SaveGameManager.MoveData> badMoves = List.of(
                new SaveGameManager.MoveData(1, List.of(new SaveGameManager.ShotData(0, 0))));
        writeState(new SaveGameManager.GameState(s.ships(), badMoves, 1, 0, 0, 0, 0));
        PrintStream before = System.out;

        IOException e = assertThrows(IOException.class, SaveGameManager::load);
        assertTrue(e.getMessage().contains("Rajada inválida"));
        assertSame(before, System.out);
    }
}