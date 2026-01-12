package assignments;

import exe.ex3.game.GhostCL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class Ex3AlgoIsPoweredTest {

    @Test
    @DisplayName("isPowered: empty ghosts -> false")
    void isPowered_empty_false() {
        Ex3Algo algo = new Ex3Algo();
        assertFalse(invokeIsPowered(algo, new GhostCL[0]));
    }

    @Test
    @DisplayName("isPowered: one ghost status!=0 and eatableTime>0 -> true (proxy stub)")
    void isPowered_true_case() {
        Assumptions.assumeTrue(GhostCL.class.isInterface(),
                "GhostCL is not an interface in this environment; skipping proxy-based stub test.");

        GhostCL g = ghostProxy(/*status*/1, /*eatable*/5.0, "3,3");
        Ex3Algo algo = new Ex3Algo();

        assertTrue(invokeIsPowered(algo, new GhostCL[]{g}));
    }

    @Test
    @DisplayName("isPowered: status!=0 but eatableTime==0 -> false")
    void isPowered_eatableZero_false() {
        Assumptions.assumeTrue(GhostCL.class.isInterface(),
                "GhostCL is not an interface in this environment; skipping proxy-based stub test.");

        GhostCL g = ghostProxy(1, 0.0, "3,3");
        Ex3Algo algo = new Ex3Algo();

        assertFalse(invokeIsPowered(algo, new GhostCL[]{g}));
    }

    @Test
    @DisplayName("isPowered: performance many ghosts under budget")
    void isPowered_perf() {
        Assumptions.assumeTrue(GhostCL.class.isInterface(),
                "GhostCL is not an interface in this environment; skipping proxy-based stub test.");

        GhostCL[] ghosts = new GhostCL[10_000];
        for (int i = 0; i < ghosts.length; i++) {
            ghosts[i] = ghostProxy(1, -1.0, "0,0"); // not powered
        }

        Ex3Algo algo = new Ex3Algo();

        assertTimeoutPreemptively(Duration.ofMillis(40), () -> {
            assertFalse(invokeIsPowered(algo, ghosts));
        });
    }

    /* ============= Reflection invoke ============= */

    private static boolean invokeIsPowered(Ex3Algo algo, GhostCL[] ghosts) {
        try {
            Method m = Ex3Algo.class.getDeclaredMethod("isPowered", GhostCL[].class);
            m.setAccessible(true);
            return (boolean) m.invoke(algo, (Object) ghosts);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* ============= Ghost stub via Proxy (works if GhostCL is interface) ============= */

    private static GhostCL ghostProxy(int status, double eatableTime, String pos) {
        return (GhostCL) Proxy.newProxyInstance(
                GhostCL.class.getClassLoader(),
                new Class[]{GhostCL.class},
                (proxy, method, args) -> {
                    String name = method.getName();

                    if (name.equals("getStatus")) return status;
                    if (name.equals("remainTimeAsEatable")) return eatableTime;
                    if (name.equals("getPos")) return pos;

                    // Default returns for other methods (safe fallbacks)
                    Class<?> rt = method.getReturnType();
                    if (rt.equals(boolean.class)) return false;
                    if (rt.equals(int.class)) return 0;
                    if (rt.equals(double.class)) return 0.0;
                    return null;
                }
        );
    }
}
