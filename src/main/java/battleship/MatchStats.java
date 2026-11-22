package battleship;

public record MatchStats(String winner, int totalShots, int sunkShips, double accuracy) {
}