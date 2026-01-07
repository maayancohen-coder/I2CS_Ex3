package assignments;

import exe.ex3.game.Game;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;

import java.awt.*;

/**
 * This is the major algorithmic class for Ex3 - the PacMan game:
 *
 * This code is a very simple example (random-walk algorithm).
 * Your task is to implement (here) your PacMan algorithm.
 */
public class Ex3Algo implements PacManAlgo {
    private int _count;

    public Ex3Algo() {
        _count = 0;
    }

    @Override
    /**
     *  Add a short description for the algorithm as a String.
     */
    public String getInfo() {
        return null;
    }

    @Override
    /**
     * This ia the main method - that you should design, implement and test.
     */
    public int move(PacmanGame game) {
        int code = 0;
        int[][] board = game.getGame(0);
        printBoard(board);
        int blue = Game.getIntColor(Color.BLUE, code);
        int pink = Game.getIntColor(Color.PINK, code);
        int black = Game.getIntColor(Color.BLACK, code);
        int green = Game.getIntColor(Color.GREEN, code);
        if (_count == 0 || _count == 300) {
            System.out.println("Blue=" + blue + ", Pink=" + pink + ", Black=" + black + ", Green=" + green);
            String pos = game.getPos(code).toString();
            System.out.println("Pacman coordinate: " + pos);
            GhostCL[] ghosts = game.getGhosts(code);
            printGhosts(ghosts);
            int up = Game.UP, left = Game.LEFT, down = Game.DOWN, right = Game.RIGHT;
        }
        _count++;
        int dir = 0;
        Map map = new Map(board);
        Pixel2D posP = parsePos(game.getPos(code).toString());
        Pixel2D target = nearestFood(map, posP, pink, blue);
        Pixel2D [] path = map.shortestPath(posP, target, blue);
        if (path[1].getX()<posP.getX()){
            dir = Game.LEFT;
        }
        else if (path[1].getX()>posP.getX()){
            dir = Game.RIGHT;
        } else if (path[0].getY()<posP.getY()) {
            dir = Game.DOWN;
        }
        else if (path[0].getY()>posP.getY()){
            dir = Game.UP;
        }
        return dir;
    }

    private static void printBoard(int[][] b) {
        for (int y = 0; y < b[0].length; y++) {
            for (int x = 0; x < b.length; x++) {
                int v = b[x][y];
                System.out.print(v + "\t");
            }
            System.out.println();
        }
    }

    private static void printGhosts(GhostCL[] gs) {
        for (int i = 0; i < gs.length; i++) {
            GhostCL g = gs[i];
            System.out.println(i + ") status: " + g.getStatus() + ",  type: " + g.getType() + ",  pos: " + g.getPos(0) + ",  time: " + g.remainTimeAsEatable(0));
        }
    }

    private static int randomDir() {
        int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
        int ind = (int) (Math.random() * dirs.length);
        return dirs[ind];
    }

    private int firstLegalDir(int[][] board, Index2D pos) {
        int x = pos.getX(), y = pos.getY();
        int WALL = 1; // לפי Blue=1 אצלך

        // נסה למעלה, שמאלה, למטה, ימינה
        if (isLegal(board, x, y - 1, WALL)) return Game.UP;
        if (isLegal(board, x - 1, y, WALL)) return Game.LEFT;
        if (isLegal(board, x, y + 1, WALL)) return Game.DOWN;
        if (isLegal(board, x + 1, y, WALL)) return Game.RIGHT;

        return Game.STAY;
    }

    private boolean isLegal(int[][] board, int x, int y, int wallVal) {
        if (x < 0 || y < 0 || x >= board.length || y >= board[0].length) return false;
        return board[x][y] != wallVal;
    }

    private static Index2D parsePos(String s) {
        String[] p = s.trim().split(",");
        return new Index2D(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()));
    }

    private static Index2D nextPos(Index2D pos, int dir, int[][] board, boolean cyclic) {
        int w = board.length, h = board[0].length;
        int x = pos.getX(), y = pos.getY();

        if (dir == Game.UP) y--;
        else if (dir == Game.DOWN) y++;
        else if (dir == Game.LEFT) x--;
        else if (dir == Game.RIGHT) x++;

        if (cyclic) {
            x = (x % w + w) % w;
            y = (y % h + h) % h;
        } else {
            if (x < 0 || y < 0 || x >= w || y >= h) return null;
        }
        return new Index2D(x, y);
    }

    private static Pixel2D nearestFood(Map map, Pixel2D pos, int foodColor, int wallColor) {
        Pixel2D nearest = null;
        Map2D mapAllDistance = map.allDistance(pos, wallColor);
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < map.getWidth(); i++) {
            for (int j = 0; j < map.getHeight(); j++) {
                Pixel2D tmp = new Index2D(i, j);
                if (map.getPixel(tmp.getX(), tmp.getY()) == foodColor
                        && mapAllDistance.getPixel(tmp) != -1) {
                    int d = mapAllDistance.getPixel(tmp);
                    if (d < min) {
                        min = d;
                        nearest = tmp;
                    }
                }
            }
        }
        return nearest;
    }
}