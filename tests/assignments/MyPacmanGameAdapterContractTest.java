package assignments;

import MyGame.adapters.MyPacmanGameAdapter;
import MyGame.server.MyGameServer;
import exe.ex3.game.Game;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MyPacmanGameAdapterContractTest {

    @Test
    @DisplayName("Contract: getGame not null, stable dimensions; getPos in-bounds and format; getGhosts never null")
    void contract_stability_over_many_steps() {
        MyGameServer s = new MyGameServer();
        s.initDefaultLevel();
        MyPacmanGameAdapter a = new MyPacmanGameAdapter(s);

        int[][] b0 = a.getGame(0);
        assertNotNull(b0);
        assertTrue(b0.length > 0 && b0[0].length > 0);
        int w0 = b0.length;
        int h0 = b0[0].length;

        // Run many steps via adapter.move
        for (int i = 0; i < 300; i++) {
            if (s.getStatus() != MyGameServer.PLAY) break;

            int dir = (i % 4 == 0) ? Game.UP :
                    (i % 4 == 1) ? Game.RIGHT :
                            (i % 4 == 2) ? Game.DOWN :
                                    Game.LEFT;

            assertDoesNotThrow(() -> a.move(dir));

            int[][] b = a.getGame(0);
            assertNotNull(b, "getGame must never return null");
            assertEquals(w0, b.length, "Board width must remain stable");
            assertEquals(h0, b[0].length, "Board height must remain stable");

            String pos = a.getPos(0);
            int[] xy = parseXY(pos);

            assertTrue(0 <= xy[0] && xy[0] < b.length, "PacX out of bounds: " + xy[0]);
            assertTrue(0 <= xy[1] && xy[1] < b[0].length, "PacY out of bounds: " + xy[1]);

            assertNotNull(a.getGhosts(0), "getGhosts must never return null");
        }
    }

    @Test
    @DisplayName("init: cyclic flag in adapter matches requested cyclic argument")
    void init_respectsCyclic() {
        MyGameServer s = new MyGameServer();
        MyPacmanGameAdapter a = new MyPacmanGameAdapter(s);

        a.init(0, "", true, 0L, 1.0, 0, 0);
        assertTrue(a.isCyclic(), "After init(cyclic=true), adapter should be cyclic");

        a.init(0, "", false, 0L, 1.0, 0, 0);
        assertFalse(a.isCyclic(), "After init(cyclic=false), adapter should be non-cyclic");
    }

    private static int[] parseXY(String s) {
        assertNotNull(s, "pos string is null");
        String[] parts = s.split(",");
        assertEquals(2, parts.length, "pos must be 'x,y' but got: " + s);
        int x = Integer.parseInt(parts[0].trim());
        int y = Integer.parseInt(parts[1].trim());
        return new int[]{x, y};
    }
}
