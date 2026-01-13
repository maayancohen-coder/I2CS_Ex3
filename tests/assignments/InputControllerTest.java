package assignments;

import MyGame.client.InputController;
import MyGame.server.MyGameServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class InputControllerTest {

    @Test
    @DisplayName("resetEdges: clears internal edge state flags")
    void resetEdges_clearsFlags() throws Exception {
        InputController ic = new InputController();

        setBool(ic, "prevLeft", true);
        setBool(ic, "prevRight", true);
        setBool(ic, "prevUp", true);
        setBool(ic, "prevDown", true);
        setBool(ic, "prevSpace", true);

        ic.resetEdges();

        assertFalse(getBool(ic, "prevLeft"));
        assertFalse(getBool(ic, "prevRight"));
        assertFalse(getBool(ic, "prevUp"));
        assertFalse(getBool(ic, "prevDown"));
        assertFalse(getBool(ic, "prevSpace"));
    }

    @Test
    @DisplayName("poll: does not throw in any mode combination (no keyboard input)")
    void poll_noThrow_allModes() {
        InputController ic = new InputController();

        assertDoesNotThrow(() -> ic.poll(false, false)); // paused manual
        assertDoesNotThrow(() -> ic.poll(true,  false)); // running manual
        assertDoesNotThrow(() -> ic.poll(false, true));  // paused auto
        assertDoesNotThrow(() -> ic.poll(true,  true));  // running auto
    }

    @Test
    @DisplayName("poll: returns non-null Actions and default directions are STAY when no input exists")
    void poll_returnsActions_defaultsAreStay() {
        InputController ic = new InputController();

        InputController.Actions a = ic.poll(false, false);

        assertNotNull(a);
        assertFalse(a.quit);
        // spaceToggle/modeToggle could theoretically be true if StdDraw queue has leftovers,
        // but normally with no input they should be false.
        assertEquals(MyGameServer.STAY, a.wasdDir);
        assertEquals(MyGameServer.STAY, a.arrowDir);
    }

    /* ------------ reflection helpers ------------ */

    private static void setBool(Object o, String field, boolean v) throws Exception {
        Field f = o.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.setBoolean(o, v);
    }

    private static boolean getBool(Object o, String field) throws Exception {
        Field f = o.getClass().getDeclaredField(field);
        f.setAccessible(true);
        return f.getBoolean(o);
    }
}
