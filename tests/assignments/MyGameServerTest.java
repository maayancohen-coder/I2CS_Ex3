package assignments;

import MyGame.server.MyGameServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MyGameServerTest {

    @Test
    @DisplayName("initDefaultLevel: creates a non-null board with positive dimensions")
    void initDefaultLevel_boardExists() {
        MyGameServer s = new MyGameServer();
        s.initDefaultLevel();

        int[][] b = s.getBoard();
        assertNotNull(b);
        assertTrue(b.length > 0);
        assertNotNull(b[0]);
        assertTrue(b[0].length > 0);
    }

    @Test
    @DisplayName("initDefaultLevel: pac position is inside board bounds")
    void initDefaultLevel_pacInBounds() {
        MyGameServer s = new MyGameServer();
        s.initDefaultLevel();

        int[][] b = s.getBoard();
        int w = b.length;
        int h = b[0].length;

        int x = s.getPacX();
        int y = s.getPacY();

        assertTrue(0 <= x && x < w, "PacX must be within [0, w)");
        assertTrue(0 <= y && y < h, "PacY must be within [0, h)");
    }

    @Test
    @DisplayName("score: never decreases across ticks and moves")
    void score_neverDecreases() {
        MyGameServer s = new MyGameServer();
        s.initDefaultLevel();

        int prev = s.getScore();

        // Mix moves and ticks (should never reduce score)
        for (int i = 0; i < 200; i++) {
            s.movePacByDir(MyGameServer.LEFT);
            s.tick();
            int now = s.getScore();
            assertTrue(now >= prev, "Score must not decrease");
            prev = now;
        }
    }

    @Test
    @DisplayName("pinkLeft: never increases across gameplay steps")
    void pinkLeft_neverIncreases() {
        MyGameServer s = new MyGameServer();
        s.initDefaultLevel();

        int prev = s.getPinkLeft();

        for (int i = 0; i < 300; i++) {
            // try to move in different directions
            int dir = (i % 4 == 0) ? MyGameServer.UP :
                    (i % 4 == 1) ? MyGameServer.RIGHT :
                            (i % 4 == 2) ? MyGameServer.DOWN :
                                    MyGameServer.LEFT;

            s.movePacByDir(dir);
            s.tick();

            int now = s.getPinkLeft();
            assertTrue(now <= prev, "PinkLeft must not increase");
            prev = now;

            // If game ends early, stop the loop
            if (s.getStatus() == MyGameServer.DONE) break;
        }
    }

    @Test
    @DisplayName("tick: does not move pac outside bounds")
    void tick_pacStaysInBounds() {
        MyGameServer s = new MyGameServer();
        s.initDefaultLevel();

        int[][] b = s.getBoard();
        int w = b.length;
        int h = b[0].length;

        for (int i = 0; i < 300; i++) {
            s.tick();
            int x = s.getPacX();
            int y = s.getPacY();
            assertTrue(0 <= x && x < w);
            assertTrue(0 <= y && y < h);

            if (s.getStatus() == MyGameServer.DONE) break;
        }
    }

    @Test
    @DisplayName("toggleCyclic: flips the cyclic flag each call")
    void toggleCyclic_flipsFlag() {
        MyGameServer s = new MyGameServer();
        s.initDefaultLevel();

        boolean a = s.isCyclic();
        s.toggleCyclic();
        boolean b = s.isCyclic();
        s.toggleCyclic();
        boolean c = s.isCyclic();

        assertNotEquals(a, b, "toggleCyclic must flip state");
        assertEquals(a, c, "toggling twice must return to original state");
    }

    @Test
    @DisplayName("quit: ends the game (status becomes DONE)")
    void quit_setsDone() {
        MyGameServer s = new MyGameServer();
        s.initDefaultLevel();

        s.quit();
        assertEquals(MyGameServer.DONE, s.getStatus(), "After quit(), status must be DONE");
    }

    @Test
    @DisplayName("ghosts: getGhosts returns non-null array or null-safe behavior; positions are within bounds when released")
    void ghosts_basicSanity() {
        MyGameServer s = new MyGameServer();
        s.initDefaultLevel();

        int[][] b = s.getBoard();
        int w = b.length;
        int h = b[0].length;

        MyGameServer.Ghost[] gs = s.getGhosts();
        if (gs == null) {
            return; // server may legitimately return null before first tick
        }

        for (MyGameServer.Ghost g : gs) {
            assertNotNull(g);

            // Only enforce bounds when ghost is released (as your UI uses that condition).
            if (g.released) {
                assertTrue(0 <= g.x && g.x < w, "Ghost x must be within bounds");
                assertTrue(0 <= g.y && g.y < h, "Ghost y must be within bounds");
            }
        }
    }

    @Test
    @DisplayName("movePacByDir: does not throw for any direction constant")
    void movePacByDir_noThrow_allDirs() {
        MyGameServer s = new MyGameServer();
        s.initDefaultLevel();

        assertDoesNotThrow(() -> s.movePacByDir(MyGameServer.UP));
        assertDoesNotThrow(() -> s.movePacByDir(MyGameServer.DOWN));
        assertDoesNotThrow(() -> s.movePacByDir(MyGameServer.LEFT));
        assertDoesNotThrow(() -> s.movePacByDir(MyGameServer.RIGHT));
        assertDoesNotThrow(() -> s.movePacByDir(MyGameServer.STAY));
    }
}
