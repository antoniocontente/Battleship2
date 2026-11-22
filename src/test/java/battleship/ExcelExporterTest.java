package battleship;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class ExcelExporterTest {

    @TempDir
    Path tempDir;

    // Stub leve apenas para simular um navio nos ShotResults sem depender da implementação concreta
    private static IShip criarNavioStub(String categoria) {
        return new IShip() {
            @Override public String getCategory() { return categoria; }
            @Override public Integer getSize() { return 1; }
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

    // Stub leve de IMove para permitir testar casos limite (shots/results nulos ou vazios)
    private static IMove criarMoveStub(int num, List<IPosition> shots, List<IGame.ShotResult> results) {
        return new IMove() {
            @Override public int getNumber() { return num; }
            @Override public List<IPosition> getShots() { return shots; }
            @Override public List<IGame.ShotResult> getShotResults() { return results; }
            @Override public String processEnemyFire(boolean verbose) { return ""; }
        };
    }

    // Stub leve de IGame para controlar exatamente as listas myMoves e alienMoves
    private static IGame criarGameStub(List<IMove> myMoves, List<IMove> alienMoves) {
        return new IGame() {
            @Override public String randomEnemyFire() { return ""; }
            @Override public String readEnemyFire(Scanner in) { return ""; }
            @Override public void fireShots(List<IPosition> shots) {}
            @Override public ShotResult fireSingleShot(IPosition pos, boolean isRepeated) { return null; }
            @Override public IFleet getMyFleet() { return null; }
            @Override public List<IMove> getAlienMoves() { return alienMoves; }
            @Override public IFleet getAlienFleet() { return null; }
            @Override public List<IMove> getMyMoves() { return myMoves; }
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
    @DisplayName("Exportar jogo sem jogadas (listas vazias) cria linha informativa no Excel")
    void testExportarJogoSemJogadas() throws Exception {
        IGame game = criarGameStub(new ArrayList<>(), new ArrayList<>());
        File ficheiro = tempDir.resolve("sem_jogadas.xlsx").toFile();

        ExcelExporter.exportarJogo(game, ficheiro.getAbsolutePath());

        assertTrue(ficheiro.exists());
        try (FileInputStream fis = new FileInputStream(ficheiro);
             Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet("Histórico de Jogadas");
            assertNotNull(sheet);
            assertEquals("Nº Jogada", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Nenhuma jogada foi efetuada até ao momento.", sheet.getRow(1).getCell(0).getStringCellValue());
        }
    }

    @Test
    @DisplayName("Exportar jogo com listas de jogadas nulas trata graciosamente como vazio")
    void testExportarJogoComListasNulas() throws Exception {
        IGame game = criarGameStub(null, null);
        File ficheiro = tempDir.resolve("listas_nulas.xlsx").toFile();

        ExcelExporter.exportarJogo(game, ficheiro.getAbsolutePath());

        assertTrue(ficheiro.exists());
        try (FileInputStream fis = new FileInputStream(ficheiro);
             Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet("Histórico de Jogadas");
            assertEquals("Nenhuma jogada foi efetuada até ao momento.", sheet.getRow(1).getCell(0).getStringCellValue());
        }
    }

    @Test
    @DisplayName("Exportar jogo com vários tipos de resultados (Inválido, Repetido, Água, Acertou, Afundou)")
    void testExportarJogoComTodosResultados() throws Exception {
        IShip fragata = criarNavioStub("Fragata");
        IShip barca = criarNavioStub("Barca");

        List<IPosition> tiros1 = List.of(
                new Position(0, 0),
                new Position(0, 1),
                new Position(0, 2),
                new Position(0, 3),
                new Position(0, 4),
                new Position(0, 5)
        );

        List<IGame.ShotResult> resultados1 = new ArrayList<>();
        resultados1.add(null); // Inválido (null)
        resultados1.add(new IGame.ShotResult(false, false, null, false)); // Inválido (!valid)
        resultados1.add(new IGame.ShotResult(true, true, null, false));   // Repetido
        resultados1.add(new IGame.ShotResult(true, false, null, false));  // Água
        resultados1.add(new IGame.ShotResult(true, false, fragata, false)); // Acertou
        resultados1.add(new IGame.ShotResult(true, false, barca, true));    // Afundou

        IMove jogadaJogador = criarMoveStub(1, tiros1, resultados1);
        IMove jogadaAlienSemTiros = criarMoveStub(2, null, new ArrayList<>());
        IMove jogadaAlienResultadosNulos = criarMoveStub(3, List.of(), null);

        IGame game = criarGameStub(
                List.of(jogadaJogador),
                List.of(jogadaAlienSemTiros, jogadaAlienResultadosNulos)
        );

        // Testa também a criação automática de subdiretorias inexistentes
        File ficheiro = tempDir.resolve("subpasta/historico_completo.xlsx").toFile();
        new ExcelExporter(); // cobre o construtor implícito da classe
        ExcelExporter.exportarJogo(game, ficheiro.getAbsolutePath());

        assertTrue(ficheiro.exists());
        try (FileInputStream fis = new FileInputStream(ficheiro);
             Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet("Histórico de Jogadas");

            Row r1 = sheet.getRow(1);
            assertEquals(1.0, r1.getCell(0).getNumericCellValue());
            assertEquals("Jogada nº 1", r1.getCell(1).getStringCellValue());
            assertEquals(6.0, r1.getCell(3).getNumericCellValue());
            assertEquals("Inválido, Inválido, Repetido, Água, Acertou (Fragata), Afundou (Barca)",
                    r1.getCell(4).getStringCellValue());

            Row r2 = sheet.getRow(2);
            assertEquals("Sem tiros", r2.getCell(2).getStringCellValue());
            assertEquals(0.0, r2.getCell(3).getNumericCellValue());
            assertEquals("Sem resultados", r2.getCell(4).getStringCellValue());

            Row r3 = sheet.getRow(3);
            assertEquals("Sem resultados", r3.getCell(4).getStringCellValue());
        }
    }

    @Test
    @DisplayName("Exportar para caminho inválido (diretoria) captura IOException sem crashar")
    void testExportarJogoCaminhoInvalido() {
        IGame game = criarGameStub(List.of(), List.of());
        // Passar o caminho da própria pasta em vez de um ficheiro força FileNotFoundException/IOException
        assertDoesNotThrow(() -> ExcelExporter.exportarJogo(game, tempDir.toAbsolutePath().toString()));
        // Cobre também o ramo em que file.getParentFile() == null
        File ficheiroRaiz = new File("temp_raiz_test.xlsx");
        try {
            assertDoesNotThrow(() -> ExcelExporter.exportarJogo(game, ficheiroRaiz.getName()));
        } finally {
            ficheiroRaiz.delete();
        }
    }
}