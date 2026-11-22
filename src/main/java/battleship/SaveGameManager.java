package battleship;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Guarda e carrega o estado de uma partida em formato JSON (biblioteca Jackson).
 * O estado guardado é a frota e as rajadas já disparadas; ao carregar, as rajadas
 * são repetidas sobre a frota para recalcular acertos, afundados e contadores.
 */
public class SaveGameManager {

    public static final String SAVE_DIR = "saves";
    public static final String DEFAULT_FILE = "savegame.json";

    public record ShipData(String category, String bearing, int row, int column) {}
    public record ShotData(int row, int column) {}
    public record MoveData(int number, List<ShotData> shots) {}
    public record GameState(List<ShipData> ships,
                            List<MoveData> moves,
                            int totalMoves,
                            int hits,
                            int sunkShips,
                            int repeatedShots,
                            int invalidShots) {}

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /** Grava o estado do jogo em saves/savegame.json. */
    public static Path save(IGame game) throws IOException {
        assert game != null;

        List<ShipData> ships = new ArrayList<>();
        for (IShip ship : game.getMyFleet().getShips())
            ships.add(new ShipData(
                    ship.getCategory(),
                    ship.getBearing().name(),
                    ship.getPosition().getRow(),
                    ship.getPosition().getColumn()));

        List<MoveData> moves = new ArrayList<>();
        int number = 1;
        for (IMove move : game.getAlienMoves()) {
            List<ShotData> shots = new ArrayList<>();
            for (IPosition shot : move.getShots())
                shots.add(new ShotData(shot.getRow(), shot.getColumn()));
            moves.add(new MoveData(number++, shots));
        }

        GameState state = new GameState(ships, moves, moves.size(),
                game.getHits(), game.getSunkShips(),
                game.getRepeatedShots(), game.getInvalidShots());

        Path dir = Path.of(SAVE_DIR);
        Files.createDirectories(dir);
        Path file = dir.resolve(DEFAULT_FILE);
        MAPPER.writeValue(file.toFile(), state);
        return file;
    }

    /** Lê saves/savegame.json e reconstrói o jogo. */
    public static Game load() throws IOException {
        File file = Path.of(SAVE_DIR, DEFAULT_FILE).toFile();
        if (!file.exists())
            throw new IOException("Não existe nenhum jogo guardado (" + file.getPath() + ").");

        GameState state = MAPPER.readValue(file, GameState.class);
        if (state.ships() == null || state.moves() == null)
            throw new IOException("Ficheiro de jogo inválido: faltam navios ou jogadas.");

        // 1. Reconstruir a frota
        Fleet fleet = new Fleet();
        for (ShipData data : state.ships()) {
            Compass bearing;
            try {
                bearing = Compass.valueOf(data.bearing());
            } catch (IllegalArgumentException e) {
                throw new IOException("Orientação inválida no ficheiro: " + data.bearing());
            }
            Ship ship = Ship.buildShip(data.category().toLowerCase(), bearing, new Position(data.row(), data.column()));            if (ship == null)
                throw new IOException("Tipo de navio desconhecido: " + data.category());
            if (!fleet.addShip(ship))
                throw new IOException("Navio inválido ou em colisão: " + data.category());
        }

        // 2. Criar o jogo e repetir as rajadas (sem imprimir as mensagens antigas)
        Game game = new Game(fleet);
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        try {
            for (MoveData move : state.moves()) {
                List<IPosition> shots = new ArrayList<>();
                for (ShotData shot : move.shots())
                    shots.add(new Position(shot.row(), shot.column()));
                game.fireShots(shots);
            }
        } catch (IllegalArgumentException e) {
            throw new IOException("Rajada inválida no ficheiro: " + e.getMessage());
        } finally {
            System.setOut(originalOut);
        }
        return game;
    }
}
