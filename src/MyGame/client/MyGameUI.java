package MyGame.client;

import MyGame.server.MyGameServer;
import assignments.StdDraw;
import exe.ex3.game.Game;

import java.awt.*;
import java.net.URL;

/**
 * Client-side renderer for the game.
 *
 * OVERVIEW
 * --------
 * This class draws the current game state on screen using StdDraw.
 * It is responsible for visualization only.
 * It does not update game logic, does not move entities, and does not handle input.
 *
 * The server holds the full game state and rules.
 * This renderer receives snapshots of that state and converts them into a single frame.
 *
 * WHAT IS RENDERED
 * ---------------
 * HUD:
 *   A single text line above the board (score, mode, instructions, etc).
 *
 * Board:
 *   Blue tiles represent walls.
 *   Pink tiles represent regular pellets.
 *   Green tiles represent power pellets.
 *
 * Characters:
 *   Pac-Man is drawn from a directional sprite based on the current direction.
 *   If the sprite resource is missing, a yellow circle is drawn as a fallback.
 *
 *   Ghosts are drawn as sprites only after they are released.
 *   When a ghost is eatable, it is drawn smaller for a clear visual cue.
 *
 * COORDINATES
 * -----------
 * The board is indexed as board[x][y].
 * x is the column index, y is the row index.
 *
 * Each cell is drawn as a square of size cell by cell pixels.
 * A HUD strip of height hud pixels is reserved above the board.
 *
 * RESPONSIBILITIES
 * ---------------
 * Initialize the drawing canvas and coordinate scaling.
 * Draw one frame: HUD, board tiles, ghosts, and Pac-Man.
 * Draw the end screen.
 */
public class MyGameUI {

    private final int cell;
    private final int hud;

    private int canvasW = 0;
    private int canvasH = 0;

    private final int BLUE  = Game.getIntColor(Color.BLUE, 0);
    private final int PINK  = Game.getIntColor(Color.PINK, 0);
    private final int GREEN = Game.getIntColor(Color.GREEN, 0);

    /**
     * Creates a new UI renderer.
     *
     * @param cell pixel size of one grid cell
     * @param hud  height in pixels reserved for the HUD area above the board
     */
    public MyGameUI(int cell, int hud) {
        this.cell = cell;
        this.hud = hud;
    }

    /**
     * Initializes the StdDraw canvas and coordinate system.
     *
     * Coordinate setup:
     * The drawing area spans from 0 to canvasW on the x-axis,
     * and from 0 to canvasH on the y-axis.
     * The HUD area is located above the board.
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
     * DRAW ORDER
     * ----------
     * 1. Clear background
     * 2. Draw HUD line
     * 3. Draw board tiles (walls and pellets)
     * 4. Draw ghosts (released only)
     * 5. Draw Pac-Man (directional sprite with fallback)
     *
     * Notes:
     * This method performs rendering only.
     * It does not change the server state.
     *
     * @param board   board tile matrix indexed as board[x][y]
     * @param pacX    Pac-Man x position in cells
     * @param pacY    Pac-Man y position in cells
     * @param pacDir  Pac-Man direction (server constants)
     * @param ghosts  server ghost array (may be null)
     * @param hudLine HUD text to display (may be null)
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

                double cx = x * cell + cell * 0.5;
                double cy = y * cell + cell * 0.5;

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
                else if (v == PINK) {
                    StdDraw.setPenColor(Color.PINK);
                    StdDraw.filledCircle(cx, cy, cell * 0.08);
                }
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

                // ✅ קטן משמעותית כשהרוח אכילה
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
            StdDraw.setPenColor(Color.YELLOW);
            StdDraw.filledCircle(px, py, cell * 0.28);
        }

        StdDraw.show();
    }

    /**
     * Draws the end screen.
     *
     * This method renders a static message only.
     * It does not change game state and does not handle input.
     * The caller decides when to exit the program.
     *
     * @param won true if the player won, false if the player lost
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
