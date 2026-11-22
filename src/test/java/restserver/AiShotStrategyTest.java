package restserver;

import battleship.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AiShotStrategyTest {

    private static IShip criarNavioStub() {
        return new IShip() {
            @Override public String getCategory() { return "Barca"; }
            @Override public Integer getSize() { return 2; }
            @Override public List<IPosition> getPositions() { return List.of(); }
            @Override public List<IPosition> getAdjacentPositions() { return List.of(); }
            @Override public IPosition getPosition() { return null; }
            @Override public Compass getBearing() { return Compass.NORTH; }
            @Override public boolean stillFloating() { return true; }
            @Override public int getTopMostPos() { return 0; }
            @Override public int getBottomMostPos() { return 0; }
            @Override public int getLeftMostPos() { return 0; }
            @Override public int getRightMostPos() { return 0; }
            @Override public boolean occupies(IPosition pos) { return false; }
            @Override public boolean tooCloseTo(IShip other) { return false; }
            @Override public boolean tooCloseTo(IPosition pos) { return false; }
            @Override public boolean isInsideBoard() { return true; }
            @Override public void shoot(IPosition pos) {}
            @Override public void sink() {}
        };
    }

    private static IMove criarMoveStub(int num, List<IPosition> shots, List<IGame.ShotResult> results) {
        return new IMove() {
            @Override public int getNumber() { return num; }
            @Override public List<IPosition> getShots() { return shots; }
            @Override public List<IGame.ShotResult> getShotResults() { return results; }
            @Override public String processEnemyFire(boolean verbose) { return ""; }
        };
    }

    private static IGame criarGameComAlienMoves(List<IMove> alienMoves) {
        return new IGame() {
            @Override public String randomEnemyFire() { return ""; }
            @Override public String readEnemyFire(Scanner in) { return ""; }
            @Override public void fireShots(List<IPosition> shots) {}
            @Override public ShotResult fireSingleShot(IPosition pos, boolean isRepeated) { return null; }
            @Override public IFleet getMyFleet() { return null; }
            @Override public List<IMove> getAlienMoves() { return alienMoves; }
            @Override public IFleet getAlienFleet() { return null; }
            @Override public List<IMove> getMyMoves() { return List.of(); }
            @Override public int getRepeatedShots() { return 0; }
            @Override public int getInvalidShots() { return 0; }
            @Override public int getHits() { return 0; }
            @Override public int getSunkShips() { return 0; }
            @Override public int getRemainingShips() { return 0; }
            @Override public void printMyBoard(boolean show_shots, boolean show_legend) {}
            @Override public void printAlienBoard(boolean show_shots, boolean show_legend) {}
            @Override public void over() {}
        };
    }

    @Test
    @DisplayName("Fase HUNT sem jogadas anteriores escolhe posições válidas em padrão xadrez")
    void testHuntSemJogadasAnteriores() {
        AiShotStrategy strategy = new AiShotStrategy();
        IGame game = criarGameComAlienMoves(List.of());

        List<IPosition> shots = strategy.computeShots(game);

        assertEquals(Game.NUMBER_SHOTS, shots.size());
        assertEquals(Game.NUMBER_SHOTS, new HashSet<>(shots).size());
        for (IPosition p : shots) {
            assertEquals(0, (p.getRow() + p.getColumn()) % 2);
        }
    }

    @Test
    @DisplayName("Fase TARGET prioriza vizinhos de acertos não afundados e ignora inválidos, repetidos, água e afundados")
    void testTargetComVariosResultadosEAdjacentesPartilhados() {
        AiShotStrategy strategy = new AiShotStrategy();
        IShip navio = criarNavioStub();

        // Jogada 1: mistura de resultados onde apenas (5,5) e (5,6) são hits válidos não afundados
        List<IPosition> tiros1 = List.of(
                new Position(0, 0), // null result
                new Position(0, 2), // inválido
                new Position(0, 4), // repetido
                new Position(0, 6), // água
                new Position(0, 8), // afundado
                new Position(5, 5), // HIT ativo 1
                new Position(5, 6), // HIT ativo 2 (partilha vizinhos com (5,5))
                new Position(9, 9)  // tiro sem resultado correspondente (lista de resultados mais curta)
        );

        List<IGame.ShotResult> resultados1 = new ArrayList<>();
        resultados1.add(null);
        resultados1.add(new IGame.ShotResult(false, false, null, false));
        resultados1.add(new IGame.ShotResult(true, true, null, false));
        resultados1.add(new IGame.ShotResult(true, false, null, false));
        resultados1.add(new IGame.ShotResult(true, false, navio, true));
        resultados1.add(new IGame.ShotResult(true, false, navio, false));
        resultados1.add(new IGame.ShotResult(true, false, navio, false));

        // Jogada 2: lista de resultados vazia + um vizinho de (5,5) já disparado anteriormente
        List<IPosition> tiros2 = List.of(new Position(4, 5));
        List<IGame.ShotResult> resultados2 = List.of();

        IGame game = criarGameComAlienMoves(List.of(
                criarMoveStub(1, tiros1, resultados1),
                criarMoveStub(2, tiros2, resultados2)
        ));

        List<IPosition> shots = strategy.computeShots(game);

        assertEquals(Game.NUMBER_SHOTS, shots.size());
        assertEquals(Game.NUMBER_SHOTS, new HashSet<>(shots).size());
        assertFalse(shots.contains(new Position(4, 5)));
        assertFalse(shots.contains(new Position(5, 5)));
        assertFalse(shots.contains(new Position(5, 6)));
    }

    @Test
    @DisplayName("Transição TARGET para HUNT quando hit no canto tem menos vizinhos do que NUMBER_SHOTS")
    void testTargetCantoCompletaComHunt() {
        AiShotStrategy strategy = new AiShotStrategy();
        IShip navio = criarNavioStub();

        // Canto (0,0) tem 3 vizinhos: (0,1), (1,0) [ímpares] e (1,1) [par - cai no padrão hunt!]
        // Bloqueando (0,1) e (1,0), sobra apenas (1,1) na fase TARGET e os restantes vêm do HUNT
        List<IPosition> tiros = List.of(
                new Position(0, 0),
                new Position(0, 1),
                new Position(1, 0)
        );
        List<IGame.ShotResult> resultados = List.of(
                new IGame.ShotResult(true, false, navio, false),
                new IGame.ShotResult(true, false, null, false),
                new IGame.ShotResult(true, false, null, false)
        );

        IGame game = criarGameComAlienMoves(List.of(criarMoveStub(1, tiros, resultados)));
        List<IPosition> shots = strategy.computeShots(game);

        assertEquals(Game.NUMBER_SHOTS, shots.size());
        assertTrue(shots.contains(new Position(1, 1)));
    }

    @Test
    @DisplayName("Fallback de segurança escolhe células ímpares quando todo o padrão xadrez já foi disparado")
    void testFallbackQuandoXadrezEsgotado() {
        AiShotStrategy strategy = new AiShotStrategy();
        IShip navio = criarNavioStub();

        // Preenche todas as células pares (r + c) % 2 == 0 como água, exceto (0,0) que é HIT
        // O hit em (0,0) vai escolher 1 vizinho ímpar (ex: (0,1)) e o fallback preenche o resto com células ímpares
        List<IPosition> tiros = new ArrayList<>();
        List<IGame.ShotResult> resultados = new ArrayList<>();

        for (int r = 0; r < Game.BOARD_SIZE; r++) {
            for (int c = 0; c < Game.BOARD_SIZE; c++) {
                if ((r + c) % 2 == 0) {
                    tiros.add(new Position(r, c));
                    if (r == 0 && c == 0) {
                        resultados.add(new IGame.ShotResult(true, false, navio, false));
                    } else {
                        resultados.add(new IGame.ShotResult(true, false, null, false));
                    }
                }
            }
        }

        IGame game = criarGameComAlienMoves(List.of(criarMoveStub(1, tiros, resultados)));
        List<IPosition> shots = strategy.computeShots(game);

        assertEquals(Game.NUMBER_SHOTS, shots.size());
        assertEquals(Game.NUMBER_SHOTS, new HashSet<>(shots).size());
        for (IPosition p : shots) {
            assertEquals(1, (p.getRow() + p.getColumn()) % 2);
        }
    }
    @Test
    @DisplayName("Tabuleiro totalmente disparado termina sem erro e múltiplos hits isolados preenchem tiros")
    void testTabuleiroCheioEMultiplosHitsIsolados() {
        AiShotStrategy strategy = new AiShotStrategy();
        IShip navio = criarNavioStub();

        // 1. Dois hits nos cantos opostos (0,0) e (9,9) com vizinhos ortogonais já gastos:
        // (0,0) só adiciona (1,1) [1 tiro < 3] e o ciclo passa ao segundo hit (9,9) que adiciona (8,8)
        List<IPosition> tirosCantos = List.of(
                new Position(0, 0), new Position(0, 1), new Position(1, 0),
                new Position(9, 9), new Position(9, 8), new Position(8, 9)
        );
        List<IGame.ShotResult> resCantos = List.of(
                new IGame.ShotResult(true, false, navio, false),
                new IGame.ShotResult(true, false, null, false),
                new IGame.ShotResult(true, false, null, false),
                new IGame.ShotResult(true, false, navio, false),
                new IGame.ShotResult(true, false, null, false),
                new IGame.ShotResult(true, false, null, false)
        );
        IGame gameCantos = criarGameComAlienMoves(List.of(criarMoveStub(1, tirosCantos, resCantos)));
        assertEquals(Game.NUMBER_SHOTS, strategy.computeShots(gameCantos).size());

        // 2. Tabuleiro 100% disparado: obriga os ciclos do fallback a percorrerem todas as linhas/colunas
        List<IPosition> todosTiros = new ArrayList<>();
        List<IGame.ShotResult> todosRes = new ArrayList<>();
        for (int r = 0; r < Game.BOARD_SIZE; r++) {
            for (int c = 0; c < Game.BOARD_SIZE; c++) {
                todosTiros.add(new Position(r, c));
                todosRes.add(new IGame.ShotResult(true, false, null, false));
            }
        }
        IGame gameCheio = criarGameComAlienMoves(List.of(criarMoveStub(1, todosTiros, todosRes)));
        assertTrue(strategy.computeShots(gameCheio).isEmpty());
    }
}