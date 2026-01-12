package assignments;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Ex3Algo.isGhostHouse(Pixel2D,int[][]).
 *
 * Notes:
 * - In this project, Pixel2D is defined under the 'assignments' package (not in the engine jar).
 * - isGhostHouse is private, so we call it via reflection.
 * - Tests cover: inside center area, outside center area, boundary behavior, and board value constraint.
 */
public class Ex3AlgoGhostHouseTest {

    @Test
    @DisplayName("isGhostHouse: inside center area AND board[x][y] == 0 -> true")
    void ghostHouse_insideAndZero_true() {
        int[][] b = emptyBoard(11, 9);
        int mx = b.length / 2;      // 5
        int my = b[0].length / 2;   // 4

        Pixel2D p = new Index2D(mx, my);
        b[p.getX()][p.getY()] = 0;

        assertTrue(invokeIsGhostHouse(p, b));
    }

    @Test
    @DisplayName("isGhostHouse: inside center area BUT board[x][y] != 0 -> false")
    void ghostHouse_insideButNonZero_false() {
        int[][] b = emptyBoard(11, 9);
        int mx = b.length / 2;
        int my = b[0].length / 2;

        Pixel2D p = new Index2D(mx + 1, my - 1); // still inside abs<3
        b[p.getX()][p.getY()] = 7;               // not 0

        assertFalse(invokeIsGhostHouse(p, b));
    }

    @Test
    @DisplayName("isGhostHouse: outside center area even if board[x][y] == 0 -> false")
    void ghostHouse_outside_false() {
        int[][] b = emptyBoard(11, 9);
        int mx = b.length / 2;
        int my = b[0].length / 2;

        Pixel2D p = new Index2D(mx + 4, my); // abs=4 => outside
        b[p.getX()][p.getY()] = 0;

        assertFalse(invokeIsGhostHouse(p, b));
    }

    @Test
    @DisplayName("isGhostHouse: boundary abs==3 is NOT included (strict < 3) -> false")
    void ghostHouse_boundaryAbs3_false() {
        int[][] b = emptyBoard(11, 9);
        int mx = b.length / 2;
        int my = b[0].length / 2;

        Pixel2D p1 = new Index2D(mx + 3, my);   // abs(x-mx)=3
        Pixel2D p2 = new Index2D(mx, my - 3);   // abs(y-my)=3
        b[p1.getX()][p1.getY()] = 0;
        b[p2.getX()][p2.getY()] = 0;

        assertFalse(invokeIsGhostHouse(p1, b));
        assertFalse(invokeIsGhostHouse(p2, b));
    }

    /* ---------------- helpers ---------------- */

    /** Creates an empty board (default 0 values). */
    private int[][] emptyBoard(int w, int h) {
        return new int[w][h];
    }

    /**
     * Invokes private boolean isGhostHouse(Pixel2D,int[][]) via reflection.
     * This keeps the production method private while still allowing unit coverage.
     */
    private boolean invokeIsGhostHouse(Pixel2D p, int[][] board) {
        try {
            Ex3Algo algo = new Ex3Algo();
            Method m = Ex3Algo.class.getDeclaredMethod("isGhostHouse", Pixel2D.class, int[][].class);
            m.setAccessible(true);
            return (boolean) m.invoke(algo, p, board);
        } catch (Exception e) {
            fail("Reflection call to isGhostHouse failed: " + e);
            return false; // unreachable
        }
    }
}
