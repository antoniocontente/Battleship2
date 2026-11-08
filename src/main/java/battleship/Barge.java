package battleship;

/**
 * The type Barge represents a ship with a size of 1 unit.
 * It is positioned on the game board based on its bearing and initial position.
 *
 * Author: britoeabreu
 * Date: 2023-10-10
 * Time: 15:30
 */
public class Barge extends Ship {

	/**
	 * Instantiates a new Barge.
	 *
	 * @param bearing The bearing of the barge (NORTH, SOUTH, EAST, or WEST).
	 * @param pos     The upper-left position of the barge on the game board.
	 */
	public Barge(Compass bearing, IPosition pos) {
		super("Barca", bearing, pos, 1);

		// Add the single position of the barge to the list of positions
		getPositions().add(new Position(pos.getRow(), pos.getColumn()));
	}

	// Violação Checkstyle (nome do método começa com maiúscula)
    // Violação Qodana (OverlyComplexMethod: 9 ifs ultrapassam o limiar 8)
    public void MetodoComErroViolacao(int x) {
        if (x == 1) System.out.println(1);
        if (x == 2) System.out.println(2);
        if (x == 3) System.out.println(3);
        if (x == 4) System.out.println(4);
        if (x == 5) System.out.println(5);
        if (x == 6) System.out.println(6);
        if (x == 7) System.out.println(7);
        if (x == 8) System.out.println(8);
        if (x == 9) System.out.println(9);
    }
}
