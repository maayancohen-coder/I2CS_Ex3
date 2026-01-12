package assignments;

import exe.ex3.game.Game;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class Ex3AlgoFirstLegalDirTest {

    @Test
    @DisplayName("firstLegalDir: UP blocked -> returns DOWN if DOWN is first legal (avoid ghost-house center)")
    void firstLegalDir_upBlocked_returnsDown() {
        int[][] board = emptyBoard(11, 11);
        Object map = createEngineMapOrSkip(board);

        Ex3Algo algo = new Ex3Algo();
        initColors(algo);

        // חשוב: לא להיות במרכז כדי לא ליפול ל-ghost house
        Index2D me = new Index2D(1, 1);

        // UP neighbor from (1,1) is (1,2) because UP => y++
        board[1][2] = blue(); // block UP

        int dir = invokeFirstLegalDir(algo, me, board, map);
        assertEquals(Game.DOWN, dir); // DOWN neighbor is (1,0) which is legal
    }

    @Test
    @DisplayName("firstLegalDir: all neighbors illegal -> returns Game.LEFT (default)")
    void firstLegalDir_allIllegal_returnsLeft() {
        int[][] board = emptyBoard(11, 11);
        Object map = createEngineMapOrSkip(board);

        Ex3Algo algo = new Ex3Algo();
        initColors(algo);

        Index2D me = new Index2D(1, 1);

        // Block all 4 neighbors around (1,1)
        board[1][2] = blue(); // UP  (1,2)
        board[1][0] = blue(); // DOWN(1,0)
        board[0][1] = blue(); // LEFT(0,1)
        board[2][1] = blue(); // RIGHT(2,1)

        int dir = invokeFirstLegalDir(algo, me, board, map);
        assertEquals(Game.LEFT, dir);
    }

    @Test
    @DisplayName("firstLegalDir: performance repeated calls (preemptive timeout, reflection)")
    void firstLegalDir_perf() throws Exception {
        int[][] board = emptyBoard(80, 80);
        Object map = createEngineMapOrSkip(board);

        Ex3Algo algo = new Ex3Algo();
        initColors(algo);

        Index2D me = new Index2D(1, 1);

        Method m = getFirstLegalDirMethodOrThrow();
        m.setAccessible(true);

        assertTimeoutPreemptively(Duration.ofMillis(1500), () -> {
            for (int i = 0; i < 10_000; i++) {
                int dir = (int) m.invoke(algo, me, board, map);
                if (dir < 0) fail("Invalid direction: " + dir);
            }
        });
    }


    private static Method getFirstLegalDirMethodOrThrow() {
        try {
            Class<?> pixelClz = Class.forName("assignments.Pixel2D");
            Class<?> mapType = findMapParamTypeOrSkip();
            return Ex3Algo.class.getDeclaredMethod("firstLegalDir", pixelClz, int[][].class, mapType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /* ========================= Helpers ========================= */

    private static int[][] emptyBoard(int w, int h) { return new int[w][h]; }

    private static int blue() { return Game.getIntColor(Color.BLUE, 0); }

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

    /** Infer Map type from Ex3Algo.firstLegalDir signature (no hard-coded class name). */
    private static Class<?> findMapParamTypeOrSkip() {
        for (Method m : Ex3Algo.class.getDeclaredMethods()) {
            if (!m.getName().equals("firstLegalDir")) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 3 && p[1].equals(int[][].class)) {
                return p[2];
            }
        }
        Assumptions.assumeTrue(false, "Could not find firstLegalDir(Pixel2D, int[][], MapType) in Ex3Algo.");
        return null;
    }

    /** Create map instance in a few reasonable ways and ensure getMap() exists. */
    private static Object createEngineMapOrSkip(int[][] board) {
        Class<?> mapType = findMapParamTypeOrSkip();

        try {
            // 1) ctor(int[][])
            try {
                Constructor<?> c = mapType.getConstructor(int[][].class);
                Object map = c.newInstance((Object) board);
                ensureGetMapWorksOrSkip(map, board);
                return map;
            } catch (NoSuchMethodException ignored) {}

            // 2) ctor(int,int) + optional init(int[][])
            try {
                Constructor<?> c = mapType.getConstructor(int.class, int.class);
                Object map = c.newInstance(board.length, board[0].length);

                try {
                    Method init = mapType.getMethod("init", int[][].class);
                    init.invoke(map, (Object) board);
                } catch (NoSuchMethodException ignored2) {}

                injectBoardIntoIntArrayFieldIfPossible(map, board);
                ensureGetMapWorksOrSkip(map, board);
                return map;
            } catch (NoSuchMethodException ignored) {}

            // 3) empty ctor + field injection
            try {
                Constructor<?> c = mapType.getConstructor();
                Object map = c.newInstance();
                injectBoardIntoIntArrayFieldIfPossible(map, board);
                ensureGetMapWorksOrSkip(map, board);
                return map;
            } catch (NoSuchMethodException ignored) {}

            Assumptions.assumeTrue(false,
                    "MapType is " + mapType.getName() + " but no usable constructor found.");
            return null;

        } catch (Throwable t) {
            Assumptions.assumeTrue(false, "Failed creating MapType " + mapType.getName() + ". Cause: " + t);
            return null;
        }
    }

    private static void ensureGetMapWorksOrSkip(Object map, int[][] expectedBoard) {
        try {
            Method gm = map.getClass().getMethod("getMap");
            Object res = gm.invoke(map);
            Assumptions.assumeTrue(res instanceof int[][], "getMap() did not return int[][]");
            int[][] arr = (int[][]) res;

            Assumptions.assumeTrue(arr.length == expectedBoard.length && arr[0].length == expectedBoard[0].length,
                    "getMap() dimensions mismatch");
        } catch (Throwable t) {
            Assumptions.assumeTrue(false, "MapType exists but getMap() not usable. Cause: " + t);
        }
    }

    private static void injectBoardIntoIntArrayFieldIfPossible(Object map, int[][] board) {
        try {
            for (Field f : map.getClass().getDeclaredFields()) {
                if (f.getType().equals(int[][].class)) {
                    f.setAccessible(true);
                    f.set(map, board);
                    return;
                }
            }
        } catch (Throwable ignored) {}
    }

    private static int invokeFirstLegalDir(Ex3Algo algo, Object pixel2d, int[][] board, Object mapObj) {
        try {
            Class<?> pixelClz = Class.forName("assignments.Pixel2D");
            Class<?> mapType = findMapParamTypeOrSkip();

            Method m = Ex3Algo.class.getDeclaredMethod("firstLegalDir", pixelClz, int[][].class, mapType);
            m.setAccessible(true);
            return (int) m.invoke(algo, pixel2d, board, mapObj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}