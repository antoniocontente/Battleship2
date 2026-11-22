package battleship;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Move.
 * Author: 122652
 * Cyclomatic Complexity for each method:
 * - Constructor: 1
 * - toString: 1
 * - getNumber: 1
 * - getShots: 1
 * - getShotResults: 1
 * - processEnemyFire: 37
 *
 * Os testes de processEnemyFire são testes de caracterização: verificam a mensagem
 * escrita na consola (modo verbose) e o JSON devolvido, tal como o código se comporta hoje,
 * para servirem de rede de segurança nas refabricações.
 * Nota: o ramo "validShots == 0" dentro de "if (repeatedShots > 0)" (linha 138) é inalcançável,
 * pelo que a cobertura de ramos não pode chegar a 100% sem alterar o código.
 */
@DisplayName("Testes da classe Move")
public class MoveTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream outContent;

    private IShip caravel;
    private IShip barge;
    private IShip frigate;

    @BeforeEach
    void setUp() {
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        caravel = new Caravel(Compass.NORTH, new Position(1, 1));
        barge = new Barge(Compass.NORTH, new Position(5, 5));
        frigate = new Frigate(Compass.EAST, new Position(8, 1));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    // ---------------------------------------------------------------- auxiliares

    private static IGame.ShotResult miss() {
        return new IGame.ShotResult(true, false, null, false);
    }

    private static IGame.ShotResult invalid() {
        return new IGame.ShotResult(false, false, null, false);
    }

    private static IGame.ShotResult repeated() {
        return new IGame.ShotResult(true, true, null, false);
    }

    private static IGame.ShotResult hit(IShip ship) {
        return new IGame.ShotResult(true, false, ship, false);
    }

    private static IGame.ShotResult sunk(IShip ship) {
        return new IGame.ShotResult(true, false, ship, true);
    }

    private static Move move(IGame.ShotResult... results) {
        return new Move(1, new ArrayList<>(), new ArrayList<>(List.of(results)));
    }

    /** Linha "Jogada nº..." escrita na consola em modo verbose (ou null se não existir). */
    private String verboseLine() {
        return outContent.toString().lines()
                .filter(l -> l.startsWith("Jogada nº"))
                .findFirst()
                .orElse(null);
    }

    private static Map<String, Object> parse(String json) throws Exception {
        return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    // ---------------------------------------------------------------- construtor, getters e toString

    @Nested
    @DisplayName("Construtor, getters e toString")
    class BasicTests {

        @Test
        @DisplayName("Os getters devolvem os valores passados ao construtor")
        void testGetters() {
            List<IPosition> shots = List.of(new Position(1, 1), new Position(2, 2));
            List<IGame.ShotResult> results = List.of(miss());

            Move m = new Move(7, shots, results);

            assertAll(
                    () -> assertEquals(7, m.getNumber()),
                    () -> assertSame(shots, m.getShots()),
                    () -> assertSame(results, m.getShotResults())
            );
        }

        @Test
        @DisplayName("toString mostra o número da jogada e o tamanho das listas")
        void testToString() {
            Move m = new Move(5, List.of(new Position(1, 1), new Position(2, 2)), List.of(miss()));

            assertEquals("Move{number=5, shots=2, results=1}", m.toString());
        }

        @Test
        @DisplayName("IMove.readMove lê o número de tiros e as coordenadas do Scanner")
        void testReadMove() {
            Move m = IMove.readMove(3, new Scanner("2 1 2 3 4"));

            assertAll(
                    () -> assertEquals(3, m.getNumber()),
                    () -> assertEquals(2, m.getShots().size()),
                    () -> assertEquals(new Position(1, 2), m.getShots().get(0)),
                    () -> assertEquals(new Position(3, 4), m.getShots().get(1)),
                    () -> assertTrue(m.getShotResults().isEmpty())
            );
        }
    }

    // ---------------------------------------------------------------- JSON devolvido

    @Nested
    @DisplayName("processEnemyFire: JSON devolvido")
    class JsonTests {

        @Test
        @DisplayName("Sem verbose não escreve a linha da jogada, mas devolve e imprime o JSON")
        void testNotVerbose() throws Exception {
            String json = move(miss(), miss(), miss()).processEnemyFire(false);

            assertNull(verboseLine(), "Em modo não verbose não devia aparecer a linha 'Jogada nº'");
            assertTrue(outContent.toString().contains("JSON (RESULTADO DO TIRO)"));
            assertEquals(3, parse(json).get("validShots"));
        }

        @Test
        @DisplayName("Conta tiros válidos, na água, repetidos e exteriores")
        void testCounters() throws Exception {
            Map<String, Object> r = parse(move(miss(), repeated(), invalid()).processEnemyFire(false));

            assertAll(
                    () -> assertEquals(1, r.get("validShots")),
                    () -> assertEquals(1, r.get("missedShots")),
                    () -> assertEquals(1, r.get("repeatedShots")),
                    () -> assertEquals(1, r.get("outsideShots")),
                    () -> assertEquals(List.of(), r.get("sunkBoats")),
                    () -> assertEquals(List.of(), r.get("hitsOnBoats"))
            );
        }

        @Test
        @DisplayName("Tiros inválidos são ignorados e contam como exteriores")
        void testInvalidShotsAreOutside() throws Exception {
            Map<String, Object> r = parse(move(invalid(), invalid(), invalid()).processEnemyFire(false));

            assertEquals(0, r.get("validShots"));
            assertEquals(0, r.get("repeatedShots"));
            assertEquals(Game.NUMBER_SHOTS, r.get("outsideShots"));
        }

        @Test
        @DisplayName("Um navio atingido mas não afundado aparece em hitsOnBoats")
        void testHitsOnBoats() throws Exception {
            Map<String, Object> r = parse(move(hit(caravel), hit(caravel), miss()).processEnemyFire(false));

            assertEquals(List.of(Map.of("type", "Caravela", "hits", 2)), r.get("hitsOnBoats"));
            assertEquals(List.of(), r.get("sunkBoats"));
        }

        @Test
        @DisplayName("Navios afundados do mesmo tipo são contados juntos e não aparecem em hitsOnBoats")
        void testSunkBoats() throws Exception {
            Map<String, Object> r = parse(move(sunk(barge), sunk(barge), hit(caravel)).processEnemyFire(false));

            assertEquals(List.of(Map.of("type", "Barca", "count", 2)), r.get("sunkBoats"));
            assertEquals(List.of(Map.of("type", "Caravela", "hits", 1)), r.get("hitsOnBoats"));
        }
    }

    // ---------------------------------------------------------------- mensagem em modo verbose

    @Nested
    @DisplayName("processEnemyFire: mensagem em modo verbose")
    class VerboseTests {

        @Test
        @DisplayName("3 tiros na água")
        void testAllMissed() {
            move(miss(), miss(), miss()).processEnemyFire(true);
            assertEquals("Jogada nº1 -> 3 tiros válidos: 3 tiros na água", verboseLine());
        }

        @Test
        @DisplayName("1 tiro na água e 2 tiros exteriores (singular e plural)")
        void testOneMissedTwoOutside() {
            move(miss(), invalid(), invalid()).processEnemyFire(true);
            assertEquals("Jogada nº1 -> 1 tiro válido: 1 tiro na água, 2 tiros exteriores", verboseLine());
        }

        @Test
        @DisplayName("Só 1 tiro repetido (e 2 exteriores)")
        void testOnlyOneRepeated() {
            move(repeated()).processEnemyFire(true);
            assertEquals("Jogada nº1 -> 1 tiro repetido, 2 tiros exteriores", verboseLine());
        }

        @Test
        @DisplayName("Só tiros repetidos, sem tiros exteriores")
        void testOnlyRepeated() {
            move(repeated(), repeated(), repeated()).processEnemyFire(true);
            assertEquals("Jogada nº1 -> 3 tiros repetidos", verboseLine());
        }

        @Test
        @DisplayName("Só tiros inválidos: apenas tiros exteriores, sem vírgula inicial")
        void testOnlyInvalid() {
            move(invalid(), invalid(), invalid()).processEnemyFire(true);
            assertEquals("Jogada nº1 -> 3 tiros exteriores", verboseLine());
        }

        @Test
        @DisplayName("Lista de resultados vazia conta como 3 tiros exteriores")
        void testEmptyResults() {
            move().processEnemyFire(true);
            assertEquals("Jogada nº1 -> 3 tiros exteriores", verboseLine());
        }

        @Test
        @DisplayName("1 tiro numa caravela e 2 tiros na água")
        void testHitAndMissed() {
            move(hit(caravel), miss(), miss()).processEnemyFire(true);
            assertEquals("Jogada nº1 -> 3 tiros válidos: 1 tiro num(a) Caravela + 2 tiros na água", verboseLine());
        }

        @Test
        @DisplayName("2 tiros na mesma caravela, sem tiros na água (remove o '+' final)")
        void testHitsWithoutMissed() {
            move(hit(caravel), hit(caravel), invalid()).processEnemyFire(true);
            // Comportamento atual: fica um espaço antes da vírgula depois de remover o "+"
            assertEquals("Jogada nº1 -> 2 tiros válidos: 2 tiros num(a) Caravela , 1 tiro exterior", verboseLine());
        }

        @Test
        @DisplayName("1 barca ao fundo, 1 tiro na água e 1 repetido")
        void testSunkMissedAndRepeated() {
            move(sunk(barge), miss(), repeated()).processEnemyFire(true);
            assertEquals("Jogada nº1 -> 2 tiros válidos: 1 Barca ao fundo + 1 tiro na água, 1 tiro repetido", verboseLine());
        }

        @Test
        @DisplayName("2 barcas ao fundo (plural) e 1 tiro repetido")
        void testTwoSunkSameType() {
            move(sunk(barge), sunk(barge), repeated()).processEnemyFire(true);
            assertEquals("Jogada nº1 -> 2 tiros válidos: 2 Barcas ao fundo , 1 tiro repetido", verboseLine());
        }

        @Test
        @DisplayName("1 tiro na água e 2 tiros repetidos (plural)")
        void testMissedAndTwoRepeated() {
            move(miss(), repeated(), repeated()).processEnemyFire(true);
            assertEquals("Jogada nº1 -> 1 tiro válido: 1 tiro na água, 2 tiros repetidos", verboseLine());
        }

        @Test
        @DisplayName("Navio afundado e navio atingido de tipos diferentes")
        void testSunkAndHitDifferentTypes() {
            move(hit(caravel), sunk(barge)).processEnemyFire(true);
            assertEquals("Jogada nº1 -> 2 tiros válidos: 1 Barca ao fundo + 1 tiro num(a) Caravela , 1 tiro exterior", verboseLine());
        }

        @Test
        @DisplayName("Só um navio afundado e 2 tiros exteriores")
        void testOnlySunk() {
            move(sunk(frigate), invalid(), invalid()).processEnemyFire(true);
            assertEquals("Jogada nº1 -> 1 tiro válido: 1 Fragata ao fundo , 2 tiros exteriores", verboseLine());
        }

        @Test
        @DisplayName("O número da jogada aparece na mensagem")
        void testMoveNumberInMessage() {
            new Move(12, new ArrayList<>(), List.of(miss(), miss(), miss())).processEnemyFire(true);
            assertTrue(verboseLine().startsWith("Jogada nº12 -> "));
        }
    }
}
