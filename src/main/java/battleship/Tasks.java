package battleship;

import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * The type Tasks.
 */
public class Tasks {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String GOODBYE_MESSAGE = "Bons ventos!";

    private static final String AJUDA = "ajuda";
    private static final String GERAFROTA = "gerafrota";
    private static final String LEFROTA = "lefrota";
    private static final String DESISTIR = "desisto";
    private static final String RAJADA = "rajada";
    private static final String TIROS = "tiros";
    private static final String MAPA = "mapa";
    private static final String STATUS = "estado";
    private static final String SIMULA = "simula";
    private static final String EXPORTAR = "exportar";
    private static final String GUARDAR = "guardar";
    private static final String CARREGAR = "carregar";
    private static final String PDF = "pdf";

    public static void menu() {

        IFleet myFleet = null;
        IGame game = null;
        menuHelp();

        System.out.print("> ");
        Scanner in = new Scanner(System.in);
        String command = in.next();
        while (!command.equals(DESISTIR)) {

            switch (command) {
                case GERAFROTA:
                    myFleet = Fleet.createRandom();
                    game = new Game(myFleet);
                    game.printMyBoard(false, true);
                    break;
                case LEFROTA:
                    myFleet = buildFleet(in);
                    game = new Game(myFleet);
                    game.printMyBoard(false, true);
                    break;
                case STATUS:
                    if (myFleet != null)
                        myFleet.printStatus();
                    break;
                case MAPA:
                    if (myFleet != null)
                        game.printMyBoard(false, true);
                    break;
                case RAJADA:
                    if (game != null) {
                        game.readEnemyFire(in);
                        myFleet.printStatus();
                        game.printMyBoard(true, false);

                        if (game.getRemainingShips() == 0) {
                            game.over();
                            System.exit(0);
                        }
                    }
                    break;
                case SIMULA:
                    if (game != null) {
                        while (game.getRemainingShips() > 0) {
                            game.randomEnemyFire();
                            myFleet.printStatus();
                            game.printMyBoard(true, false);
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }

                        if (game.getRemainingShips() == 0) {
                            game.over();
                            System.exit(0);
                        }
                    }
                    break;
                case TIROS:
                    if (game != null)
                        game.printMyBoard(true, true);
                    break;
                case GUARDAR:
                    if (game != null) {
                        try {
                            System.out.println("Jogo guardado em " + SaveGameManager.save(game));
                        } catch (Exception e) {
                            System.out.println("Erro ao guardar o jogo: " + e.getMessage());
                        }
                    } else {
                        System.out.println("Não há jogo para guardar. Use gerafrota ou lefrota primeiro.");
                    }
                    break;
                case CARREGAR:
                    try {
                        Game loaded = SaveGameManager.load();
                        game = loaded;
                        myFleet = loaded.getMyFleet();
                        System.out.println("Jogo carregado!");
                        myFleet.printStatus();
                        loaded.printMyBoard(true, false);
                    } catch (Exception e) {
                        System.out.println("Erro ao carregar o jogo: " + e.getMessage());
                    }
                    break;
                case EXPORTAR:
                    if (game != null) {
                        ExcelExporter.exportarJogo(game, "reports/historico_partida.xlsx");
                    } else {
                        System.out.println("Ainda não existe nenhum jogo em curso!");
                    }
                    break;
                case PDF:
                    if (game != null) {
                        int afundados = game.getSunkShips();
                        int tirosTotais = 0;
                        if (game.getAlienMoves() != null) {
                            for (IMove move : game.getAlienMoves()) {
                                if (move.getShots() != null) {
                                    tirosTotais += move.getShots().size();
                                }
                            }
                        }
                        int hits = game.getHits();
                        double precisao = tirosTotais > 0 ? ((double) hits / tirosTotais) * 100.0 : 0.0;
                        String statusJogo = (game.getRemainingShips() == 0) ? "Jogador (Fim do Jogo)" : "Em Curso";

                        String caminhoPdf = "reports/relatorio_partida.pdf";

                        PDFReportGenerator.generateReport(caminhoPdf, statusJogo, tirosTotais, afundados, precisao);
                    } else {
                        System.out.println("Ainda não existe nenhum jogo em curso para gerar PDF!");
                    }
                    break;
                case AJUDA:
                    menuHelp();
                    break;
                default:
                    System.out.println("Que comando é esse??? Repete ...");
            }
            System.out.print("> ");
            command = in.next();
        }
        System.out.println(GOODBYE_MESSAGE);
    }

    public static void menuHelp() {
        System.out.println("======================= AJUDA DO MENU =========================");
        System.out.println("Digite um dos comandos abaixo para interagir com o jogo:");
        System.out.println("- " + GERAFROTA + ": Gera uma frota aleatória de navios.");
        System.out.println("- " + LEFROTA + ": Permite criar e carregar uma frota personalizada.");
        System.out.println("- " + STATUS + ": Mostra o status atual da frota.");
        System.out.println("- " + MAPA + ": Exibe o mapa da frota.");
        System.out.println("- " + RAJADA + ": Realiza uma rajada de disparos.");
        System.out.println("- " + SIMULA + ": Simula um jogo completo.");
        System.out.println("- " + TIROS + ": Lista os tiros válidos realizados (* = tiro em navio, o = tiro na água)");
        System.out.println("- " + GUARDAR + ": Guarda o jogo atual em saves/savegame.json.");
        System.out.println("- " + CARREGAR + ": Carrega o jogo guardado e permite continuar.");
        System.out.println("- " + EXPORTAR + ": Exporta o histórico das jogadas para um ficheiro Excel.");
        System.out.println("- " + PDF + ": Gera um relatório da partida em formato PDF.");
        System.out.println("- " + DESISTIR + ": Encerra o jogo.");
        System.out.println("===============================================================");
    }

    public static Fleet buildFleet(Scanner in) {
        assert in != null;

        Fleet fleet = new Fleet();
        int i = 0;
        while (i < Fleet.FLEET_SIZE) {
            IShip s = readShip(in);
            if (s != null) {
                boolean success = fleet.addShip(s);
                if (success)
                    i++;
                else
                    LOGGER.info("Falha na criacao de {} {} {}", s.getCategory(), s.getBearing(), s.getPosition());
            } else {
                LOGGER.info("Navio desconhecido!");
            }
        }
        LOGGER.info("{} navios adicionados com sucesso!", i);
        return fleet;
    }

    public static Ship readShip(Scanner in) {
        assert in != null;

        String shipKind = in.next();
        Position pos = readPosition(in);
        char c = in.next().charAt(0);
        Compass bearing = Compass.charToCompass(c);
        return Ship.buildShip(shipKind, bearing, pos);
    }

    public static Position readPosition(Scanner in) {
        assert in != null;

        int row = in.nextInt();
        int column = in.nextInt();
        return new Position(row, column);
    }

    public static IPosition readClassicPosition(@NotNull Scanner in) {
        if (!in.hasNext()) {
            throw new IllegalArgumentException("Nenhuma posição válida encontrada!");
        }

        String part1 = in.next();
        String part2 = null;

        if (in.hasNextInt()) {
            part2 = in.next();
        }

        String input = (part2 != null) ? part1 + part2 : part1;
        input = input.toUpperCase();

        if (input.matches("[A-Z]\\d+")) {
            char column = input.charAt(0);
            int row = Integer.parseInt(input.substring(1));
            return new Position(column, row);
        } else if (part2 != null && part1.matches("[A-Z]") && part2.matches("\\d+")) {
            char column = part1.charAt(0);
            int row = Integer.parseInt(part2);
            return new Position(column, row);
        } else {
            throw new IllegalArgumentException("Formato inválido. Use 'A3', 'A 3' ou similar.");
        }
    }
}