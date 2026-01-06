package assignments;

/**
 * This class represents a 2D map as a "screen" or a raster matrix or maze over integers.
 * @author boaz.benmoshe
 *
 */
public class Map implements Map2D {
	private int[][] _map;
	private boolean _cyclicFlag = true;
	
	/**
	 * Constructs a w*h 2D raster map with an init value v.
	 * @param w
	 * @param h
	 * @param v
	 */
	public Map(int w, int h, int v) {
        init(w,h, v);
    }
	/**
	 * Constructs a square map (size*size).
	 * @param size
	 */
	public Map(int size) {
        this(size,size, 0);
    }
	
	/**
	 * Constructs a map from a given 2D array.
	 * @param data
	 */
	public Map(int[][] data) {

        init(data);
	}
    /**
     * Initializes (or reinitializes) this map to w*h filled with v.
     *
     * Pseudocode:
     * 1. if w<=0 or h<=0 throw
     * 2. allocate _map[w][h]
     * 3. for each cell set to v
     *
     * @param w width
     * @param h height
     * @param v fill value
     * @throws RuntimeException if w <= 0 or h <= 0
     */
	@Override
	public void init(int w, int h, int v) {
        if (w <= 0 || h <= 0) {
            throw new RuntimeException("Width and height must be positive");
        }
        _map = new int[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                _map[x][y] = v;
            }
        }
	}
    /**
     * Initializes this map from a given matrix (deep copy).
     * The input must be rectangular: all rows must have the same length.
     *
     * Pseudocode:
     * 1. validate arr not null and not empty
     * 2. validate rectangular shape
     * 3. allocate _map[w][h]
     * 4. copy all entries
     *
     * @param arr source matrix
     * @throws RuntimeException if arr is null/empty/not-rectangular
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
                this._map[x][y] = arr[x][y];
            }
        }
	}
    /**
     * Returns a deep copy of the map matrix.
     *
     * Pseudocode:
     * 1. allocate new matrix ans[width][height]
     * 2. copy all values from _map to ans
     * 3. return ans
     *
     * @return deep copy of internal matrix
     * @throws RuntimeException if map not initialized
     */
	@Override
	public int[][] getMap() {
        checkMapInitialized();
        int w = getWidth();
        int h = getHeight();
        int[][] ans = new int[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                ans[x][y] = this._map[x][y];
            }
        }
        return ans;
	}
    /**
     * Returns map width (x dimension).
     *
     * Pseudocode:
     * 1. return _map.length
     *
     * @return width
     * @throws RuntimeException if map not initialized
     */
	@Override
	public int getWidth() {
        checkMapInitialized();
        return _map.length;
    }
	@Override
    /**
     * Returns map height (y dimension).
     *
     * Pseudocode:
     * 1. return _map[0].length
     *
     * @return height
     * @throws RuntimeException if map not initialized
     */
	public int getHeight() {
        checkMapInitialized();
        if (_map.length == 0) {
            throw new RuntimeException("Map width 0");
        }
        return _map[0].length;
    }
	@Override
    /**
     * Returns the value stored at (x,y).
     *
     * Pseudocode:
     * 1. if (x,y) out of bounds throw
     * 2. return _map[x][y]
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return value at (x,y)
     * @throws RuntimeException if map not initialized or out of bounds
     */
	public int getPixel(int x, int y) {
        checkMapInitialized();
        if (!isInsideXY(x, y)) {
            throw new RuntimeException("Pixel (" + x + "," + y + ") is out of bounds");
        }
        return _map[x][y];
    }
	@Override
    /**
     * Returns the value stored at pixel p.
     *
     * Pseudocode:
     * 1. if p null throw
     * 2. return getPixel(p.x, p.y)
     *
     * @param p pixel
     * @return value at p
     * @throws RuntimeException if p null or out of bounds
     */
	public int getPixel(Pixel2D p) {
        checkMapInitialized();
        if (p == null) {
            throw new RuntimeException("Pixel2D is null");
        }
        return getPixel(p.getX(), p.getY());
	}
	@Override
    /**
     * Sets the value at (x,y) to v.
     *
     * Pseudocode:
     * 1. if (x,y) out of bounds throw
     * 2. _map[x][y] = v
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param v value to set
     * @throws RuntimeException if out of bounds
     */
	public void setPixel(int x, int y, int v) {
        checkMapInitialized();
        if (!isInsideXY(x, y)) {
            throw new RuntimeException("Pixel (" + x + "," + y + ") is out of bounds");
        }
        _map[x][y] = v;
    }
	@Override
    /**
     * Sets the value at pixel p to v.
     *
     * Pseudocode:
     * 1. if p null throw
     * 2. setPixel(p.x, p.y, v)
     *
     * @param p pixel coordinate
     * @param v value to set
     * @throws RuntimeException if p null or out of bounds
     */
	public void setPixel(Pixel2D p, int v) {
        checkMapInitialized();
        if (p == null) {
            throw new RuntimeException("Pixel2D is null");
        }
        setPixel(p.getX(), p.getY(), v);
	}
	@Override
	/** 
	 * Fills this map with the new color (new_v) starting from p.
	 * https://en.wikipedia.org/wiki/Flood_fill
	 */
	public int fill(Pixel2D xy, int new_v) {
		int ans=0;
		/////// add your code below ///////

		///////////////////////////////////
		return ans;
	}

	@Override
	/**
	 * BFS like shortest the computation based on iterative raster implementation of BFS, see:
	 * https://en.wikipedia.org/wiki/Breadth-first_search
	 */
	public Pixel2D[] shortestPath(Pixel2D p1, Pixel2D p2, int obsColor) {
		Pixel2D[] ans = null;  // the result.
		/////// add your code below ///////

		///////////////////////////////////
		return ans;
	}
	@Override
    /**
     * Returns true iff pixel p is inside bounds.
     *
     * Pseudocode:
     * 1. if map not initialized or p null -> false
     * 2. return bounds-check for p.x,p.y
     *
     * @param p pixel
     * @return true iff inside bounds
     */
	public boolean isInside(Pixel2D p) {
        if (_map == null || p == null) {
            return false;
        }
        return isInsideXY(p.getX(), p.getY());
	}

	@Override
	/////// add your code below ///////
	public boolean isCyclic() {
		return false;
	}
	@Override
	/////// add your code below ///////
	public void setCyclic(boolean cy) {;}
	@Override
	/////// add your code below ///////
	public Map2D allDistance(Pixel2D start, int obsColor) {
		Map2D ans = null;  // the result.
		/////// add your code below ///////

		///////////////////////////////////
		return ans;
	}
    private void checkMapInitialized() {
        if (_map == null) {
            throw new RuntimeException("Map is not initialized");
        }
    }
    private boolean isInsideXY(int x, int y) {
        return x >= 0 && x < getWidth() && y >= 0 && y < getHeight();
    }

}
