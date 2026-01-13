package assignments;
import MyGame.adapters.MyPacmanGameAdapter;
import MyGame.server.MyGameServer;
import exe.ex3.game.GhostCL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MyPacmanGameAdapterTest {

    @Test
    @DisplayName("init: initializes default level and matches cyclic flag")
    void init_setsDefaultLevel_andCyclicMatches() {
        MyGameServer s = new MyGameServer();
        MyPacmanGameAdapter a = new MyPacmanGameAdapter(s);

        a.init(0, "", true, 0L, 1.0, 0, 0);
        assertTrue(a.isCyclic(), "Adapter must reflect server cyclic after init(cyclic=true)");

        a.init(0, "", false, 0L, 1.0, 0, 0);
        assertFalse(a.isCyclic(), "Adapter must reflect server cyclic after init(cyclic=false)");

        int[][] b = a.getGame(0);
        assertNotNull(b);
        assertTrue(b.length > 0 && b[0].length > 0);
    }

    @Test
    @DisplayName("getData: returns expected compact format with score and pink left")
    void getData_format() {
        MyGameServer s = new MyGameServer();
        s.initDefaultLevel();
        MyPacmanGameAdapter a = new MyPacmanGameAdapter(s);

        String d = a.getData(0);
        assertNotNull(d);
        assertTrue(d.contains("Score="), "Must include Score=");
        assertTrue(d.contains("PinkLeft="), "Must include PinkLeft=");
    }

    @Test
    @DisplayName("getPos: returns 'x,y' with integers")
    void getPos_format() {
        MyGameServer s = new MyGameServer();
        s.initDefaultLevel();
        MyPacmanGameAdapter a = new MyPacmanGameAdapter(s);

        String p = a.getPos(0);
        assertNotNull(p);

        String[] parts = p.split(",");
        assertEquals(2, parts.length, "Position must be in 'x,y' format");
        Integer.parseInt(parts[0].trim());
        Integer.parseInt(parts[1].trim());
    }

    @Test
    @DisplayName("getStatus: maps server status to PacmanGame INIT/PLAY/DONE")
    void getStatus_mapping() {
        MyGameServer s = new MyGameServer();
        MyPacmanGameAdapter a = new MyPacmanGameAdapter(s);

        s.initDefaultLevel();
        int st = a.getStatus();
        assertTrue(st == MyPacmanGameAdapter.PLAY || st == MyPacmanGameAdapter.INIT,
                "After initDefaultLevel status should be INIT or PLAY depending on server implementation");

        a.end(0);
        assertEquals(MyPacmanGameAdapter.DONE, a.getStatus(), "After end() adapter status must be DONE");
    }

    @Test
    @DisplayName("getGhosts: never returns null; returns array aligned with server ghosts length")
    void getGhosts_notNull_andLengthMatches() {
        MyGameServer s = new MyGameServer();
        s.initDefaultLevel();
        MyPacmanGameAdapter a = new MyPacmanGameAdapter(s);

        GhostCL[] gs = a.getGhosts(0);
        assertNotNull(gs, "Adapter must never return null ghosts array");

        MyGameServer.Ghost[] raw = s.getGhosts();
        if (raw == null) {
            assertEquals(0, gs.length, "If server returns null ghosts, adapter returns empty array");
        } else {
            assertEquals(raw.length, gs.length, "Adapter ghost array length must match server ghost array length");
        }
    }

    @Test
    @DisplayName("getGhosts: each GhostCL returns pos in 'x,y' and info non-empty")
    void getGhosts_ghostFields_basic() {
        MyGameServer s = new MyGameServer();
        s.initDefaultLevel();
        MyPacmanGameAdapter a = new MyPacmanGameAdapter(s);

        GhostCL[] gs = a.getGhosts(0);
        for (GhostCL g : gs) {
            assertNotNull(g);

            String pos = g.getPos(0);
            assertNotNull(pos);
            String[] parts = pos.split(",");
            assertEquals(2, parts.length, "Ghost pos must be 'x,y'");
            Integer.parseInt(parts[0].trim());
            Integer.parseInt(parts[1].trim());

            String info = g.getInfo();
            assertNotNull(info);
            assertFalse(info.trim().isEmpty());
        }
    }

    @Test
    @DisplayName("move: returns getData snapshot and advances the game (tick happens)")
    void move_returnsSnapshot_andAdvancesTick() {
        MyGameServer s = new MyGameServer();
        s.initDefaultLevel();
        MyPacmanGameAdapter a = new MyPacmanGameAdapter(s);

        String beforePos = a.getPos(0);
        String beforeData = a.getData(0);

        String afterData = a.move(MyGameServer.RIGHT);

        assertNotNull(afterData);
        assertTrue(afterData.contains("Score=") && afterData.contains("PinkLeft="),
                "move() should return the same snapshot format as getData()");

        String afterPos = a.getPos(0);

        boolean posChanged = !afterPos.equals(beforePos);
        boolean dataChanged = !afterData.equals(beforeData);

        assertTrue(posChanged || dataChanged,
                "move() must advance game: position or snapshot should change after a step");
    }

    @Test
    @DisplayName("getKeyChar: adapter does not use it and returns null")
    void getKeyChar_isNull() {
        MyGameServer s = new MyGameServer();
        s.initDefaultLevel();
        MyPacmanGameAdapter a = new MyPacmanGameAdapter(s);

        assertNull(a.getKeyChar());
    }
}
