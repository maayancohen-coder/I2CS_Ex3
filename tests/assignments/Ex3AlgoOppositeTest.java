package assignments;

import exe.ex3.game.Game;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for opposite(int) helper.
 *
 * Contract:
 * - opposite(UP)   == DOWN
 * - opposite(DOWN) == UP
 * - opposite(LEFT) == RIGHT
 * - opposite(RIGHT)== LEFT
 * - any other value returns -1 (as implemented)
 */
public class Ex3AlgoOppositeTest {

    @Test
    @DisplayName("opposite: UP <-> DOWN and LEFT <-> RIGHT")
    void opposite_basicPairs() {
        assertEquals(Game.DOWN, invokeOpposite(Game.UP));
        assertEquals(Game.UP, invokeOpposite(Game.DOWN));
        assertEquals(Game.RIGHT, invokeOpposite(Game.LEFT));
        assertEquals(Game.LEFT, invokeOpposite(Game.RIGHT));
    }

    @Test
    @DisplayName("opposite: unknown direction returns -1")
    void opposite_unknownDir() {
        assertEquals(-1, invokeOpposite(-999));
        assertEquals(-1, invokeOpposite(999));
        assertEquals(-1, invokeOpposite(Game.STAY)); // STAY isn't handled -> -1 in your code
    }

    /* ------------------------------------------------
       helper to access private opposite(int) via reflection
       (opposite is private in Ex3Algo)
       ------------------------------------------------ */
    private int invokeOpposite(int dir) {
        try {
            Ex3Algo algo = new Ex3Algo();
            Method m = Ex3Algo.class.getDeclaredMethod("opposite", int.class);
            m.setAccessible(true);
            return (int) m.invoke(algo, dir);
        } catch (Exception e) {
            fail("Reflection call to opposite(int) failed: " + e);
            return -1; // unreachable
        }
    }
}
