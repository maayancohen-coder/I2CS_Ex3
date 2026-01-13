package MyGame.client;

import MyGame.server.MyGameServer;
import assignments.StdDraw;

import java.awt.event.KeyEvent;

/**
 * Client-side input controller.
 *
 * <p>Encapsulates keyboard polling and edge-detection so the main game loop stays clean.</p>
 * <p>Important: This class does NOT modify the server state directly. It only reports user intent.</p>
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
     * @param running  whether game is currently running (not paused by SPACE)
     * @param autoMode whether game is currently in AUTO mode
     */
    public Actions poll(boolean running, boolean autoMode) {
        Actions a = new Actions();

        // typed keys
        while (StdDraw.hasNextKeyTyped()) {
            char k = StdDraw.nextKeyTyped();

            if (k == 'q' || k == 'Q') a.quit = true;
            if (k == ' ') a.spaceToggle = true;

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

        // fallback SPACE edge via isKeyPressed
        boolean spacePressed = StdDraw.isKeyPressed(KeyEvent.VK_SPACE);
        if (spacePressed && !prevSpace) a.spaceToggle = true;
        prevSpace = spacePressed;

        // arrows: edge detection (one step per press)
        a.arrowDir = pollArrowsEdge();

        return a;
    }

    /** Resets edge detection (useful after toggling modes / pause). */
    public void resetEdges() {
        prevLeft = prevRight = prevUp = prevDown = false;
        prevSpace = false;
    }

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
