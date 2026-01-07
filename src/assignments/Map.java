package assignments;

import java.util.ArrayList;
import java.util.ArrayDeque;

/**
 * Represents a 2D integer map.
 * Provides basic pixel access and BFS-based algorithms:
 * flood-fill, shortest path and all-distances.
 * @author maayan.cohen
 *
 */
public class Map implements Map2D {
    private int[][] _map;
    private boolean _cyclicFlag = true;

    /**
     * Constructs a w*h raster map initialized with value v.
     *
     * @param w width (x dimension)
     * @param h height (y dimension)
     * @param v initial value for all cells
     */
    public Map(int w, int h, int v) {
        init(w, h, v);
    }

    /**
     * Constructs a square map (size*size) initialized with 0.
     *
     * @param size width == height == size
     */
    public Map(int size) {
        this(size, size, 0);
    }

    /**
     * Constructs a map from a given matrix (deep copy).
     *
     * @param data source matrix (rectangular)
     */
    public Map(int[][] data) {
        init(data);
    }

    /**
     * Reinitializes the map to be w*h filled with v.
     *
     * @param w width (x dimension), must be positive
     * @param h height (y dimension), must be positive
     * @param v fill value
     * @throws RuntimeException if w<=0 or h<=0
     */
    @Override
    public void init(int w, int h, int v) {
        if (w <= 0 || h <= 0) throw new RuntimeException("Width and height must be positive");
        _map = new int[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                _map[x][y] = v;
            }
        }
    }

    /**
     * Reinitializes the map from a given matrix (deep copy).
     * The input must be rectangular (all rows same length).
     *
     * @param arr source matrix
     * @throws RuntimeException if arr is null/empty/not rectangular
     */
    @Override
    public void init(int[][] arr) {
        if (arr == null || arr.length == 0 || arr[0] == null || arr[0].length == 0) {
            throw new RuntimeException("Array must be non-null and non-empty");
        }
        int w = arr.length;
        int h = arr[0].length;
        for (int x = 0; x < w; x++) {
            if (arr[x] == null || arr[x].length != h) {
                throw new RuntimeException("Array must be rectangular (same length for all rows)");
            }
        }
        _map = new int[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                _map[x][y] = arr[x][y];
            }
        }
    }

    /**
     * @return a deep copy of the internal matrix.
     * @throws RuntimeException if map not initialized
     */
    @Override
    public int[][] getMap() {
        checkMapInitialized();
        int w = getWidth(), h = getHeight();
        int[][] ans = new int[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                ans[x][y] = _map[x][y];
            }
        }
        return ans;
    }

    /**
     * @return the map width (x dimension).
     * @throws RuntimeException if map not initialized
     */
    @Override
    public int getWidth() {
        checkMapInitialized();
        return _map.length;
    }

    /**
     * @return the map height (y dimension).
     * @throws RuntimeException if map not initialized
     */
    @Override
    public int getHeight() {
        checkMapInitialized();
        if (_map.length == 0) throw new RuntimeException("Map width 0");
        return _map[0].length;
    }

    /**
     * Returns the value stored at (x,y).
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return value at (x,y)
     * @throws RuntimeException if map not initialized or out of bounds
     */
    @Override
    public int getPixel(int x, int y) {
        checkMapInitialized();
        if (!isInsideXY(x, y)) throw new RuntimeException("Pixel (" + x + "," + y + ") is out of bounds");
        return _map[x][y];
    }

    /**
     * Returns the value stored at pixel p.
     *
     * @param p pixel
     * @return value at p
     * @throws RuntimeException if map not initialized, p is null, or out of bounds
     */
    @Override
    public int getPixel(Pixel2D p) {
        checkMapInitialized();
        if (p == null) throw new RuntimeException("Pixel2D is null");
        return getPixel(p.getX(), p.getY());
    }

    /**
     * Sets the value stored at (x,y).
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param v new value
     * @throws RuntimeException if map not initialized or out of bounds
     */
    @Override
    public void setPixel(int x, int y, int v) {
        checkMapInitialized();
        if (!isInsideXY(x, y)) throw new RuntimeException("Pixel (" + x + "," + y + ") is out of bounds");
        _map[x][y] = v;
    }

    /**
     * Sets the value stored at pixel p.
     *
     * @param p pixel
     * @param v new value
     * @throws RuntimeException if map not initialized, p is null, or out of bounds
     */
    @Override
    public void setPixel(Pixel2D p, int v) {
        checkMapInitialized();
        if (p == null) throw new RuntimeException("Pixel2D is null");
        setPixel(p.getX(), p.getY(), v);
    }

    /**
     * Flood-fill (BFS) starting from {@code xy}.
     * Replaces all 4-connected cells having the same value as {@code xy}
     * with {@code new_v}. Cyclic wrapping is respected if enabled.
     *
     * @param xy start pixel
     * @param new_v new value to assign
     * @return number of pixels changed
     */
    @Override
    public int fill(Pixel2D xy, int new_v) {
        requirePixel(xy);
        if (!isInside(xy)) return 0;

        int old_v = getPixel(xy);
        if (old_v == new_v) return 0;

        int w = getWidth(), h = getHeight();
        boolean[][] visited = new boolean[w][h];

        ArrayDeque<Pixel2D> q = new ArrayDeque<>();
        q.addLast(new Index2D(xy));
        visited[xy.getX()][xy.getY()] = true;

        int count = 0;

        while (!q.isEmpty()) {
            Pixel2D p = q.removeFirst();
            if (getPixel(p) != old_v) continue;

            setPixel(p, new_v);
            count++;

            for (Pixel2D nb : neighbors4(p)) {
                enqueueIfMatch(nb, old_v, visited, q);
            }
        }
        return count;
    }

    /**
     * Computes the shortest path from {@code p1} to {@code p2} using BFS (4-neighbors).
     * Cells with value {@code obsColor} are treated as obstacles.
     * Cyclic wrapping is respected if enabled.
     *
     * @param p1 start pixel
     * @param p2 target pixel
     * @param obsColor obstacle value
     * @return shortest path including p1 and p2, or null if unreachable
     */
    @Override
    public Pixel2D[] shortestPath(Pixel2D p1, Pixel2D p2, int obsColor) {
        requirePixel(p1);
        requirePixel(p2);

        if (!isWalkable(p1, obsColor) || !isWalkable(p2, obsColor)) return null;
        if (p1.equals(p2)) return new Pixel2D[]{ new Index2D(p1) };

        int w = getWidth(), h = getHeight();
        boolean[][] visited = new boolean[w][h];
        Pixel2D[][] parent = new Pixel2D[w][h];

        ArrayDeque<Pixel2D> q = new ArrayDeque<>();
        q.addLast(new Index2D(p1));
        visited[p1.getX()][p1.getY()] = true;

        while (!q.isEmpty()) {
            Pixel2D cur = q.removeFirst();

            for (Pixel2D nb : neighbors4(cur)) {
                if (!isWalkable(nb, obsColor)) continue;

                int x = nb.getX(), y = nb.getY();
                if (visited[x][y]) continue;

                visited[x][y] = true;
                parent[x][y] = cur;

                if (nb.equals(p2)) {
                    return buildPath(p1, p2, parent);
                }
                q.addLast(new Index2D(nb));
            }
        }
        return null;
    }

    /**
     * Computes the shortest distance from {@code start} to all reachable cells using BFS.
     * Obstacle cells (with value {@code obsColor}) are ignored.
     *
     * @param start start pixel
     * @param obsColor obstacle value
     * @return a map where each cell holds its distance from start, or -1 if unreachable
     */
    @Override
    public Map2D allDistance(Pixel2D start, int obsColor) {
        requirePixel(start);
        if (!isInside(start)) return null;

        Map ans = new Map(getWidth(), getHeight(), -1);
        ans.setCyclic(isCyclic());

        if (getPixel(start) == obsColor) return ans;

        ArrayDeque<Pixel2D> q = new ArrayDeque<>();
        q.addLast(new Index2D(start));
        ans.setPixel(start, 0);

        while (!q.isEmpty()) {
            Pixel2D cur = q.removeFirst();
            int d = ans.getPixel(cur);

            for (Pixel2D nb : neighbors4(cur)) {
                if (!isInside(nb)) continue;
                if (getPixel(nb) == obsColor) continue;
                if (ans.getPixel(nb) != -1) continue;

                ans.setPixel(nb, d + 1);
                q.addLast(new Index2D(nb));
            }
        }
        return ans;
    }

    /**
     * @param p pixel
     * @return true iff pixel p is inside bounds
     */
    @Override
    public boolean isInside(Pixel2D p) {
        if (_map == null || p == null) return false;
        return isInsideXY(p.getX(), p.getY());
    }

    /**
     * @return true iff this map is cyclic (wrap-around enabled)
     */
    @Override
    public boolean isCyclic() {
        return _cyclicFlag;
    }

    /**
     * Enables/disables cyclic (wrap-around) borders.
     *
     * @param cy true = cyclic, false = non-cyclic
     */
    @Override
    public void setCyclic(boolean cy) {
        _cyclicFlag = cy;
    }

    // ----------------- private helpers -----------------

    /** @throws RuntimeException if the map has not been initialized. */
    private void checkMapInitialized() {
        if (_map == null) throw new RuntimeException("Map is not initialized");
    }

    /** @return true iff (x,y) is inside bounds. */
    private boolean isInsideXY(int x, int y) {
        return x >= 0 && x < getWidth() && y >= 0 && y < getHeight();
    }

    /**
     * Returns the 4-neighborhood of p (up, left, down, right).
     * If cyclic is enabled, neighbors wrap around the borders.
     *
     * @param p center pixel
     * @return array of 4 neighbor pixels (some may be outside if non-cyclic)
     */
    private Pixel2D[] neighbors4(Pixel2D p) {
        int x = p.getX(), y = p.getY();
        int w = getWidth(), h = getHeight();

        int leftX = x - 1, rightX = x + 1;
        int upY = y - 1, downY = y + 1;

        if (isCyclic()) {
            leftX = wrap(leftX, w);
            rightX = wrap(rightX, w);
            upY = wrap(upY, h);
            downY = wrap(downY, h);
        }

        return new Pixel2D[] {
                new Index2D(x, upY),
                new Index2D(leftX, y),
                new Index2D(x, downY),
                new Index2D(rightX, y)
        };
    }

    /**
     * Wraps an index into [0, max-1] (modulo arithmetic).
     *
     * @param v index (may be negative or >= max)
     * @param max dimension size
     * @return wrapped index in [0..max-1]
     */
    private int wrap(int v, int max) {
        int r = v % max;
        if (r < 0) r += max;
        return r;
    }

    /**
     * Reconstructs a path from p1 to p2 using the parent matrix.
     * Assumes p2 is reachable (i.e., parent chain exists).
     *
     * @param p1 start pixel
     * @param p2 target pixel
     * @param parent parent pointers: parent[x][y] is the predecessor on the BFS tree
     * @return path including p1 and p2, or null on inconsistent parent chain
     */
    private Pixel2D[] buildPath(Pixel2D p1, Pixel2D p2, Pixel2D[][] parent) {
        ArrayList<Pixel2D> rev = new ArrayList<>();
        Pixel2D cur = new Index2D(p2);
        rev.add(cur);

        while (!cur.equals(p1)) {
            Pixel2D par = parent[cur.getX()][cur.getY()];
            if (par == null) return null;
            cur = par;
            rev.add(cur);
        }

        Pixel2D[] path = new Pixel2D[rev.size()];
        for (int i = 0; i < rev.size(); i++) {
            path[i] = rev.get(rev.size() - 1 - i);
        }
        return path;
    }

    /**
     * Ensures the map is initialized and the given pixel is not null.
     *
     * @param p pixel reference
     * @throws RuntimeException if map not initialized or p is null
     */
    private void requirePixel(Pixel2D p) {
        checkMapInitialized();
        if (p == null) throw new RuntimeException("Pixel2D is null");
    }

    /**
     * @return true iff p is inside the map and not an obstacle.
     */
    private boolean isWalkable(Pixel2D p, int obsColor) {
        return isInside(p) && getPixel(p) != obsColor;
    }

    /**
     * Enqueues nb for flood-fill iff it is inside, not visited yet, and equals matchValue.
     * Also marks the neighbor as visited at enqueue time (prevents duplicates in the queue).
     */
    private void enqueueIfMatch(Pixel2D nb, int matchValue,
                                boolean[][] visited, ArrayDeque<Pixel2D> q) {
        if (!isInside(nb)) return;
        int x = nb.getX(), y = nb.getY();
        if (visited[x][y]) return;
        if (getPixel(nb) != matchValue) return;

        visited[x][y] = true;
        q.addLast(new Index2D(nb));
    }
}
