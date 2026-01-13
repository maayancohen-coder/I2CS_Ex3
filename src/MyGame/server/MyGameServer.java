package MyGame.server;

import exe.ex3.game.Game;
import exe.ex3.game.PacmanGame;

import java.awt.*;
import java.util.Random;

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
    private int pinkLeft = 0;     // remaining
    private int pinkTotal = 0;    // initial total
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

        // edible timer
        public long eatableUntilMs = 0;

        public boolean isEatable() {
            return System.currentTimeMillis() < eatableUntilMs;
        }

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
        buildDefaultLevel();
        initGhosts();
        startMs = System.currentTimeMillis();
        status = PLAY;
    }

    private void initGhosts() {
        long now = System.currentTimeMillis();
        ghosts = new Ghost[] {
                new Ghost(cx - 1, cy, now + 0,    "/media/g1.png"),
                new Ghost(cx,     cy, now + 3000, "/media/g2.png"),
                new Ghost(cx + 1, cy, now + 6000, "/media/g3.png")
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

    // ---------- called each frame ----------
    public void tick() {
        if (paused || status != PLAY) return;

        // disable super mode when time ends
        if (superMode && System.currentTimeMillis() > superModeUntil) {
            superMode = false;
            if (ghosts != null) {
                for (Ghost g : ghosts) g.eatableUntilMs = 0;
            }
        }

        updateGhosts();
        checkCollisions();
    }

    // move pacman one step in a direction (no automatic ghost tick here)
    public void movePacByDir(int dir) {
        if (paused || status != PLAY) return;
        if (dir == UP || dir == DOWN || dir == LEFT || dir == RIGHT) {
            movePacOneStep(dir);
        }
    }

    private void movePacOneStep(int dir) {
        pacDir = dir;

        int nx = pacX, ny = pacY;
        if (dir == LEFT) nx--;
        else if (dir == RIGHT) nx++;
        else if (dir == UP) ny++;
        else if (dir == DOWN) ny--;

        if (cyclic) {
            if (nx < 0) nx = board.length - 1;
            if (nx >= board.length) nx = 0;
            if (ny < 0) ny = board[0].length - 1;
            if (ny >= board[0].length) ny = 0;
        } else {
            if (nx < 0 || ny < 0 || nx >= board.length || ny >= board[0].length) return;
        }

        if (board[nx][ny] == BLUE) return;

        if (board[nx][ny] == PINK) {
            score += 1;
            pinkLeft--;
        }

        if (board[nx][ny] == GREEN) {
            score += 5;
            activateSuperMode();
        }

        board[nx][ny] = 0;
        pacX = nx;
        pacY = ny;

        if (pinkLeft <= 0) {
            won = true;
            status = DONE;
        }
    }

    private void activateSuperMode() {
        superMode = true;
        superModeUntil = System.currentTimeMillis() + SUPER_DURATION_MS;

        if (ghosts != null) {
            for (Ghost g : ghosts) {
                g.eatableUntilMs = superModeUntil;
            }
        }
    }

    // ---------- ghosts ----------
    private void updateGhosts() {
        long now = System.currentTimeMillis();

        for (Ghost g : ghosts) {

            // release phase
            if (!g.released) {
                if (now < g.releaseAtMs) continue;

                int tx = doorX;
                int ty = doorY;

                if (g.x == tx && g.y == ty) {
                    g.released = true;
                    g.dir = DOWN;
                    g.lastMoveMs = now;
                    continue;
                }

                int nx = g.x;
                int ny = g.y;

                if (g.x < tx) nx = g.x + 1;
                else if (g.x > tx) nx = g.x - 1;
                else {
                    if (g.y > ty) ny = g.y - 1;
                    else if (g.y < ty) ny = g.y + 1;
                }

                if (isFree(nx, ny)) {
                    g.x = nx; g.y = ny;
                }
                continue;
            }

            // pacing
            if (now - g.lastMoveMs < ghostStepMs) continue;
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

                if (cyclic) {
                    if (nx < 0) nx = board.length - 1;
                    if (nx >= board.length) nx = 0;
                    if (ny < 0) ny = board[0].length - 1;
                    if (ny >= board[0].length) ny = 0;
                } else {
                    if (nx < 0 || ny < 0 || nx >= board.length || ny >= board[0].length) continue;
                }

                if (!isFree(nx, ny)) continue;

                g.dir = d;
                g.x = nx; g.y = ny;
                break;
            }
        }
    }

    private boolean isFree(int x, int y) {
        if (x < 0 || y < 0 || x >= board.length || y >= board[0].length) return false;
        return board[x][y] != BLUE;
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

            if (cyclic) {
                if (nx < 0) nx = board.length - 1;
                if (nx >= board.length) nx = 0;
                if (ny < 0) ny = board[0].length - 1;
                if (ny >= board[0].length) ny = 0;
            } else {
                if (nx < 0 || ny < 0 || nx >= board.length || ny >= board[0].length) continue;
            }

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

    private void checkCollisions() {
        if (status != PLAY || won) return;

        for (Ghost g : ghosts) {
            if (!g.released) continue;

            if (g.x == pacX && g.y == pacY) {
                if (g.isEatable()) {
                    score += 200;

                    // back to house
                    g.x = cx;
                    g.y = cy;
                    g.released = false;
                    g.dir = STAY;
                    g.lastMoveMs = 0;
                    g.releaseAtMs = System.currentTimeMillis() + 2000;
                    return;
                }

                won = false;
                status = DONE;
                return;
            }
        }
    }

    // ---------- map ----------
    private void buildDefaultLevel() {
        int w = 19, h = 15;
        int[][] b = new int[w][h];

        java.util.function.BiConsumer<Integer,Integer> wall = (x,y) -> {
            if (x>=0 && x<w && y>=0 && y<h) b[x][y] = BLUE;
        };
        java.util.function.BiConsumer<Integer,Integer> open = (x,y) -> {
            if (x>=0 && x<w && y>=0 && y<h) b[x][y] = 0;
        };

        for (int x=0; x<w; x++) for (int y=0; y<h; y++) b[x][y] = BLUE;

        // outer loop
        for (int x=1; x<=w-2; x++) { open.accept(x, 1); open.accept(x, h-2); }
        for (int y=1; y<=h-2; y++) { open.accept(1, y); open.accept(w-2, y); }

        // tunnels (2 rows)
        int t1 = h/2;
        int t2 = h/2 - 1;
        for (int ty : new int[]{t1, t2}) {
            open.accept(0, ty); open.accept(w-1, ty);
            open.accept(1, ty); open.accept(2, ty);
            open.accept(w-2, ty); open.accept(w-3, ty);
        }

        // inner loop
        for (int x=3; x<=w-4; x++) { open.accept(x, 3); open.accept(x, h-4); }
        for (int y=3; y<=h-4; y++) { open.accept(3, y); open.accept(w-4, y); }

        cx = w/2; cy = h/2;

        // connectors
        open.accept(cx, 2);     open.accept(cx, 3);
        open.accept(cx, h-3);   open.accept(cx, h-4);
        open.accept(2, cy);     open.accept(3, cy);
        open.accept(w-3, cy);   open.accept(w-4, cy);

        // ghost house
        int hx0 = cx - 3, hx1 = cx + 3;
        hy0 = cy - 2; hy1 = cy + 2;

        for (int x=hx0; x<=hx1; x++) { wall.accept(x, hy0); wall.accept(x, hy1); }
        for (int y=hy0; y<=hy1; y++) { wall.accept(hx0, y); wall.accept(hx1, y); }

        for (int x=hx0+1; x<=hx1-1; x++)
            for (int y=hy0+1; y<=hy1-1; y++)
                open.accept(x, y);

        // door (down)
        doorX = cx;
        doorY = hy0 - 1;
        open.accept(doorX, hy0);
        open.accept(doorX, doorY);

        // ring around house
        int rx0 = cx - 5, rx1 = cx + 5;
        int ry0 = cy - 3, ry1 = cy + 3;

        for (int x=rx0; x<=rx1; x++) { open.accept(x, ry0); open.accept(x, ry1); }
        for (int y=ry0; y<=ry1; y++) { open.accept(rx0, y); open.accept(rx1, y); }

        open.accept(cx, ry0); open.accept(cx, ry0-1);
        open.accept(cx, ry1); open.accept(cx, ry1+1);
        open.accept(rx0, cy); open.accept(rx0-1, cy);
        open.accept(rx1, cy); open.accept(rx1+1, cy);

        // dots: fill walkable with PINK
        for (int x=0; x<w; x++)
            for (int y=0; y<h; y++)
                if (b[x][y] != BLUE) b[x][y] = PINK;

        // power dots (GREEN)
        b[2][2] = GREEN; b[w-3][2] = GREEN;
        b[2][h-3] = GREEN; b[w-3][h-3] = GREEN;

        // clear inside house (no dots)
        for (int x=hx0; x<=hx1; x++)
            for (int y=hy0; y<=hy1; y++)
                if (b[x][y] != BLUE) b[x][y] = 0;

        b[doorX][hy0] = 0;
        b[doorX][doorY] = 0;

        // pac start above house
        pacX = cx;
        pacY = hy1 + 2;
        b[pacX][pacY] = 0;

        // count ONLY PINK for win
        pinkLeft = 0;
        for (int x=0; x<w; x++) {
            for (int y=0; y<h; y++) {
                if (b[x][y] == PINK) pinkLeft++;
            }
        }
        pinkTotal = pinkLeft;

        won = false;
        status = INIT;

        this.board = b;
    }
}
