package assignments;

import MyGame.server.MyGameServer;
import exe.ex3.game.Game;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class MyGameServerInvariantsTest {

    @Test
    @DisplayName("Board: rectangular structure (all columns have same height)")
    void board_isRectangular() {
        MyGameServer s = new MyGameServer();
        s.initDefaultLevel();

        int[][] b = s.getBoard();
        assertNotNull(b);
        assertTrue(b.length > 0);
        int h = b[0].length;
        for (int x = 0; x < b.length; x++) {
            assertNotNull(b[x], "Column " + x + " is null");
            assertEquals(h, b[x].length, "All columns must have same height");
        }
    }

    @Test
    @DisplayName("movePacByDir(STAY): does not change pac position")
    void stay_doesNotMove() {
        MyGameServer s = new MyGameServer();
        s.initDefaultLevel();

        int x0 = s.getPacX();
        int y0 = s.getPacY();

        s.movePacByDir(MyGameServer.STAY);

        assertEquals(x0, s.getPacX());
        assertEquals(y0, s.getPacY());
    }

    @Test
    @DisplayName("Move into wall: pac position does not change (forced setup, no ignores)")
    void moveIntoWall_doesNotMove_forcedSetup() throws Exception {
        MyGameServer s = new MyGameServer();
        s.initDefaultLevel();

        int[][] b = s.getBoard();
        int w = b.length, h = b[0].length;

        int BLUE = exe.ex3.game.Game.getIntColor(java.awt.Color.BLUE, 0);

        // Find any non-wall cell that has an adjacent wall.
        int targetX = -1, targetY = -1, dirToWall = MyGameServer.STAY;

        for (int x = 0; x < w && targetX == -1; x++) {
            for (int y = 0; y < h && targetX == -1; y++) {
                if (b[x][y] == BLUE) continue; // cannot stand inside wall

                // check neighbors for wall
                if (y + 1 < h && b[x][y + 1] == BLUE) { targetX = x; targetY = y; dirToWall = MyGameServer.UP; }
                else if (y - 1 >= 0 && b[x][y - 1] == BLUE) { targetX = x; targetY = y; dirToWall = MyGameServer.DOWN; }
                else if (x - 1 >= 0 && b[x - 1][y] == BLUE) { targetX = x; targetY = y; dirToWall = MyGameServer.LEFT; }
                else if (x + 1 < w && b[x + 1][y] == BLUE) { targetX = x; targetY = y; dirToWall = MyGameServer.RIGHT; }
            }
        }

        assertTrue(targetX != -1, "Could not find a cell adjacent to a wall on this board");

        // Force pacman position for deterministic test (reflection, no production code change)
        setIntField(s, "pacX", targetX);
        setIntField(s, "pacY", targetY);

        int x0 = s.getPacX();
        int y0 = s.getPacY();

        s.movePacByDir(dirToWall);

        assertEquals(x0, s.getPacX(), "Pac should not move into a wall");
        assertEquals(y0, s.getPacY(), "Pac should not move into a wall");
    }

    private static void setIntField(Object obj, String fieldName, int value) throws Exception {
        java.lang.reflect.Field f;
        Class<?> c = obj.getClass();
        while (true) {
            try {
                f = c.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
                if (c == null) throw e;
            }
        }
        f.setAccessible(true);
        f.setInt(obj, value);
    }

}
