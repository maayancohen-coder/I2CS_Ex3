package assignments;

import exe.ex3.game.Game;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class Ex3AlgoIsLegalTest {

    /* ============= Tests ============= */

    @Test
    @DisplayName("isLegal: regular empty cell -> true")
    void isLegal_regular_true() {
        int[][] b = emptyBoard(11, 9);
        Ex3Algo algo = new Ex3Algo();
        initColors(algo);

        Index2D p = new Index2D(1, 1);
        assertTrue(invokeIsLegal(algo, p, b));
    }

    @Test
    @DisplayName("isLegal: BLUE cell (wall) -> false")
    void isLegal_blue_false() {
        int[][] b = emptyBoard(11, 9);
        Ex3Algo algo = new Ex3Algo();
        initColors(algo);

        b[2][3] = blue();
        Index2D p = new Index2D(2, 3);
        assertFalse(invokeIsLegal(algo, p, b));
    }

    @Test
    @DisplayName("isLegal: inside ghost house and board[x][y]==0 -> false")
    void isLegal_ghostHouse_false() {
        int[][] b = emptyBoard(11, 9);
        Ex3Algo algo = new Ex3Algo();
        initColors(algo);

        int mx = b.length / 2;
        int my = b[0].length / 2;
        Index2D p = new Index2D(mx, my);
        b[mx][my] = 0;

        assertFalse(invokeIsLegal(algo, p, b));
    }

    @Test
    @DisplayName("isLegal: performance on large board (many calls) under realistic budget")
    void isLegal_perf() {
        int[][] b = emptyBoard(200, 200);
        Ex3Algo algo = new Ex3Algo();
        initColors(algo);

        assertTimeoutPreemptively(Duration.ofMillis(80), () -> {
            for (int x = 0; x < b.length; x++) {
                for (int y = 0; y < b[0].length; y++) {
                    invokeIsLegal(algo, new Index2D(x, y), b);
                }
            }
        });
    }

    /* ============= Helpers ============= */

    private static int[][] emptyBoard(int w, int h) {
        return new int[w][h];
    }

    private static int blue() {
        return Game.getIntColor(Color.BLUE, 0);
    }

    private static void initColors(Ex3Algo algo) {
        setIntField(algo, "BLUE", Game.getIntColor(Color.BLUE, 0));
        setIntField(algo, "PINK", Game.getIntColor(Color.PINK, 0));
        setIntField(algo, "GREEN", Game.getIntColor(Color.GREEN, 0));
    }

    private static void setIntField(Object o, String name, int v) {
        try {
            Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.setInt(o, v);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean invokeIsLegal(Ex3Algo algo, Object pixel2d, int[][] board) {
        try {
            Method m = Ex3Algo.class.getDeclaredMethod("isLegal", Class.forName("assignments.Pixel2D"), int[][].class);
            m.setAccessible(true);
            return (boolean) m.invoke(algo, pixel2d, board);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
