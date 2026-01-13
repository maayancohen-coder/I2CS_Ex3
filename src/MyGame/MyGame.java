package MyGame;

import java.awt.Color;
import java.util.Random;
import java.util.function.BiConsumer;

import assignments.GameInfo;
import assignments.StdDraw;
import exe.ex3.game.Game;

public class MyGame {

    // board codes for adapter-algo compatibility
    public static final int EMPTY = 0;
    public static final int BLUE  = Game.getIntColor(Color.BLUE, 0);   // wall
    public static final int PINK  = Game.getIntColor(Color.PINK, 0);   // dot
    public static final int GREEN = Game.getIntColor(Color.GREEN, 0);  // power

    // directions expected by MyGameUI (do not change UI)
    public static final int LEFT  = exe.ex3.game.PacmanGame.LEFT;
    public static final int RIGHT = exe.ex3.game.PacmanGame.RIGHT;
    public static final int UP    = exe.ex3.game.PacmanGame.UP;
    public static final int DOWN  = exe.ex3.game.PacmanGame.DOWN;

    public static final int PLAY = 1;
    public static final int END  = 0;

    public enum ControlMode { MANUAL, AUTO }

    private int[][] board;
    private boolean cyclic = true;

    private int status = PLAY;
    private boolean paused = false;
    private ControlMode controlMode = ControlMode.MANUAL;

    private int pacX, pacY;
    private Direction pacDir = Direction.LEFT;

    private Ghost[] ghosts;

    private int score = 0;
    private int pinkLeft = 0;

    // powered mode
    long poweredUntilMs = 0;

    // timing
    private long startMs = System.currentTimeMillis();

    // ghost house geometry
    int doorX;
    int hy0;

    private final Random rnd = new Random();

    // ---------- getters ----------
    public int[][] getBoard() { return board; }
    public int getStatus() { return status; }
    public boolean isPaused() { return paused; }
    public boolean isWon() { return pinkLeft <= 0 && status == END; }
    public ControlMode getControlMode() { return controlMode; }

    public int getPacX() { return pacX; }
    public int getPacY() { return pacY; }
    public Direction getPacDir() { return pacDir; }

    public Ghost[] getGhosts() { return ghosts; }

    public boolean isCyclic() { return cyclic; }
    public boolean isPowered() { return System.currentTimeMillis() < poweredUntilMs; }
    public long poweredRemainingMs() { return Math.max(0, poweredUntilMs - System.currentTimeMillis()); }

    public int getScore() { return score; }

    // ---------- init ----------
    public void initDefaultLevel() {
        int w = 21, h = 15;
        int[][] b = new int[w][h];

        // fill with dots
        for (int x=0; x<w; x++) for (int y=0; y<h; y++) b[x][y] = PINK;

        // border walls
        for (int x=0; x<w; x++) { b[x][0]=BLUE; b[x][h-1]=BLUE; }
        for (int y=0; y<h; y++) { b[0][y]=BLUE; b[w-1][y]=BLUE; }

        // tunnels
        b[0][h/2] = EMPTY;
        b[w-1][h/2] = EMPTY;

        // center ghost house area (EMPTY) - matches your algo expectation
        int cx = w/2, cy = h/2;
        for (int x=cx-2; x<=cx+2; x++) {
            for (int y=cy-2; y<=cy+2; y++) {
                b[x][y] = EMPTY;
            }
        }

        // door coords used in updateGhosts/collisions
        doorX = cx;
        hy0 = cy-2;

        // some power pellets
        b[2][2]=GREEN; b[w-3][2]=GREEN; b[2][h-3]=GREEN; b[w-3][h-3]=GREEN;

        // pac start
        pacX = cx;
        pacY = cy + 4;
        pacDir = Direction.LEFT;

        // ghosts start inside house with delays
        ghosts = new Ghost[4];
        ghosts[0] = new Ghost(cx, hy0+1, "media/g0.png", 0);
        ghosts[1] = new Ghost(cx-1, hy0+1, "media/g1.png", 2000);
        ghosts[2] = new Ghost(cx+1, hy0+1, "media/g2.png", 4000);
        ghosts[3] = new Ghost(cx, hy0+2, "media/g3.png", 6000);

        board = b;
        countPink();
        score = 0;
        status = PLAY;
        paused = false;
        controlMode = ControlMode.MANUAL;
        poweredUntilMs = 0;
        startMs = System.currentTimeMillis();
    }

    private void countPink() {
        int c=0;
        for (int x=0; x<board.length; x++)
            for (int y=0; y<board[0].length; y++)
                if (board[x][y] == PINK) c++;
        pinkLeft = c;
    }

    // ---------- input ----------
    public void handleKeyOnce(char ch) {
        ch = Character.toLowerCase(ch);

        if (ch == ' ') { paused = !paused; return; }
        if (ch == 'c') { cyclic = !cyclic; return; }
        if (ch == 'm') { controlMode = (controlMode == ControlMode.MANUAL ? ControlMode.AUTO : ControlMode.MANUAL); return; }
        if (ch == 'q') { status = END; return; }

        if (paused) return;

        if (controlMode == ControlMode.MANUAL) {
            if (ch == 'w') movePacOneStep(Direction.UP);
            if (ch == 's') movePacOneStep(Direction.DOWN);
            if (ch == 'a') movePacOneStep(Direction.LEFT);
            if (ch == 'd') movePacOneStep(Direction.RIGHT);
        }
    }

    // ---------- game loop ----------
    public void tick() {
        if (status != PLAY) return;
        if (paused) return;

        updateGhosts();
        handleCollisions();

        if (pinkLeft <= 0) status = END;
    }

    public void movePacByDir(int dir) {
        if (paused || status != PLAY) return;

        Direction d = Direction.NONE;
        if (dir == Game.UP) d = Direction.UP;
        if (dir == Game.DOWN) d = Direction.DOWN;
        if (dir == Game.LEFT) d = Direction.LEFT;
        if (dir == Game.RIGHT) d = Direction.RIGHT;

        movePacOneStep(d);
    }

    private void movePacOneStep(Direction d) {
        if (d == null || d == Direction.NONE) return;

        int nx = pacX + d.dx;
        int ny = pacY + d.dy;

        if (cyclic) {
            nx = (nx + board.length) % board.length;
            ny = (ny + board[0].length) % board[0].length;
        }

        if (board[nx][ny] == BLUE) return;
        if (isGhostHouseCell(nx, ny)) return;

        pacX = nx;
        pacY = ny;
        pacDir = d;

        // eat
        if (board[pacX][pacY] == PINK) {
            board[pacX][pacY] = EMPTY;
            score += 10;
            pinkLeft--;
        }
        if (board[pacX][pacY] == GREEN) {
            board[pacX][pacY] = EMPTY;
            score += 50;
            poweredUntilMs = System.currentTimeMillis() + 8000; // 8s
        }
    }

    private void updateGhosts() {
        long now = System.currentTimeMillis();

        // sync UI aliases & eatable timer (so UI compiles without changes)
        for (Ghost gh : ghosts) {
            if (gh == null) continue;
            gh.imgPath = gh.img;
            gh.released = gh.active;
            gh.eatableUntilMs = poweredUntilMs;
        }

        // simple random movement + house release
        for (int i = 0; i < ghosts.length; i++) {
            Ghost g = ghosts[i];

            if (!g.active && now >= g.releaseAtMs) {
                g.active = true;
                g.released = true;
            }

            if (!g.active) continue;

            // if still inside house -> move to door corridor first
            if (isInsideHouseArea(g.x, g.y)) {
                int tx = doorX;
                int ty = hy0 + 3;
                stepGhostTowards(g, tx, ty);
                continue;
            }

            // random walk
            int[] dx = {1,-1,0,0};
            int[] dy = {0,0,1,-1};

            int tries = 0;
            while (tries < 10) {
                int k = rnd.nextInt(4);
                int nx = g.x + dx[k];
                int ny = g.y + dy[k];

                if (cyclic) {
                    nx = (nx + board.length) % board.length;
                    ny = (ny + board[0].length) % board[0].length;
                }

                if (isGhostFree(nx, ny)) {
                    g.x = nx;
                    g.y = ny;
                    break;
                }
                tries++;
            }
        }
    }

    private void stepGhostTowards(Ghost g, int tx, int ty) {
        int bestX = g.x, bestY = g.y;
        int bestDist = dist(g.x, g.y, tx, ty);

        int[] dx = {1,-1,0,0};
        int[] dy = {0,0,1,-1};

        for (int k=0; k<4; k++) {
            int nx = g.x + dx[k];
            int ny = g.y + dy[k];

            if (cyclic) {
                nx = (nx + board.length) % board.length;
                ny = (ny + board[0].length) % board[0].length;
            }
            if (!isGhostFree(nx, ny)) continue;

            int d = dist(nx, ny, tx, ty);
            if (d < bestDist) {
                bestDist = d;
                bestX = nx;
                bestY = ny;
            }
        }

        g.x = bestX;
        g.y = bestY;
    }

    private int dist(int x1, int y1, int x2, int y2) {
        return Math.abs(x1-x2) + Math.abs(y1-y2);
    }

    private boolean isGhostFree(int x, int y) {
        if (board[x][y] == BLUE) return false;
        return true;
    }

    private void handleCollisions() {
        for (Ghost g : ghosts) {
            if (!g.active) continue;

            if (g.x == pacX && g.y == pacY) {
                if (isPowered()) {
                    // eat ghost: send back into house and delay release again
                    score += 200;
                    g.x = doorX;
                    g.y = hy0 + 1;
                    g.active = false;
                    g.released = false;
                    g.releaseAtMs = System.currentTimeMillis() + 3000;
                } else {
                    status = END;
                }
            }
        }
    }

    private boolean isInsideHouseArea(int x, int y) {
        int cx = board.length / 2;
        int cy = board[0].length / 2;
        return Math.abs(x - cx) < 3 && Math.abs(y - cy) < 3 && board[x][y] == EMPTY;
    }

    private boolean isGhostHouseCell(int x, int y) {
        return isInsideHouseArea(x, y);
    }

    String hudLine() {
        long sec = (System.currentTimeMillis() - startMs) / 1000;
        return "Score: " + score +
                " | PinkLeft: " + pinkLeft +
                " | " + (paused ? "PAUSE" : "PLAY") +
                " | Mode: " + controlMode +
                " | Power: " + (isPowered() ? (poweredRemainingMs()/1000.0 + "s") : "OFF") +
                " | t=" + sec + "s";
    }

    // ---------- nested ghost ----------
    public static class Ghost {
        public int x, y;

        // original fields used by game logic
        public String img;
        public boolean active = false;
        public long releaseAtMs;

        // fields/methods expected by MyGameUI (keep UI unchanged)
        public boolean released = false;     // alias of active
        public String imgPath;               // alias of img
        public long eatableUntilMs = 0;      // used by isEatable(now)

        public Ghost(int x, int y, String img, long delayMs) {
            this.x = x;
            this.y = y;

            this.img = img;
            this.imgPath = img;

            this.releaseAtMs = System.currentTimeMillis() + delayMs;
        }

        public boolean isEatable(long now) {
            return now < eatableUntilMs;
        }
    }
}
