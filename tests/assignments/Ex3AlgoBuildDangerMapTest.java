package assignments;

import exe.ex3.game.GhostCL;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import exe.ex3.game.Game;

import java.awt.Color;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

public class Ex3AlgoBuildDangerMapTest {

    @Test
    @DisplayName("buildDangerMap: no ghosts -> all cells Infinity")
    void buildDangerMap_noGhosts_allInfinity() {
        Ex3Algo algo = new Ex3Algo();
        initBlue(algo);

        int[][] board = emptyBoard(5, 4);
        Map map = new Map(board);

        GhostCL[] ghosts = new GhostCL[0];

        double[][] danger = invokeBuildDangerMap(algo, map, board, ghosts);

        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[0].length; y++) {
                assertTrue(Double.isInfinite(danger[x][y]), "Expected Infinity when there are no dangerous ghosts");
            }
        }
    }

    @Test
    @DisplayName("buildDangerMap: ignores inactive ghosts (status==0) -> all Infinity")
    void buildDangerMap_inactiveGhosts_ignored() {
        Assumptions.assumeTrue(GhostCL.class.isInterface(), "GhostCL not interface here; skipping proxy-based test.");

        Ex3Algo algo = new Ex3Algo();
        initBlue(algo);

        int[][] board = emptyBoard(6, 6);
        Map map = new Map(board);

        GhostCL g1 = ghostProxy(0, -1.0, "2,2"); // inactive
        GhostCL g2 = ghostProxy(0,  5.0, "4,4"); // inactive
        double[][] danger = invokeBuildDangerMap(algo, map, board, new GhostCL[]{g1, g2});

        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[0].length; y++) {
                assertTrue(Double.isInfinite(danger[x][y]), "Inactive ghosts must not contribute to danger map");
            }
        }
    }

    @Test
    @DisplayName("buildDangerMap: one dangerous ghost -> ghost cell danger is 0, neighbors finite")
    void buildDangerMap_dangerousGhost_basicDistances() {
        Assumptions.assumeTrue(GhostCL.class.isInterface(), "GhostCL not interface here; skipping proxy-based test.");

        Ex3Algo algo = new Ex3Algo();
        initBlue(algo);

        int[][] board = emptyBoard(7, 7);
        Map map = new Map(board);
        map.setCyclic(true);

        GhostCL g = ghostProxy(1, -1.0, "3,3"); // dangerous (not eatable)
        double[][] danger = invokeBuildDangerMap(algo, map, board, new GhostCL[]{g});

        assertEquals(0.0, danger[3][3], 1e-9, "Ghost cell should have distance 0");
        assertTrue(danger[3][4] >= 1.0 && danger[3][4] < Double.POSITIVE_INFINITY, "Neighbor should be finite");
        assertTrue(danger[0][0] < Double.POSITIVE_INFINITY, "Far cells should still be finite on open board");
    }

    @Test
    @DisplayName("buildDangerMap: non-dangerous (eatable time > 2) is ignored")
    void buildDangerMap_eatableLong_ignored() {
        Assumptions.assumeTrue(GhostCL.class.isInterface(), "GhostCL not interface here; skipping proxy-based test.");

        Ex3Algo algo = new Ex3Algo();
        initBlue(algo);

        int[][] board = emptyBoard(7, 7);
        Map map = new Map(board);

        GhostCL g = ghostProxy(1, 10.0, "3,3"); // NOT dangerous (t > 2)
        double[][] danger = invokeBuildDangerMap(algo, map, board, new GhostCL[]{g});

        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[0].length; y++) {
                assertTrue(Double.isInfinite(danger[x][y]), "Eatable long-time ghost should be ignored");
            }
        }
    }

    @Test
    @DisplayName("buildDangerMap: dangerous definition includes t<=2 (conservative)")
    void buildDangerMap_tinyEatable_consideredDangerous() {
        Assumptions.assumeTrue(GhostCL.class.isInterface(), "GhostCL not interface here; skipping proxy-based test.");

        Ex3Algo algo = new Ex3Algo();
        initBlue(algo);

        int[][] board = emptyBoard(7, 7);
        Map map = new Map(board);

        GhostCL g = ghostProxy(1, 2.0, "3,3"); // considered dangerous by code (t <= 2)
        double[][] danger = invokeBuildDangerMap(algo, map, board, new GhostCL[]{g});

        assertEquals(0.0, danger[3][3], 1e-9);
        assertTrue(danger[2][3] < Double.POSITIVE_INFINITY);
    }

    /* ================= helpers ================= */

    private static int[][] emptyBoard(int w, int h) {
        return new int[w][h];
    }

    private static double[][] invokeBuildDangerMap(Ex3Algo algo, Map map, int[][] board, GhostCL[] ghosts) {
        try {
            Method m = Ex3Algo.class.getDeclaredMethod("buildDangerMap", Map.class, int[][].class, GhostCL[].class);
            m.setAccessible(true);
            return (double[][]) m.invoke(algo, map, board, (Object) ghosts);
        } catch (Exception e) {
            throw new RuntimeException("Reflection call to buildDangerMap failed: " + e, e);
        }
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

    private static void initBlue(Ex3Algo algo) {
        setIntField(algo, "BLUE", Game.getIntColor(Color.BLUE, 0));
    }

    private static void setIntField(Object o, String name, int v) {
        try {
            java.lang.reflect.Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.setInt(o, v);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
