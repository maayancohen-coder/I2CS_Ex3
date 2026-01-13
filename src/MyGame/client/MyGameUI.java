package MyGame.client;

import MyGame.server.MyGameServer;
import assignments.StdDraw;
import exe.ex3.game.Game;

import java.awt.*;
import java.net.URL;

/**
 * Client-side renderer (UI) for {@link MyGameServer}.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Initialize the drawing canvas and coordinate system.</li>
 *   <li>Render the board tiles (walls + pellets).</li>
 *   <li>Render Pac-Man and ghosts as sprites (with graceful fallback if resources are missing).</li>
 *   <li>Render a simple HUD line above the board.</li>
 * </ul>
 *
 * <p>Coordinates:</p>
 * <ul>
 *   <li>Board is indexed as {@code board[x][y]} (x = column, y = row).</li>
 *   <li>UI draws each cell in a {@code cell x cell} square.</li>
 *   <li>HUD is an additional vertical strip of {@code hud} pixels above the grid.</li>
 * </ul>
 */
public class MyGameUI {

    /** Pixel size of a single grid cell. */
    private final int cell;

    /** HUD height in pixels above the board area. */
    private final int hud;

    /** Canvas width in pixels (computed from board width * cell). */
    private int canvasW = 0;

    /** Canvas height in pixels (computed from board height * cell + hud). */
    private int canvasH = 0;

    /**
     * Tile codes as used by the engine (derived from {@link Game#getIntColor(Color, int)}).
     * We keep them here to render the server board consistently.
     */
    private final int BLUE  = Game.getIntColor(Color.BLUE, 0);
    private final int PINK  = Game.getIntColor(Color.PINK, 0);
    private final int GREEN = Game.getIntColor(Color.GREEN, 0);

    /**
     * Creates a UI renderer.
     *
     * @param cell pixel size of one board cell (e.g., 32)
     * @param hud  height in pixels for the HUD strip on top (e.g., 60)
     */
    public MyGameUI(int cell, int hud) {
        this.cell = cell;
        this.hud = hud;
    }

    /**
     * Initializes StdDraw canvas size and coordinate system.
     *
     * <p>The coordinate system is set so that (0,0) is the bottom-left corner of the board,
     * and the top area above the board is reserved for the HUD.</p>
     *
     * @param w board width in cells
     * @param h board height in cells
     */
    public void initCanvas(int w, int h) {
        canvasW = w * cell;
        canvasH = h * cell + hud;

        StdDraw.setCanvasSize(canvasW, canvasH);
        StdDraw.setXscale(0, canvasW);
        StdDraw.setYscale(0, canvasH);
        StdDraw.enableDoubleBuffering();
    }

    /**
     * Draws a single frame of the game.
     *
     * <p>Rendering order:</p>
     * <ol>
     *   <li>Clear background</li>
     *   <li>HUD text line</li>
     *   <li>Board tiles (walls + pellets)</li>
     *   <li>Ghost sprites (released only)</li>
     *   <li>Pac-Man sprite (direction-based), with fallback shape</li>
     * </ol>
     *
     * @param board   game board values (indexed {@code [x][y]})
     * @param pacX    Pac-Man x coordinate in cells
     * @param pacY    Pac-Man y coordinate in cells
     * @param pacDir  Pac-Man direction code (server constants: LEFT/RIGHT/UP/DOWN)
     * @param ghosts  server ghosts array (may be {@code null})
     * @param hudLine text to show in the HUD (may be {@code null})
     */
    public void draw(int[][] board, int pacX, int pacY, int pacDir, MyGameServer.Ghost[] ghosts, String hudLine) {
        int w = board.length;
        int h = board[0].length;

        StdDraw.clear(Color.BLACK);

        // HUD
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.setFont(new Font("Arial", Font.PLAIN, 14));
        StdDraw.text(canvasW * 0.5, h * cell + hud * 0.55, hudLine == null ? "" : hudLine);

        // Board
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int v = board[x][y];

                // cell center in pixels
                double cx = x * cell + cell * 0.5;
                double cy = y * cell + cell * 0.5;

                // Walls: draw only the visible edges (clean maze look)
                if (v == BLUE) {
                    StdDraw.setPenColor(new Color(0, 140, 255));
                    StdDraw.setPenRadius(0.004);

                    boolean up    = (y + 1 < h) && (board[x][y + 1] == BLUE);
                    boolean down  = (y - 1 >= 0) && (board[x][y - 1] == BLUE);
                    boolean left  = (x - 1 >= 0) && (board[x - 1][y] == BLUE);
                    boolean right = (x + 1 < w) && (board[x + 1][y] == BLUE);

                    double x0 = x * cell;
                    double x1 = x * cell + cell;
                    double y0 = y * cell;
                    double y1 = y * cell + cell;

                    if (!up)    StdDraw.line(x0, y1, x1, y1);
                    if (!down)  StdDraw.line(x0, y0, x1, y0);
                    if (!left)  StdDraw.line(x0, y0, x0, y1);
                    if (!right) StdDraw.line(x1, y0, x1, y1);

                    StdDraw.setPenRadius();
                }
                // Small pellet
                else if (v == PINK) {
                    StdDraw.setPenColor(Color.PINK);
                    StdDraw.filledCircle(cx, cy, cell * 0.08);
                }
                // Power pellet
                else if (v == GREEN) {
                    StdDraw.setPenColor(Color.GREEN);
                    StdDraw.filledCircle(cx, cy, cell * 0.14);
                }
            }
        }

        // Ghosts (images) - released only
        if (ghosts != null) {
            for (MyGameServer.Ghost g : ghosts) {
                if (!g.released) continue;

                double gx = g.x * cell + cell * 0.5;
                double gy = g.y * cell + cell * 0.5;

                // Visually smaller when ghost is eatable
                double scale = g.isEatable() ? 0.55 : 0.95;
                double gSize = cell * scale;

                URL gurl = MyGameUI.class.getResource(g.imgPath);
                if (gurl != null) {
                    StdDraw.picture(gx, gy, gurl.toString(), gSize, gSize);
                }
            }
        }

        // Pacman (image by direction)
        double px = pacX * cell + cell * 0.5;
        double py = pacY * cell + cell * 0.5;
        double size = cell * 0.95;

        String imgPath = "/media/p1.left.png";
        if (pacDir == MyGameServer.RIGHT) imgPath = "/media/p1.right.png";
        else if (pacDir == MyGameServer.UP) imgPath = "/media/p1.up.png";
        else if (pacDir == MyGameServer.DOWN) imgPath = "/media/p1.down.png";

        URL pacUrl = MyGameUI.class.getResource(imgPath);
        if (pacUrl != null) {
            StdDraw.picture(px, py, pacUrl.toString(), size, size);
        } else {
            // Fallback if sprite is missing
            StdDraw.setPenColor(Color.YELLOW);
            StdDraw.filledCircle(px, py, cell * 0.28);
        }

        StdDraw.show();
    }

    /**
     * Draws a simple end screen overlay.
     *
     * <p>This method does not change game state; it only renders a message.
     * The outer loop should handle reading the quit key and exiting.</p>
     *
     * @param won {@code true} for a win screen, {@code false} for game over
     */
    public void drawEndScreen(boolean won) {
        StdDraw.clear(Color.BLACK);

        StdDraw.setPenColor(Color.WHITE);
        StdDraw.setFont(new Font("Arial", Font.BOLD, 36));
        StdDraw.text(canvasW * 0.5, canvasH * 0.55, won ? "YOU WIN!" : "GAME OVER");

        StdDraw.setFont(new Font("Arial", Font.PLAIN, 18));
        StdDraw.text(canvasW * 0.5, canvasH * 0.45, "Press Q to quit");

        StdDraw.show();
    }
}
