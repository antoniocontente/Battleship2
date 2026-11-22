package battleship;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;

public class PDFReportGenerator {

    public static void generateReport(String filePath, String winner, int totalShots, int sunkShips, double accuracy) {
        File file = new File(filePath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();

                // Título principal
                stream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                stream.newLineAtOffset(50, 750);
                stream.showText("Relatorio da Partida - Battleship 2.0");
                stream.newLineAtOffset(0, -35);

                // Estatísticas da partida
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.showText("Vencedor: " + winner);
                stream.newLineAtOffset(0, -20);
                stream.showText("Total de tiros/jogadas: " + totalShots);
                stream.newLineAtOffset(0, -20);
                stream.showText("Navios afundados: " + sunkShips);
                stream.newLineAtOffset(0, -20);
                stream.showText(String.format("Precisao: %.2f%%", accuracy));
                stream.newLineAtOffset(0, -35);

                // Nota informativa
                stream.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
                stream.showText("Relatorio gerado a partir do estado atual da partida.");

                stream.endText();
            }

            document.save(filePath);
            System.out.println("[INFO] Relatorio em PDF gerado com sucesso em: " + filePath);
        } catch (IOException e) {
            System.err.println("[ERRO] Nao foi possivel gerar o relatorio PDF: " + e.getMessage());
        }
    }
}