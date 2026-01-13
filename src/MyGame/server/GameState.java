package MyGame.server;

public class GameState {
    public final int[][] board;      // copy or readonly ref (מומלץ copy)
    public final int pacX, pacY, pacDir;
    public final GhostState[] ghosts;
    public final int score, pinkLeft, pinkTotal;
    public final boolean paused, won;
    public final int status; // INIT/PLAY/DONE

    public GameState(int[][] board, int pacX, int pacY, int pacDir,
                     GhostState[] ghosts,
                     int score, int pinkLeft, int pinkTotal,
                     boolean paused, boolean won, int status) {
        this.board = board;
        this.pacX = pacX; this.pacY = pacY; this.pacDir = pacDir;
        this.ghosts = ghosts;
        this.score = score; this.pinkLeft = pinkLeft; this.pinkTotal = pinkTotal;
        this.paused = paused; this.won = won; this.status = status;
    }
}
