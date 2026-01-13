package assignments;

import exe.ex3.game.Game;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacmanGame;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

public class Ex3AlgoMoveTest {

    @Test
    @DisplayName("move: returns only UP/DOWN/LEFT/RIGHT (never STAY or other numbers)")
    void move_returnsOnlyFourDirs() {
        assumeProxies();

        int[][] board = openBoard(9, 9);
        board[4][4] = PINK();

        PacmanGame game = gameProxy(board, "1,1", new GhostCL[0]);

        Ex3Algo algo = new Ex3Algo();
        int dir = algo.move(game);

        assertTrue(isDir(dir), "move() must return one of UP/DOWN/LEFT/RIGHT");
    }

    @Test
    @DisplayName("move: never chooses an illegal tile (BLUE wall or ghost house)")
    void move_neverIllegalTile() {
        assumeProxies();

        int[][] board = openBoard(9, 9);
        int blue = BLUE();

        // Build a small "ghost house" center area by putting zeros in the center (your check uses board==0)
        // Your isGhostHouse requires board[x][y]==0 and within <3 of center.
        // On openBoard everything is 0, so the center region is a ghost house!
        // We'll force pac near center and allow ONLY one exit that is NOT inside that region by placing walls.
        int mx = board.length / 2;     // 4
        int my = board[0].length / 2;  // 4

        // Put pac just outside ghost house border, but with options that would step into ghost house if bugged.
        // Place pac at (mx-3, my) -> one step right enters |x-mx|<3 area.
        String pacPos = (mx - 3) + "," + my;

        // Make a pink target somewhere
        board[0][0] = PINK();

        // Block all directions except LEFT so the only legal move is LEFT (which stays outside ghost house zone).
        // Need to consider wrap neighbors too; we will not place pac at edge here, so no wrap issues.
        // Neighbors of (mx-3, my):
        // RIGHT enters ghost house (should be illegal)
        // UP/DOWN could also enter ghost house depending on y. We block them with walls.
        // LEFT is safe and legal.
        board[mx - 2][my] = blue;      // block RIGHT (also prevents stepping into ghost house)
        board[mx - 3][my + 1] = blue;  // block UP
        board[mx - 3][my - 1] = blue;  // block DOWN

        PacmanGame game = gameProxy(board, pacPos, new GhostCL[0]);

        Ex3Algo algo = new Ex3Algo();
        int dir = algo.move(game);

        assertEquals(Game.LEFT, dir, "Only LEFT is legal and outside ghost house/walls");
    }

    @Test
    @DisplayName("move: if a dangerous ghost is adjacent, never steps into certain death (danger <= 1)")
    void move_avoidsCertainDeathAdjacentGhost() {
        assumeProxies();

        int[][] board = openBoard(9, 9);
        board[7][7] = PINK(); // target somewhere

        // Pac at (1,1). Put a dangerous ghost at (2,1) (RIGHT neighbor).
        GhostCL dangerGhost = ghostProxy(
                1,        // status active
                -1.0,     // not eatable => dangerous
                "2,1"
        );

        PacmanGame game = gameProxy(board, "1,1", new GhostCL[]{dangerGhost});

        Ex3Algo algo = new Ex3Algo();
        int dir = algo.move(game);

        // The move RIGHT would step onto the ghost cell which must be avoided by hard filter (danger<=1).
        assertNotEquals(Game.RIGHT, dir, "Must not step onto a dangerous ghost cell");
        assertTrue(isDir(dir));
    }

    @Test
    @DisplayName("move: unique safe move due to walls + dangerous ghost + wrap neighbors -> must pick it")
    void move_uniqueSafeMove_withWrapAndGhost() {
        assumeProxies();

        int[][] board = openBoard(7, 7);
        int blue = BLUE();

        // Place pac at (0,0) so wrap-around neighbors exist:
        // UP -> (0,1)
        // DOWN -> (0,6) wrap
        // LEFT -> (6,0) wrap
        // RIGHT -> (1,0)
        //
        // We will make only UP safe+legal.
        //
        // Block LEFT and DOWN with walls:
        board[6][0] = blue; // LEFT wrap
        board[0][6] = blue; // DOWN wrap

        // Make RIGHT "certain death" by placing a dangerous ghost there:
        GhostCL dangerGhost = ghostProxy(1, -1.0, "1,0");

        // UP is the only safe legal move
        board[3][3] = PINK();

        PacmanGame game = gameProxy(board, "0,0", new GhostCL[]{dangerGhost});

        Ex3Algo algo = new Ex3Algo();
        int dir = algo.move(game);

        assertEquals(Game.UP, dir, "Only UP is safe+legal (others are wall/wrap-wall/death)");
    }

    @Test
    @DisplayName("move: on a tiny board it still returns a valid direction")
    void move_tinyBoard_validDir() {
        assumeProxies();

        int[][] board = openBoard(3, 3);
        board[0][0] = PINK();

        PacmanGame game = gameProxy(board, "2,2", new GhostCL[0]);

        Ex3Algo algo = new Ex3Algo();
        int dir = algo.move(game);

        assertTrue(isDir(dir));
    }

    /* ===================== helpers ===================== */

    private static void assumeProxies() {
        Assumptions.assumeTrue(PacmanGame.class.isInterface(), "PacmanGame not interface here; skipping proxy-based test.");
        Assumptions.assumeTrue(GhostCL.class.isInterface(), "GhostCL not interface here; skipping proxy-based test.");
    }

    private static boolean isDir(int d) {
        return d == Game.UP || d == Game.DOWN || d == Game.LEFT || d == Game.RIGHT;
    }

    private static int[][] openBoard(int w, int h) {
        return new int[w][h]; // 0 = empty floor in your server boards
    }

    private static int BLUE()  { return Game.getIntColor(Color.BLUE, 0); }
    private static int PINK()  { return Game.getIntColor(Color.PINK, 0); }
    // private static int GREEN() { return Game.getIntColor(Color.GREEN, 0); } // unused here

    private static PacmanGame gameProxy(int[][] board, String pacPos, GhostCL[] ghosts) {
        return (PacmanGame) Proxy.newProxyInstance(
                PacmanGame.class.getClassLoader(),
                new Class[]{PacmanGame.class},
                (proxy, method, args) -> {
                    String name = method.getName();

                    if (name.equals("getGame")) return board;
                    if (name.equals("getPos")) return pacPos;
                    if (name.equals("getGhosts")) return ghosts;

                    // Not used by your algorithm in move()
                    Class<?> rt = method.getReturnType();
                    if (rt.equals(boolean.class)) return false;
                    if (rt.equals(int.class)) return 0;
                    if (rt.equals(double.class)) return 0.0;
                    return null;
                }
        );
    }

    private static GhostCL ghostProxy(int status, double eatableTime, String pos) {
        return (GhostCL) Proxy.newProxyInstance(
                GhostCL.class.getClassLoader(),
                new Class[]{GhostCL.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("getStatus")) return status;
                    if (name.equals("remainTimeAsEatable")) return eatableTime;
                    if (name.equals("getPos")) return pos;

                    Class<?> rt = method.getReturnType();
                    if (rt.equals(boolean.class)) return false;
                    if (rt.equals(int.class)) return 0;
                    if (rt.equals(double.class)) return 0.0;
                    return null;
                }
        );
    }
}
