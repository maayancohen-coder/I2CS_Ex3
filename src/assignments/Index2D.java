package assignments;

public class Index2D implements Pixel2D {
    private int _x, _y;
    public Index2D() {
        this(0,0);
    }
    public Index2D(int x, int y) {
        _x=x;_y=y;
    }
    public Index2D(Pixel2D t) {
        this(t.getX(), t.getY());
    }
    @Override
    public int getX() {
        return _x;
    }
    @Override
    public int getY() {
        return _y;
    }
    /**
     * Computes the Euclidean distance between this pixel and another pixel.
     * The calculation is based on the formula:
     * sqrt((x1 - x2)^2 + (y1 - y2)^2).
     *
     * @param t another pixel
     * @return the Euclidean distance between the two pixels
     * @throws RuntimeException if p2 is null
     */
    public double distance2D(Pixel2D t) {
        checkNotNull(t);
        int dx = _x - t.getX();
        int dy = _y - t.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
    public Index2D(String s) {
        try {
            String[] a = s.split(",");
            double x_double = Double.parseDouble(a[0]);
            double y_double = Double.parseDouble(a[1]);
            this._x = (int) x_double;
            this._y = (int) y_double;
        } catch (Exception e) {
            System.err.println("Error parsing position string: " + s);
            this._x = 0;
            this._y = 0;
        }
    }
    @Override
    public String toString() {
        return getX()+","+getY();
    }
    @Override
    public boolean equals(Object t) {
        boolean ans = false;
       /////// you do NOT need to add your code below ///////
        if(t instanceof Pixel2D) {
            Pixel2D p = (Pixel2D) t;
            ans = (this.distance2D(p)==0);
        }
       ///////////////////////////////////
        return ans;
    }
    private void checkNotNull(Pixel2D p) {
        if (p == null) {
            throw new RuntimeException("Pixel2D cannot be null");
        }
    }
}
