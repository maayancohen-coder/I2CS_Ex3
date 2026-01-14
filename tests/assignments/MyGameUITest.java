package assignments;

import MyGame.client.MyGameUI;
import MyGame.server.MyGameServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class MyGameUITest {

    @Test
    @DisplayName("initCanvas: does not throw on valid dimensions")
    void initCanvas_noThrow() {
        MyGameUI ui = new MyGameUI(20, 30);
        assertDoesNotThrow(() -> ui.initCanvas(10, 8));
    }

    @Test
    @DisplayName("draw: does not throw with null ghosts and null hud line")
    void draw_noThrow_nulls() {
        MyGameUI ui = new MyGameUI(10, 10);
        ui.initCanvas(5, 5);

        int[][] board = new int[5][5];

        assertDoesNotThrow(() ->
                ui.draw(board, 2, 2, MyGameServer.LEFT, null, null)
        );
    }

    @Test
    @DisplayName("draw: does not throw when pac sprite resources are missing (fallback circle)")
    void draw_noThrow_missingPacSprite() {
        MyGameUI ui = new MyGameUI(10, 10);
        ui.initCanvas(5, 5);

        int[][] board = new int[5][5];

        assertDoesNotThrow(() ->
                ui.draw(board, 2, 2, MyGameServer.UP, null, "HUD")
        );
    }

    @Test
    @DisplayName("draw: does not throw when ghosts exist but are unreleased")
    void draw_noThrow_unreleasedGhosts() throws Exception {
        MyGameUI ui = new MyGameUI(10, 10);
        ui.initCanvas(7, 7);

        int[][] board = new int[7][7];

        // Create Ghost instance even if constructor is not public
        MyGameServer.Ghost g = newGhostInstance();
        setField(g, "x", 3);
        setField(g, "y", 3);
        setField(g, "released", false);
        setField(g, "imgPath", "//does-not-exist.png");

        assertDoesNotThrow(() ->
                ui.draw(board, 1, 1, MyGameServer.RIGHT, new MyGameServer.Ghost[]{g}, "HUD")
        );
    }

    @Test
    @DisplayName("drawEndScreen: does not throw for win/lose")
    void drawEndScreen_noThrow() {
        MyGameUI ui = new MyGameUI(20, 30);
        ui.initCanvas(10, 10);

        assertDoesNotThrow(() -> ui.drawEndScreen(true));
        assertDoesNotThrow(() -> ui.drawEndScreen(false));
    }

    /* ----------------- reflection helpers ----------------- */

    private static MyGameServer.Ghost newGhostInstance() throws Exception {
        Class<?> c = MyGameServer.Ghost.class;

        // Try no-arg constructor first
        try {
            Constructor<?> ctor = c.getDeclaredConstructor();
            ctor.setAccessible(true);
            return (MyGameServer.Ghost) ctor.newInstance();
        } catch (NoSuchMethodException ignore) {
            // Otherwise, use the first constructor with default values
            Constructor<?> ctor = c.getDeclaredConstructors()[0];
            ctor.setAccessible(true);
            Class<?>[] types = ctor.getParameterTypes();
            Object[] args = new Object[types.length];

            for (int i = 0; i < types.length; i++) {
                args[i] = defaultValue(types[i]);
            }

            return (MyGameServer.Ghost) ctor.newInstance(args);
        }
    }

    private static Object defaultValue(Class<?> t) {
        if (!t.isPrimitive()) return null;
        if (t == boolean.class) return false;
        if (t == byte.class) return (byte) 0;
        if (t == short.class) return (short) 0;
        if (t == int.class) return 0;
        if (t == long.class) return 0L;
        if (t == float.class) return 0f;
        if (t == double.class) return 0d;
        if (t == char.class) return (char) 0;
        return null;
    }

    private static void setField(Object obj, String name, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }
}
