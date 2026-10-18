/**
 * 
 */
package battleship;

public class Main
{
	/**
	 * Main.
	 *
	 * @param args the args
	 */
	public static void main(String[] args)
    {
		System.out.println("***  Battleship  ***");

		// Teste de geracao do relatorio PDF
		PDFReportGenerator.generateReport("reports/relatorio_partida.pdf", "Jogador", 25, 11, 80.0);

		Tasks.menu();
    }
}
