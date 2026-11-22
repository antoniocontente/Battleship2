package battleship;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PDFReportGenerator.
 * Author: 122652
 * Cyclomatic Complexity for each method:
 * - generateReport: 3 (if getParentFile() != null; try/catch IOException)
 */
@DisplayName("Testes da classe PDFReportGenerator")
public class PDFReportGeneratorTest {

    @TempDir
    Path tempDir;

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;

    @BeforeEach
    void setUp() {
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    /** Lê o texto de um PDF gerado, para validar o conteúdo. */
    private String readPdfText(File pdf) throws IOException {
        try (PDDocument document = PDDocument.load(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    @Test
    @DisplayName("A classe pode ser instanciada (construtor por omissão)")
    void testConstructor() {
        assertNotNull(new PDFReportGenerator());
    }

    @Test
    @DisplayName("Gera o ficheiro PDF numa pasta que já existe")
    void testGenerateReportInExistingFolder() {
        File pdf = tempDir.resolve("relatorio.pdf").toFile();

        PDFReportGenerator.generateReport(pdf.getPath(), "Jogador", 25, 11, 80.0);

        assertTrue(pdf.exists(), "O ficheiro PDF devia ter sido criado");
        assertTrue(pdf.length() > 0, "O ficheiro PDF não devia estar vazio");
    }

    @Test
    @DisplayName("Cria as pastas em falta antes de gravar o PDF")
    void testGenerateReportCreatesMissingFolders() {
        File pdf = tempDir.resolve("reports").resolve("sub").resolve("relatorio.pdf").toFile();
        assertFalse(pdf.getParentFile().exists(), "Pré-condição: a pasta ainda não existe");

        PDFReportGenerator.generateReport(pdf.getPath(), "Jogador", 25, 11, 80.0);

        assertTrue(pdf.getParentFile().isDirectory(), "As pastas deviam ter sido criadas");
        assertTrue(pdf.exists(), "O ficheiro PDF devia ter sido criado");
    }

    @Test
    @DisplayName("Aceita um caminho sem pasta (getParentFile() é null)")
    void testGenerateReportWithoutParentFolder() throws IOException {
        String fileName = "relatorio_teste_122652_" + System.nanoTime() + ".pdf";
        File pdf = new File(fileName);
        assertNull(pdf.getParentFile(), "Pré-condição: o caminho não tem pasta");
        try {
            PDFReportGenerator.generateReport(fileName, "Jogador", 1, 1, 100.0);
            assertTrue(pdf.exists(), "O ficheiro PDF devia ter sido criado na pasta de trabalho");
        } finally {
            Files.deleteIfExists(pdf.toPath());
        }
    }

    @Test
    @DisplayName("O PDF gerado é válido e tem exatamente uma página")
    void testGeneratedPdfHasOnePage() throws IOException {
        File pdf = tempDir.resolve("relatorio.pdf").toFile();

        PDFReportGenerator.generateReport(pdf.getPath(), "Jogador", 25, 11, 80.0);

        try (PDDocument document = PDDocument.load(pdf)) {
            assertEquals(1, document.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("O PDF contém o título, o vencedor, os tiros, os navios afundados e a precisão")
    void testGeneratedPdfContent() throws IOException {
        File pdf = tempDir.resolve("relatorio.pdf").toFile();

        PDFReportGenerator.generateReport(pdf.getPath(), "Computador", 42, 7, 66.666);

        String text = readPdfText(pdf);
        assertAll(
                () -> assertTrue(text.contains("Relatorio da Partida - Battleship 2.0"), "Falta o título"),
                () -> assertTrue(text.contains("Vencedor: Computador"), "Falta o vencedor"),
                () -> assertTrue(text.contains("Total de tiros/jogadas: 42"), "Falta o total de tiros"),
                () -> assertTrue(text.contains("Navios afundados: 7"), "Faltam os navios afundados"),
                // %.2f depende do Locale da JVM: "66.67" (en) ou "66,67" (pt)
                () -> assertTrue(text.matches("(?s).*Precisao: 66[.,]67%.*"), "Falta a precisão com 2 casas decimais"),
                () -> assertTrue(text.contains("Relatorio gerado a partir do estado atual da partida."), "Falta a nota final")
        );
    }

    @Test
    @DisplayName("Aceita valores limite: zero tiros, zero navios e precisão 0%")
    void testGenerateReportWithZeroValues() throws IOException {
        File pdf = tempDir.resolve("zeros.pdf").toFile();

        PDFReportGenerator.generateReport(pdf.getPath(), "Ninguém", 0, 0, 0.0);

        String text = readPdfText(pdf);
        assertTrue(text.contains("Total de tiros/jogadas: 0"));
        assertTrue(text.contains("Navios afundados: 0"));
        assertTrue(text.matches("(?s).*Precisao: 0[.,]00%.*"));
    }

    @Test
    @DisplayName("Escreve uma mensagem de sucesso na consola")
    void testGenerateReportPrintsSuccessMessage() {
        File pdf = tempDir.resolve("relatorio.pdf").toFile();

        PDFReportGenerator.generateReport(pdf.getPath(), "Jogador", 25, 11, 80.0);

        assertTrue(outContent.toString().contains("[INFO] Relatorio em PDF gerado com sucesso em: " + pdf.getPath()));
        assertFalse(errContent.toString().contains("[ERRO]"), "Não devia haver mensagens de erro");
    }

    @Test
    @DisplayName("Sobrescreve um relatório já existente")
    void testGenerateReportOverwritesExistingFile() throws IOException {
        File pdf = tempDir.resolve("relatorio.pdf").toFile();

        PDFReportGenerator.generateReport(pdf.getPath(), "Primeiro", 1, 1, 10.0);
        PDFReportGenerator.generateReport(pdf.getPath(), "Segundo", 2, 2, 20.0);

        String text = readPdfText(pdf);
        assertTrue(text.contains("Vencedor: Segundo"));
        assertFalse(text.contains("Vencedor: Primeiro"));
    }

    @Test
    @DisplayName("Trata a IOException sem a propagar quando não é possível gravar o ficheiro")
    void testGenerateReportHandlesIOException() throws IOException {
        // A "pasta" do relatório é, na verdade, um ficheiro: mkdirs() falha e save() lança IOException
        File notAFolder = tempDir.resolve("isto_e_um_ficheiro.txt").toFile();
        assertTrue(notAFolder.createNewFile());
        File pdf = new File(notAFolder, "relatorio.pdf");

        assertDoesNotThrow(() -> PDFReportGenerator.generateReport(pdf.getPath(), "Jogador", 25, 11, 80.0));

        assertFalse(pdf.exists(), "O ficheiro PDF não devia ter sido criado");
        assertTrue(errContent.toString().contains("[ERRO] Nao foi possivel gerar o relatorio PDF"),
                "Devia ter sido escrita a mensagem de erro");
    }
}