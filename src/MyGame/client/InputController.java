package MyGame.client;

import MyGame.server.MyGameServer;
import assignments.StdDraw;

import java.awt.event.KeyEvent;

/**
 * Keyboard input controller for the client.
 *
 * <p>This class centralizes all keyboard polling and <b>edge-detection</b> logic
 * so {@code MyMain} can stay focused on the game loop.</p>
 *
 * <h2>What this class does</h2>
 * <ul>
 *   <li>Reads <b>typed</b> keys via {@link StdDraw#hasNextKeyTyped()} / {@link StdDraw#nextKeyTyped()}.</li>
 *   <li>Reads <b>pressed</b> keys via {@link StdDraw#isKeyPressed(int)} for arrow keys and SPACE fallback.</li>
 *   <li>Converts raw input into a simple {@link Actions} object.</li>
 * </ul>
 *
 * <h2>Important</h2>
 * <ul>
 *   <li>This class <b>does not</b> change game/server state. It only reports user intent.</li>
 *   <li>Arrow keys are handled with <b>edge detection</b>: one movement per key press (not continuous hold).</li>
 *   <li>WASD uses the typed-keys queue: one movement per typed character.</li>
 * </ul>
 */
public class InputController {

    /**
     * A compact "intent" object returned from {@link #poll(boolean, boolean)}.
     * All fields are public for simple, allocation-light use in the main loop.
     */
    public static class Actions {

        /** True if user requested quit (Q / q). */
        public boolean quit = false;

        /** True if user toggled pause/run (SPACE). */
        public boolean spaceToggle = false;

        /** True if user toggled MANUAL/AUTO mode (M / m / מ / ם). */
        public boolean modeToggle = false;

        /**
         * One-step manual movement from WASD typed input.
         * {@link MyGameServer#STAY} if none.
         */
        public int wasdDir = MyGameServer.STAY;

        /**
         * One-step manual movement from arrow key press (edge-detected).
         * {@link MyGameServer#STAY} if none.
         */
        public int arrowDir = MyGameServer.STAY;
    }

    // Previous pressed state for edge detection (arrows + space fallback)
    private boolean prevLeft = false, prevRight = false, prevUp = false, prevDown = false;
    private boolean prevSpace = false;

    /**
     * Polls user input once for the current frame.
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>Quit: Q/q (typed)</li>
     *   <li>Pause toggle: SPACE (typed, and also pressed-edge fallback)</li>
     *   <li>Mode toggle: M/m and Hebrew equivalents מ/ם (typed)</li>
     *   <li>Manual movement:
     *     <ul>
     *       <li>WASD: typed (one step per key typed), only if {@code running && !autoMode}</li>
     *       <li>Arrows: pressed-edge (one step per press), always reported; main loop may ignore in AUTO</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param running  whether the game is currently running (not paused)
     * @param autoMode whether AUTO mode is enabled (algorithm controls Pac-Man)
     * @return an {@link Actions} object describing what the user requested in this frame
     */
    public Actions poll(boolean running, boolean autoMode) {
        Actions a = new Actions();

        // --- Typed keys (queue-based): discrete events ---
        while (StdDraw.hasNextKeyTyped()) {
            char k = StdDraw.nextKeyTyped();

            if (k == 'q' || k == 'Q') a.quit = true;
            if (k == ' ') a.spaceToggle = true;

            // M toggle (English/Hebrew)
            if (k == 'm' || k == 'M' || k == 'מ' || k == 'ם') a.modeToggle = true;

            // Manual movement via WASD (typed = one step)
            if (running && !autoMode) {
                if (k == 'a' || k == 'A') a.wasdDir = MyGameServer.LEFT;
                if (k == 'd' || k == 'D') a.wasdDir = MyGameServer.RIGHT;
                if (k == 'w' || k == 'W') a.wasdDir = MyGameServer.UP;
                if (k == 's' || k == 'S') a.wasdDir = MyGameServer.DOWN;
            }
        }

        // --- Pressed keys: continuous state (we convert to edge events where needed) ---

        // SPACE fallback edge via isKeyPressed (helps in environments where typed SPACE is flaky)
        boolean spacePressed = StdDraw.isKeyPressed(KeyEvent.VK_SPACE);
        if (spacePressed && !prevSpace) a.spaceToggle = true;
        prevSpace = spacePressed;

        // Arrows: edge detection (one movement per press)
        a.arrowDir = pollArrowsEdge();

        return a;
    }

    /**
     * Resets edge-detection state.
     *
     * <p>Useful after switching modes or toggling pause, to prevent "stuck" edges
     * from triggering an unintended movement/toggle on the next frame.</p>
     */
    public void resetEdges() {
        prevLeft = prevRight = prevUp = prevDown = false;
        prevSpace = false;
    }

    /**
     * Reads arrow keys as pressed-state and converts them into a single direction
     * only on the transition from "not pressed" to "pressed".
     *
     * <p>Priority order (if multiple edges happen simultaneously): left, right, up, down.</p>
     *
     * @return a server direction constant, or {@link MyGameServer#STAY} if no new arrow press occurred
     */
    private int pollArrowsEdge() {
        boolean left  = StdDraw.isKeyPressed(KeyEvent.VK_LEFT);
        boolean right = StdDraw.isKeyPressed(KeyEvent.VK_RIGHT);
        boolean up    = StdDraw.isKeyPressed(KeyEvent.VK_UP);
        boolean down  = StdDraw.isKeyPressed(KeyEvent.VK_DOWN);

        int dir = MyGameServer.STAY;
        if (left && !prevLeft) dir = MyGameServer.LEFT;
        else if (right && !prevRight) dir = MyGameServer.RIGHT;
        else if (up && !prevUp) dir = MyGameServer.UP;
        else if (down && !prevDown) dir = MyGameServer.DOWN;

        // Update previous state for next frame
        prevLeft = left;
        prevRight = right;
        prevUp = up;
        prevDown = down;

        return dir;
    }
}
