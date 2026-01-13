package MyGame;

import assignments.StdDraw;
import exe.ex3.game.Game;

import java.awt.*;
import java.net.URL;

public class MyGameUI {

    private final int cell;
    private final int hud;

    // save canvas size (because StdDraw doesn't have getXscaleMax)
    private int canvasW = 0;
    private int canvasH = 0;

    private final int BLUE  = Game.getIntColor(Color.BLUE, 0);
    private final int PINK  = Game.getIntColor(Color.PINK, 0);
    private final int GREEN = Game.getIntColor(Color.GREEN, 0);

    public MyGameUI(int cell, int hud) {
        this.cell = cell;
        this.hud = hud;
    }

    public void initCanvas(int w, int h) {
        canvasW = w * cell;
        canvasH = h * cell + hud;

        StdDraw.setCanvasSize(canvasW, canvasH);
        StdDraw.setXscale(0, canvasW);
        StdDraw.setYscale(0, canvasH);
        StdDraw.enableDoubleBuffering();
    }

    public void draw(int[][] board, int pacX, int pacY, int pacDir, MyGame.Ghost[] ghosts, String hudLine) {
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

        // Ghosts (images) - show only after release (feel free to remove this if you want)
        if (ghosts != null) {
            long now = System.currentTimeMillis();

            for (MyGame.Ghost g : ghosts) {
                if (!g.released) continue;

                double gx = g.x * cell + cell * 0.5;
                double gy = g.y * cell + cell * 0.5;

                // 👻 גודל הרוח
                double gSize = cell * 0.95;
                if (g.isEatable(now)) {
                    gSize = cell * 0.45;   // יותר קטן
                }

                URL gurl = MyGameUI.class.getResource(g.imgPath);
                if (gurl != null) {
                    StdDraw.picture(gx, gy, gurl.toString(), gSize, gSize);
                }
            }
        }


        // Pacman (image by direction) - YOUR filenames
        double px = pacX * cell + cell * 0.5;
        double py = pacY * cell + cell * 0.5;
        double size = cell * 0.95;

        String imgPath = "/media/p1.left.png";
        if (pacDir == MyGame.RIGHT) imgPath = "/media/p1.right.png";
        else if (pacDir == MyGame.UP) imgPath = "/media/p1.up.png";
        else if (pacDir == MyGame.DOWN) imgPath = "/media/p1.down.png";

        URL pacUrl = MyGameUI.class.getResource(imgPath);
        if (pacUrl != null) {
            StdDraw.picture(px, py, pacUrl.toString(), size, size);
        } else {
            StdDraw.setPenColor(Color.YELLOW);
            StdDraw.filledCircle(px, py, cell * 0.28);
        }

        StdDraw.show();
    }

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
