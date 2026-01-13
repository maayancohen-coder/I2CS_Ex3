package assignments;

import MyGame.adapters.MyPacmanGameAdapter;
import MyGame.server.MyGameServer;
import exe.ex3.game.PacManAlgo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AdapterShapeStabilityIntegrationTest {

    @Test
    @DisplayName("Integration: board dimensions stay stable and pos format remains valid across AUTO steps")
    void boardDims_andPosFormat_stable_inAuto() {
        MyGameServer server = new MyGameServer();
        server.initDefaultLevel();

        MyPacmanGameAdapter adapter = new MyPacmanGameAdapter(server);
        PacManAlgo algo = new Ex3Algo();

        int[][] b0 = adapter.getGame(0);
        int w0 = b0.length, h0 = b0[0].length;

        for (int i = 0; i < 300; i++) {
            if (server.getStatus() != MyGameServer.PLAY) break;

            int dir = algo.move(adapter);
            adapter.move(dir);

            int[][] b = adapter.getGame(0);
            assertEquals(w0, b.length);
            assertEquals(h0, b[0].length);

            String pos = adapter.getPos(0);
            String[] parts = pos.split(",");
            assertEquals(2, parts.length);
            Integer.parseInt(parts[0].trim());
            Integer.parseInt(parts[1].trim());
        }
    }
}
