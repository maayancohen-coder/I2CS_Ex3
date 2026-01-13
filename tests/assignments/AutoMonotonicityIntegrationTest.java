package assignments;

import MyGame.adapters.MyPacmanGameAdapter;
import MyGame.server.MyGameServer;
import exe.ex3.game.PacManAlgo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AutoMonotonicityIntegrationTest {

    @Test
    @DisplayName("AUTO integration: score never decreases, pinkLeft never increases, and pink totals stay consistent")
    void auto_monotonicity_and_invariants() {
        MyGameServer server = new MyGameServer();
        server.initDefaultLevel();

        MyPacmanGameAdapter adapter = new MyPacmanGameAdapter(server);
        PacManAlgo algo = new Ex3Algo();

        int prevScore = server.getScore();
        int prevPinkLeft = server.getPinkLeft();

        int total = server.getPinkTotal();
        assertTrue(total >= 0, "PinkTotal must be non-negative");

        int steps = 400;

        for (int i = 0; i < steps; i++) {
            if (server.getStatus() != MyGameServer.PLAY) break;

            int dir = algo.move(adapter);
            adapter.move(dir); // includes tick

            int score = server.getScore();
            int left = server.getPinkLeft();
            int eaten = server.getPinkEaten();

            assertTrue(score >= prevScore, "Score decreased at step " + i + ": " + prevScore + " -> " + score);
            assertTrue(left <= prevPinkLeft, "PinkLeft increased at step " + i + ": " + prevPinkLeft + " -> " + left);

            // Strong invariant: eaten + left == total (if your server tracks these consistently)
            assertEquals(total, eaten + left,
                    "Invariant broken at step " + i + ": eaten(" + eaten + ") + left(" + left + ") != total(" + total + ")");

            prevScore = score;
            prevPinkLeft = left;
        }
    }
}
