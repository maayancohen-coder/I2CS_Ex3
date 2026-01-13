package MyGame.server;

public class GhostState {
    public final int x, y;
    public final boolean released;
    public final String imgPath;
    public final boolean eatable;

    public GhostState(int x, int y, boolean released, String imgPath, boolean eatable) {
        this.x = x; this.y = y;
        this.released = released;
        this.imgPath = imgPath;
        this.eatable = eatable;
    }
}
