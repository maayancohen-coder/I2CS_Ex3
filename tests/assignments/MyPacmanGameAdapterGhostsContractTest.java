package assignments;

import MyGame.adapters.MyPacmanGameAdapter;
import MyGame.server.MyGameServer;
import exe.ex3.game.GhostCL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MyPacmanGameAdapterGhostsContractTest {

    @Test
    @DisplayName("getGhosts: never returns null and all entries are non-null GhostCL")
    void getGhosts_neverNull_entriesNonNull() {
        MyGameServer server = new MyGameServer();
        server.initDefaultLevel();

        MyPacmanGameAdapter adapter = new MyPacmanGameAdapter(server);

        GhostCL[] ghosts = adapter.getGhosts(0);

        assertNotNull(ghosts, "getGhosts must not return null");

        for (GhostCL g : ghosts) {
            assertNotNull(g, "GhostCL entry must not be null");
        }
    }

    @Test
    @DisplayName("GhostCL: getPos format is x,y and parseable")
    void ghost_getPos_format() {
        MyGameServer server = new MyGameServer();
        server.initDefaultLevel();

        MyPacmanGameAdapter adapter = new MyPacmanGameAdapter(server);
        GhostCL[] ghosts = adapter.getGhosts(0);

        for (GhostCL g : ghosts) {
            String pos = g.getPos(0);
            assertNotNull(pos);

            String[] parts = pos.split(",");
            assertEquals(2, parts.length, "Ghost position must be in 'x,y' format");

            Integer.parseInt(parts[0].trim());
            Integer.parseInt(parts[1].trim());
        }
    }

    @Test
    @DisplayName("GhostCL: remainTimeAsEatable returns -1 or positive value")
    void ghost_remainTime_contract() {
        MyGameServer server = new MyGameServer();
        server.initDefaultLevel();

        MyPacmanGameAdapter adapter = new MyPacmanGameAdapter(server);
        GhostCL[] ghosts = adapter.getGhosts(0);

        for (GhostCL g : ghosts) {
            double t = g.remainTimeAsEatable(0);
            assertTrue(t == -1.0 || t > 0.0,
                    "remainTimeAsEatable must be -1 or positive, got: " + t);
        }
    }

    @Test
    @DisplayName("GhostCL: getStatus returns INIT or PLAY only")
    void ghost_status_contract() {
        MyGameServer server = new MyGameServer();
        server.initDefaultLevel();

        MyPacmanGameAdapter adapter = new MyPacmanGameAdapter(server);
        GhostCL[] ghosts = adapter.getGhosts(0);

        for (GhostCL g : ghosts) {
            int st = g.getStatus();
            assertTrue(st == GhostCL.INIT || st == GhostCL.PLAY,
                    "Ghost status must be INIT or PLAY, got: " + st);
        }
    }
}
