package MyGame.client;

import MyGame.server.MyGameServer;
import assignments.StdDraw;

import java.awt.event.KeyEvent;

/**
 * Client-side keyboard input controller.
 *
 * OVERVIEW
 * --------
 * This class polls the keyboard and translates raw key events into a simple Actions object.
 * It keeps the main game loop clean by centralizing all input logic in one place.
 *
 * IMPORTANT
 * ---------
 * This class does not modify the server or the game state.
 * It only reports user intent to the caller.
 *
 * INPUT SOURCES
 * -------------
 * Typed keys:
 *   Read using StdDraw.hasNextKeyTyped and StdDraw.nextKeyTyped.
 *   Used for discrete actions such as quit, toggles, and one-step WASD movement.
 *
 * Pressed keys:
 *   Read using StdDraw.isKeyPressed.
 *   Used for arrow keys and a SPACE fallback.
 *
 * EDGE DETECTION
 * --------------
 * Arrow keys are handled with edge detection.
 * This means a direction is reported only when the key changes from not pressed to pressed.
 * Holding an arrow key does not generate continuous movement.
 *
 * RETURN VALUE
 * ------------
 * The poll method returns an Actions object that may include:
 * quit request
 * pause toggle request
 * mode toggle request
 * one-step movement from WASD
 * one-step movement from arrow keys
 */
public class InputController {

    /** Results of polling input for the current frame. */
    public static class Actions {
        public boolean quit = false;
        public boolean spaceToggle = false;
        public boolean modeToggle = false;

        /** One-step manual movement from WASD typed input (STAY if none). */
        public int wasdDir = MyGameServer.STAY;

        /** One-step manual movement from arrow edge (STAY if none). */
        public int arrowDir = MyGameServer.STAY;
    }

    private boolean prevLeft=false, prevRight=false, prevUp=false, prevDown=false;
    private boolean prevSpace=false;

    /**
     * Polls keys once for this frame.
     *
     * Behavior summary:
     * Quit:
     *   Q or q
     *
     * Pause toggle:
     *   SPACE (typed), plus a pressed-key fallback using isKeyPressed
     *
     * Mode toggle:
     *   M or m
     *   Hebrew equivalents are also supported: מ and ם
     *
     * Manual movement:
     *   WASD typed input produces one movement step per typed key
     *   Arrow keys produce one movement step per key press using edge detection
     *
     * @param running  whether game is currently running (not paused by SPACE)
     * @param autoMode whether game is currently in AUTO mode
     * @return an Actions object describing the input for this frame
     */
    public Actions poll(boolean running, boolean autoMode) {
        Actions a = new Actions();
        boolean spaceTypedThisFrame = false;

        // typed keys
        while (StdDraw.hasNextKeyTyped()) {
            char k = StdDraw.nextKeyTyped();

            if (k == 'q' || k == 'Q') a.quit = true;
            if (k == ' ') {
                a.spaceToggle = true;
                spaceTypedThisFrame = true;
            }
            // M toggle (English/Hebrew)
            if (k == 'm' || k == 'M' || k == 'מ' || k == 'ם') a.modeToggle = true;

            // manual movement via WASD (typed = one step)
            if (running && !autoMode) {
                if (k == 'a' || k == 'A') a.wasdDir = MyGameServer.LEFT;
                if (k == 'd' || k == 'D') a.wasdDir = MyGameServer.RIGHT;
                if (k == 'w' || k == 'W') a.wasdDir = MyGameServer.UP;
                if (k == 's' || k == 'S') a.wasdDir = MyGameServer.DOWN;
            }
        }

        boolean spacePressed = StdDraw.isKeyPressed(KeyEvent.VK_SPACE);
        if (!spaceTypedThisFrame && spacePressed && !prevSpace) {
            a.spaceToggle = true;
        }
        prevSpace = spacePressed;

        // arrows: edge detection (one step per press)
        a.arrowDir = pollArrowsEdge();

        return a;
    }

    /**
     * Resets internal edge-detection state.
     *
     * Use this after toggling pause or switching modes.
     * It prevents a key that is already held down from triggering an immediate action
     * on the next frame.
     */
    public void resetEdges() {
        prevLeft = prevRight = prevUp = prevDown = false;
        prevLeft = prevRight = prevUp = prevDown = false;
        prevSpace = StdDraw.isKeyPressed(KeyEvent.VK_SPACE);

    }

    /**
     * Reads arrow keys and returns a single direction on the press edge only.
     *
     * If multiple arrows are pressed at the same time, the priority is:
     * left, right, up, down.
     *
     * @return a server direction constant, or STAY if no new arrow press occurred
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

        prevLeft = left;
        prevRight = right;
        prevUp = up;
        prevDown = down;

        return dir;
    }
}
