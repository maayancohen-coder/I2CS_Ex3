package MyGame.server;

import exe.ex3.game.Game;
import exe.ex3.game.PacmanGame;

import java.awt.*;
import java.util.Random;

/**
 * Server-side game logic for your Pac-Man project.
 *
 * <p>This class contains ONLY game rules and state:
 * board, Pac-Man movement, ghost movement, collisions, scoring, and super-mode timers.</p>
 *
 * <p>Input handling (keyboard) and rendering (StdDraw) are client-side responsibilities.</p>
 */
public class MyGameServer {

    // ===== Directions (align with engine) =====
    public static final int STAY  = PacmanGame.STAY;
    public static final int LEFT  = PacmanGame.LEFT;
    public static final int RIGHT = PacmanGame.RIGHT;
    public static final int UP    = PacmanGame.UP;
    public static final int DOWN  = PacmanGame.DOWN;

    // ===== Status =====
    public static final int INIT = 0;
    public static final int PLAY = 1;
    public static final int DONE = 2;

    // ===== Encoded colors =====
    private final int BLUE  = Game.getIntColor(Color.BLUE, 0);   // wall
    private final int PINK  = Game.getIntColor(Color.PINK, 0);   // dot
    private final int GREEN = Game.getIntColor(Color.GREEN, 0);  // power dot

    // ===== Board / Pacman =====
    private int[][] board;
    private int pacX, pacY;
    private int pacDir = LEFT;

    private int score = 0;
    private boolean cyclic = true;
    private boolean paused = false;
    private int status = INIT;

    // ===== Win condition: only PINK =====
    private int pinkLeft = 0;
    private int pinkTotal = 0;
    private boolean won = false;

    public int getPinkLeft()  { return pinkLeft; }
    public int getPinkTotal() { return pinkTotal; }
    public int getPinkEaten() { return pinkTotal - pinkLeft; }
    public boolean isWon() { return won; }

    // ===== Ghost House geometry =====
    private int cx, cy;
    private int hy0, hy1;
    private int doorX, doorY;

    // --- Super mode ---
    private boolean superMode = false;
    private long superModeUntil = 0;
    private static final long SUPER_DURATION_MS = 8000;

    // ===== Ghosts =====
    public static class Ghost {
        public int x, y;
        public int dir = STAY;
        public boolean released = false;
        public long releaseAtMs;
        public long lastMoveMs = 0;
        public String imgPath;

        /** Absolute time (ms) until which the ghost is edible. 0 = not edible. */
        public long eatableUntilMs = 0;

        public boolean isEatable() { return System.currentTimeMillis() < eatableUntilMs; }

        public Ghost(int x, int y, long releaseAtMs, String imgPath) {
            this.x = x;
            this.y = y;
            this.releaseAtMs = releaseAtMs;
            this.imgPath = imgPath;
        }
    }

    private Ghost[] ghosts;
    public Ghost[] getGhosts() { return ghosts; }

    private final Random rnd = new Random(1);
    private long ghostStepMs = 180;

    private long startMs = 0;
    public long getStartMs() { return startMs; }

    // ---------- init ----------
    public void initDefaultLevel() {
        loadDefaultLevel();   // ✅ now via LevelBuilder
        initGhosts();
        startMs = System.currentTimeMillis();
        status = PLAY;
    }

    private void loadDefaultLevel() {
        LevelBuilder.LevelData lvl = LevelBuilder.buildDefault(BLUE, PINK, GREEN);

        this.board = lvl.board;
        this.pacX = lvl.pacX;
        this.pacY = lvl.pacY;

        this.cx = lvl.cx;
        this.cy = lvl.cy;

        this.hy0 = lvl.hy0;
        this.hy1 = lvl.hy1;
        this.doorX = lvl.doorX;
        this.doorY = lvl.doorY;

        this.pinkLeft = lvl.pinkLeft;
        this.pinkTotal = lvl.pinkTotal;

        this.won = false;
        this.status = INIT;
        this.paused = false;
        this.superMode = false;
        this.superModeUntil = 0;
        this.score = 0;
        this.pacDir = LEFT;
    }

    private void initGhosts() {
        long now = System.currentTimeMillis();
        ghosts = new Ghost[] {
                new Ghost(cx - 1, cy, now + 0, "//g1.png"),
                new Ghost(cx,     cy, now + 3000, "//g2.png"),
                new Ghost(cx + 1, cy, now + 6000, "//g3.png")
        };
        for (Ghost g : ghosts) {
            g.released = false;
            g.dir = STAY;
            g.lastMoveMs = 0;
            g.eatableUntilMs = 0;
        }
    }

    // ---------- getters ----------
    public int[][] getBoard() { return board; }
    public int getPacX() { return pacX; }
    public int getPacY() { return pacY; }
    public int getPacDir() { return pacDir; }
    public int getScore() { return score; }
    public boolean isCyclic() { return cyclic; }
    public boolean isPaused() { return paused; }
    public int getStatus() { return status; }

    // ---------- toggles ----------
    public void togglePause() { paused = !paused; }
    public void toggleCyclic() { cyclic = !cyclic; }
    public void quit() { status = DONE; }

    // =========================================================
    // ===================== GAME LOOP API =====================
    // =========================================================

    /** Advances server-side time by one tick: timers, ghosts, collisions. */
    public void tick() {
        if (paused || status != PLAY) return;

        stepSuperTimers();
        updateGhosts();
        checkCollisions();
    }

    /** Moves Pac-Man by one cell in the given direction (no ghost movement here). */
    public void movePacByDir(int dir) {
        if (paused || status != PLAY) return;
        if (dir == UP || dir == DOWN || dir == LEFT || dir == RIGHT) {
            movePacOneStep(dir);
        }
    }

    // =========================================================
    // ===================== PACMAN LOGIC ======================
    // =========================================================

    private void movePacOneStep(int dir) {
        pacDir = dir;

        int nx = pacX, ny = pacY;
        if (dir == LEFT) nx--;
        else if (dir == RIGHT) nx++;
        else if (dir == UP) ny++;
        else if (dir == DOWN) ny--;

        int[] wrapped = wrapOrReject(nx, ny);
        if (wrapped == null) return;
        nx = wrapped[0]; ny = wrapped[1];

        if (board[nx][ny] == BLUE) return;

        handlePacCell(nx, ny);

        board[nx][ny] = 0;
        pacX = nx;
        pacY = ny;

        checkWinCondition();
    }

    private int[] wrapOrReject(int nx, int ny) {
        if (cyclic) {
            if (nx < 0) nx = board.length - 1;
            if (nx >= board.length) nx = 0;
            if (ny < 0) ny = board[0].length - 1;
            if (ny >= board[0].length) ny = 0;
            return new int[]{nx, ny};
        } else {
            if (nx < 0 || ny < 0 || nx >= board.length || ny >= board[0].length) return null;
            return new int[]{nx, ny};
        }
    }

    private void handlePacCell(int nx, int ny) {
        int cell = board[nx][ny];

        if (cell == PINK) {
            score += 1;
            pinkLeft--;
        } else if (cell == GREEN) {
            score += 5;
            activateSuperMode();
        }
    }

    private void checkWinCondition() {
        if (pinkLeft <= 0) {
            won = true;
            status = DONE;
        }
    }

    // =========================================================
    // ===================== SUPER MODE ========================
    // =========================================================

    private void activateSuperMode() {
        superMode = true;
        superModeUntil = System.currentTimeMillis() + SUPER_DURATION_MS;

        if (ghosts != null) {
            for (Ghost g : ghosts) g.eatableUntilMs = superModeUntil;
        }
    }

    private void stepSuperTimers() {
        if (superMode && System.currentTimeMillis() > superModeUntil) {
            superMode = false;
            if (ghosts != null) for (Ghost g : ghosts) g.eatableUntilMs = 0;
        }
    }

    // =========================================================
    // ===================== GHOST LOGIC =======================
    // =========================================================

    private void updateGhosts() {
        long now = System.currentTimeMillis();
        for (Ghost g : ghosts) {
            if (!g.released) stepGhostRelease(g, now);
            else stepGhostRandomWalk(g, now);
        }
    }

    /** Moves a ghost from the house to the corridor below the door (release sequence). */
    private void stepGhostRelease(Ghost g, long now) {
        if (now < g.releaseAtMs) return;

        int tx = doorX;
        int ty = doorY;

        if (g.x == tx && g.y == ty) {
            g.released = true;
            g.dir = DOWN;
            g.lastMoveMs = now;
            return;
        }

        int nx = g.x, ny = g.y;
        if (g.x < tx) nx = g.x + 1;
        else if (g.x > tx) nx = g.x - 1;
        else {
            if (g.y > ty) ny = g.y - 1;
            else if (g.y < ty) ny = g.y + 1;
        }

        if (isFree(nx, ny)) { g.x = nx; g.y = ny; }
    }

    /** Random walk with pacing and reverse-avoidance when possible. */
    private void stepGhostRandomWalk(Ghost g, long now) {
        if (now - g.lastMoveMs < ghostStepMs) return;
        g.lastMoveMs = now;

        int[] dirs = new int[]{UP, DOWN, LEFT, RIGHT};
        shuffle(dirs);

        for (int d : dirs) {
            if (isReverse(g.dir, d) && hasNonReverseOption(g)) continue;

            int nx = g.x, ny = g.y;
            if (d == LEFT) nx--;
            else if (d == RIGHT) nx++;
            else if (d == UP) ny++;
            else if (d == DOWN) ny--;

            int[] wrapped = wrapOrRejectGhost(nx, ny);
            if (wrapped == null) continue;
            nx = wrapped[0]; ny = wrapped[1];

            if (!isFree(nx, ny)) continue;

            g.dir = d;
            g.x = nx;
            g.y = ny;
            break;
        }
    }

    private int[] wrapOrRejectGhost(int nx, int ny) {
        if (cyclic) {
            if (nx < 0) nx = board.length - 1;
            if (nx >= board.length) nx = 0;
            if (ny < 0) ny = board[0].length - 1;
            if (ny >= board[0].length) ny = 0;
            return new int[]{nx, ny};
        } else {
            if (nx < 0 || ny < 0 || nx >= board.length || ny >= board[0].length) return null;
            return new int[]{nx, ny};
        }
    }

    private boolean isFree(int x, int y) {
        return !(x < 0 || y < 0 || x >= board.length || y >= board[0].length) && board[x][y] != BLUE;
    }

    private boolean isReverse(int a, int b) {
        return (a == UP && b == DOWN) || (a == DOWN && b == UP) ||
                (a == LEFT && b == RIGHT) || (a == RIGHT && b == LEFT);
    }

    private boolean hasNonReverseOption(Ghost g) {
        for (int d : new int[]{UP, DOWN, LEFT, RIGHT}) {
            if (isReverse(g.dir, d)) continue;

            int nx = g.x, ny = g.y;
            if (d == LEFT) nx--;
            else if (d == RIGHT) nx++;
            else if (d == UP) ny++;
            else if (d == DOWN) ny--;

            int[] wrapped = wrapOrRejectGhost(nx, ny);
            if (wrapped == null) continue;
            nx = wrapped[0]; ny = wrapped[1];

            if (isFree(nx, ny)) return true;
        }
        return false;
    }

    private void shuffle(int[] a) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = a[i]; a[i] = a[j]; a[j] = t;
        }
    }

    // =========================================================
    // ===================== COLLISIONS ========================
    // =========================================================

    private void checkCollisions() {
        if (status != PLAY || won) return;

        for (Ghost g : ghosts) {
            if (!g.released) continue;
            if (g.x == pacX && g.y == pacY) {
                resolvePacGhostCollision(g);
                return;
            }
        }
    }

    private void resolvePacGhostCollision(Ghost g) {
        if (g.isEatable()) eatGhost(g);
        else die();
    }

    private void eatGhost(Ghost g) {
        score += 200;

        g.x = cx;
        g.y = cy;
        g.released = false;
        g.dir = STAY;
        g.lastMoveMs = 0;
        g.releaseAtMs = System.currentTimeMillis() + 2000;
    }

    private void die() {
        won = false;
        status = DONE;
    }
}
