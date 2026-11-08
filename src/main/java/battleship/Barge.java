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


	public void metodoMonstroDeTeste() {
		int x = 0;
		if (x == 0) { x++; }
		if (x == 1) { x++; }
		if (x == 2) { x++; }
		if (x == 3) { x++; }
		if (x == 4) { x++; }
		if (x == 5) { x++; }
		if (x == 6) { x++; }
		if (x == 7) { x++; }
		if (x == 8) { x++; }
		if (x == 9) { x++; }
		if (x == 10) { x++; }
		if (x == 11) { x++; }
		if (x == 12) { x++; }
		if (x == 13) { x++; }
		if (x == 14) { x++; }
		if (x == 15) { x++; }
		if (x == 16) { x++; }
		if (x == 17) { x++; }
		if (x == 18) { x++; }
		if (x == 19) { x++; }
		if (x == 20) { x++; }
		System.out.println("Método gigante de teste para chumbar o Quality Gate: " + x);
	}
}