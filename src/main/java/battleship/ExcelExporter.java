package battleship;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExcelExporter {

    public static void exportarJogo(IGame game, String caminhoFicheiro) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Histórico de Jogadas");

        // Cabeçalho detalhado
        Row headerRow = sheet.createRow(0);
        String[] colunas = {"Nº Jogada", "Origem", "Coordenadas dos Tiros", "Total Tiros", "Resultados"};

        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < colunas.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(colunas[i]);
            cell.setCellStyle(headerStyle);
        }

        // Junta jogadas do jogador e do alien para garantir que nada fica de fora
        List<IMove> todasAsJogadas = new ArrayList<>();

        if (game.getMyMoves() != null && !game.getMyMoves().isEmpty()) {
            todasAsJogadas.addAll(game.getMyMoves());
        }
        if (game.getAlienMoves() != null && !game.getAlienMoves().isEmpty()) {
            todasAsJogadas.addAll(game.getAlienMoves());
        }

        int rowNum = 1;

        if (!todasAsJogadas.isEmpty()) {
            for (IMove move : todasAsJogadas) {
                Row row = sheet.createRow(rowNum++);

                // Nº da Jogada
                row.createCell(0).setCellValue(move.getNumber());

                // Origem (Jugador ou Alien)
                row.createCell(1).setCellValue("Jogada nº " + move.getNumber());

                // Formatar lista de tiros (ex: [A1, A2, A3])
                String posicoesStr = (move.getShots() != null) ? move.getShots().toString() : "Sem tiros";
                row.createCell(2).setCellValue(posicoesStr);

                // Quantidade de tiros
                int numShots = (move.getShots() != null) ? move.getShots().size() : 0;
                row.createCell(3).setCellValue(numShots);

                // Detalhe de cada resultado
                StringBuilder resultadosStr = new StringBuilder();
                if (move.getShotResults() != null && !move.getShotResults().isEmpty()) {
                    for (int j = 0; j < move.getShotResults().size(); j++) {
                        IGame.ShotResult res = move.getShotResults().get(j);
                        if (j > 0) {
                            resultadosStr.append(", ");
                        }
                        if (res == null || !res.valid()) {
                            resultadosStr.append("Inválido");
                        } else if (res.repeated()) {
                            resultadosStr.append("Repetido");
                        } else if (res.ship() == null) {
                            resultadosStr.append("Água");
                        } else if (res.sunk()) {
                            resultadosStr.append("Afundou (").append(res.ship().getCategory()).append(")");
                        } else {
                            resultadosStr.append("Acertou (").append(res.ship().getCategory()).append(")");
                        }
                    }
                } else {
                    resultadosStr.append("Sem resultados");
                }
                row.createCell(4).setCellValue(resultadosStr.toString());
            }
        } else {
            // Se ainda não houver jogadas registadas na memória
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("Nenhuma jogada foi efetuada até ao momento.");
        }

        // Ajustar largura das colunas
        for (int i = 0; i < colunas.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Assegurar que a pasta de destino existe
        java.io.File file = new java.io.File(caminhoFicheiro);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        // Gravar no ficheiro
        try (Workbook wb = workbook; FileOutputStream fileOut = new FileOutputStream(file)) {
            wb.write(fileOut);
            System.out.println("Histórico exportado com sucesso para: " + caminhoFicheiro);
        } catch (IOException e) {
            System.err.println("Erro ao exportar o ficheiro Excel: " + e.getMessage());
        }
    }
}