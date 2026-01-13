package MyGame.server;

/**
 * Builds Pac-Man levels (board + important geometry) in a clean, testable way.
 *
 * <p>Why separate this?
 * <ul>
 *   <li>Map-building is large and changes often (new levels, tuning tunnels, ghost-house).</li>
 *   <li>Keeping it out of the server reduces clutter and risk of breaking game logic.</li>
 * </ul>
 *
 * <p>The builder does not run game logic; it only constructs a static level snapshot.</p>
 */
public final class LevelBuilder {

    private LevelBuilder() { }

    /**
     * Immutable snapshot of the built level: board + spawn positions + ghost-house geometry.
     */
    public static final class LevelData {
        public final int[][] board;

        public final int pacX, pacY;

        // center
        public final int cx, cy;

        // ghost-house geometry
        public final int hy0, hy1;
        public final int doorX, doorY;

        // scoring/win counters (pink only)
        public final int pinkLeft;
        public final int pinkTotal;

        private LevelData(int[][] board,
                          int pacX, int pacY,
                          int cx, int cy,
                          int hy0, int hy1,
                          int doorX, int doorY,
                          int pinkLeft, int pinkTotal) {
            this.board = board;
            this.pacX = pacX;
            this.pacY = pacY;
            this.cx = cx;
            this.cy = cy;
            this.hy0 = hy0;
            this.hy1 = hy1;
            this.doorX = doorX;
            this.doorY = doorY;
            this.pinkLeft = pinkLeft;
            this.pinkTotal = pinkTotal;
        }
    }

    /**
     * Builds the default level (same maze as your current working version).
     *
     * @param BLUE  encoded wall value
     * @param PINK  encoded dot value (win condition)
     * @param GREEN encoded power dot value (super mode trigger)
     */
    public static LevelData buildDefault(int BLUE, int PINK, int GREEN) {

        int w = 19, h = 15;
        int[][] b = new int[w][h];

        java.util.function.BiConsumer<Integer,Integer> wall = (x,y) -> {
            if (x>=0 && x<w && y>=0 && y<h) b[x][y] = BLUE;
        };
        java.util.function.BiConsumer<Integer,Integer> open = (x,y) -> {
            if (x>=0 && x<w && y>=0 && y<h) b[x][y] = 0;
        };

        // fill walls
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

        int cx = w/2, cy = h/2;

        // connectors
        open.accept(cx, 2);     open.accept(cx, 3);
        open.accept(cx, h-3);   open.accept(cx, h-4);
        open.accept(2, cy);     open.accept(3, cy);
        open.accept(w-3, cy);   open.accept(w-4, cy);

        // ghost house
        int hx0 = cx - 3, hx1 = cx + 3;
        int hy0 = cy - 2, hy1 = cy + 2;

        for (int x=hx0; x<=hx1; x++) { wall.accept(x, hy0); wall.accept(x, hy1); }
        for (int y=hy0; y<=hy1; y++) { wall.accept(hx0, y); wall.accept(hx1, y); }

        for (int x=hx0+1; x<=hx1-1; x++)
            for (int y=hy0+1; y<=hy1-1; y++)
                open.accept(x, y);

        // door (down)
        int doorX = cx;
        int doorY = hy0 - 1;
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
        int pacX = cx;
        int pacY = hy1 + 2;
        b[pacX][pacY] = 0;

        // count ONLY PINK for win
        int pinkLeft = 0;
        for (int x=0; x<w; x++) for (int y=0; y<h; y++) if (b[x][y] == PINK) pinkLeft++;
        int pinkTotal = pinkLeft;

        return new LevelData(b, pacX, pacY, cx, cy, hy0, hy1, doorX, doorY, pinkLeft, pinkTotal);
    }
}
