package assignments;

import MyGame.adapters.MyPacmanGameAdapter;
import MyGame.server.MyGameServer;
import exe.ex3.game.Game;
import exe.ex3.game.PacManAlgo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AutoIntegrationTest {

    @Test
    @DisplayName("AUTO integration: server+adapter+algo run several steps without crashing and directions are valid")
    void auto_integration_runsSteps() {
        MyGameServer server = new MyGameServer();
        server.initDefaultLevel();

        MyPacmanGameAdapter adapter = new MyPacmanGameAdapter(server);
        PacManAlgo algo = new Ex3Algo();

        // run a bunch of steps (stop early if game ends)
        int steps = 250;

        for (int i = 0; i < steps; i++) {
            if (server.getStatus() != MyGameServer.PLAY) break;

            int dir = algo.move(adapter);

            // direction must be one of the 4
            assertTrue(isDir(dir), "Algo must return UP/DOWN/LEFT/RIGHT, got: " + dir);

            // adapter.move() should not crash and should return snapshot
            String snap = adapter.move(dir);
            assertNotNull(snap);
            assertTrue(snap.contains("Score=") && snap.contains("PinkLeft="),
                    "Snapshot must contain Score= and PinkLeft=");

            // pos format remains "x,y" and in bounds
            String pos = adapter.getPos(0);
            int[] xy = parseXY(pos);

            int[][] board = adapter.getGame(0);
            assertNotNull(board);
            assertTrue(board.length > 0 && board[0].length > 0);

            assertTrue(0 <= xy[0] && xy[0] < board.length, "PacX out of bounds: " + xy[0]);
            assertTrue(0 <= xy[1] && xy[1] < board[0].length, "PacY out of bounds: " + xy[1]);
        }
    }

    private static boolean isDir(int d) {
        return d == Game.UP || d == Game.DOWN || d == Game.LEFT || d == Game.RIGHT;
    }

    private static int[] parseXY(String s) {
        assertNotNull(s, "Position string is null");
        String[] parts = s.split(",");
        assertEquals(2, parts.length, "Position must be in 'x,y' format: " + s);

        int x = Integer.parseInt(parts[0].trim());
        int y = Integer.parseInt(parts[1].trim());
        return new int[]{x, y};
    }
}
