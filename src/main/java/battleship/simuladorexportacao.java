package battleship;

import java.util.ArrayList;
import java.util.List;

public class simuladorexportacao {

    public static void main(String[] args) {
        System.out.println("=== A INICIAR SIMULAÇÃO RÁPIDA DE JOGO ===");

        // 1. Criar uma frota aleatória e o jogo
        IFleet frota = Fleet.createRandom();
        IGame game = new Game(frota);

        // 2. Simular 3 rajadas de tiros automaticamente sem precisar de teclado
        System.out.println("A simular jogadas...");

        // Rajada 1: Tiros em A1, A2, A3
        List<IPosition> rajada1 = new ArrayList<>();
        rajada1.add(new Position('A', 1));
        rajada1.add(new Position('A', 2));
        rajada1.add(new Position('A', 3));
        game.fireShots(rajada1);

        // Rajada 2: Tiros em B1, B2, B3
        List<IPosition> rajada2 = new ArrayList<>();
        rajada2.add(new Position('B', 1));
        rajada2.add(new Position('B', 2));
        rajada2.add(new Position('B', 3));
        game.fireShots(rajada2);

        // Rajada 3: Tiros em C1, C2, C3
        List<IPosition> rajada3 = new ArrayList<>();
        rajada3.add(new Position('C', 1));
        rajada3.add(new Position('C', 2));
        rajada3.add(new Position('C', 3));
        game.fireShots(rajada3);

        System.out.println("Jogadas simuladas com sucesso!");

        // 3. Exportar para Excel imediatamente
        String ficheiroTeste = "teste_simulacao.xlsx";
        System.out.println("A exportar para Excel: " + ficheiroTeste);
        ExcelExporter.exportarJogo(game, ficheiroTeste);

        System.out.println("=== TESTE CONCLUÍDO ===");
    }
}