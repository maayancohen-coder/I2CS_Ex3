package MyGame.client;

import MyGame.adapters.MyPacmanGameAdapter;
import MyGame.server.MyGameServer;
import assignments.Ex3Algo;
import assignments.StdDraw;
import exe.ex3.game.PacManAlgo;

import java.awt.event.KeyEvent;

public class MyMain {

    private static final int LOOP_PAUSE_MS  = 10;

    // ghost tick rate in manual (ghosts keep moving)
    private static final int GHOST_TICK_MS  = 120;

    // auto step rate (each step includes tick via adapter.move)
    private static final int AUTO_STEP_MS   = 120;

    public static void main(String[] args) {

        // ---- SERVER ----
        MyGameServer server = new MyGameServer();
        server.initDefaultLevel();

        // ---- CLIENT UI ----
        MyGameUI ui = new MyGameUI(36, 40);
        ui.initCanvas(server.getBoard().length, server.getBoard()[0].length);

        // ---- ALGO + ADAPTER (AUTO) ----
        MyPacmanGameAdapter adapter = new MyPacmanGameAdapter(server);
        PacManAlgo algo = new Ex3Algo();

        // default: MANUAL + STOPPED until SPACE
        boolean autoMode = false;
        boolean running  = false;

        long lastGhostTick = 0;
        long lastAutoStep  = 0;

        // edge detection for arrows
        boolean prevLeft=false, prevRight=false, prevUp=false, prevDown=false;
        boolean prevSpace=false;

        while (server.getStatus() == MyGameServer.PLAY) {

            String hud =
                    "Mode: " + (autoMode ? "AUTO" : "MANUAL") +
                            " | " + (running ? "RUNNING" : "PAUSED") +
                            " | Pink: " + server.getPinkEaten() + "/" + server.getPinkTotal() +
                            " (left " + server.getPinkLeft() + ")" +
                            " | Score: " + server.getScore();

            ui.draw(
                    server.getBoard(),
                    server.getPacX(), server.getPacY(), server.getPacDir(),
                    server.getGhosts(),
                    hud
            );

            boolean spaceToggle = false;
            boolean modeToggle  = false;

            // ===== typed keys =====
            while (StdDraw.hasNextKeyTyped()) {
                char k = StdDraw.nextKeyTyped();

                if (k == 'q' || k == 'Q') {
                    ui.drawEndScreen(server.isWon());
                    return;
                }

                if (k == ' ') spaceToggle = true;

                // M toggles manual/auto (English/Hebrew)
                if (k == 'm' || k == 'M' || k == 'מ' || k == 'ם') modeToggle = true;

                // MANUAL movement via WASD (reliable)
                if (running && !autoMode) {
                    if (k == 'a' || k == 'A') server.movePacByDir(MyGameServer.LEFT);
                    if (k == 'd' || k == 'D') server.movePacByDir(MyGameServer.RIGHT);
                    if (k == 'w' || k == 'W') server.movePacByDir(MyGameServer.UP);
                    if (k == 's' || k == 'S') server.movePacByDir(MyGameServer.DOWN);
                }
            }

            // ===== fallback SPACE via isKeyPressed =====
            boolean spacePressed = StdDraw.isKeyPressed(KeyEvent.VK_SPACE);
            if (spacePressed && !prevSpace) spaceToggle = true;
            prevSpace = spacePressed;

            // apply toggles
            if (spaceToggle) {
                running = !running;
                lastGhostTick = 0;
                lastAutoStep  = 0;
                prevLeft = prevRight = prevUp = prevDown = false;
            }

            if (modeToggle) {
                autoMode = !autoMode;
                lastGhostTick = 0;
                lastAutoStep  = 0;
                prevLeft = prevRight = prevUp = prevDown = false;
            }

            // stopped => nothing moves
            if (!running) {
                StdDraw.pause(LOOP_PAUSE_MS);
                continue;
            }

            long now = System.currentTimeMillis();

            // ===== MANUAL: ghosts keep moving (tick), pacman only on key press =====
            if (!autoMode) {
                boolean left  = StdDraw.isKeyPressed(KeyEvent.VK_LEFT);
                boolean right = StdDraw.isKeyPressed(KeyEvent.VK_RIGHT);
                boolean up    = StdDraw.isKeyPressed(KeyEvent.VK_UP);
                boolean down  = StdDraw.isKeyPressed(KeyEvent.VK_DOWN);

                if (left && !prevLeft)   server.movePacByDir(MyGameServer.LEFT);
                if (right && !prevRight) server.movePacByDir(MyGameServer.RIGHT);
                if (up && !prevUp)       server.movePacByDir(MyGameServer.UP);
                if (down && !prevDown)   server.movePacByDir(MyGameServer.DOWN);

                prevLeft = left;
                prevRight = right;
                prevUp = up;
                prevDown = down;

                if (lastGhostTick == 0 || now - lastGhostTick >= GHOST_TICK_MS) {
                    server.tick();
                    lastGhostTick = now;
                }
            }

            // ===== AUTO: pacman follows algo, each step includes tick =====
            if (autoMode) {
                if (lastAutoStep == 0 || now - lastAutoStep >= AUTO_STEP_MS) {
                    int dir = algo.move(adapter);
                    adapter.move(dir); // includes server.movePacByDir + server.tick
                    lastAutoStep = now;
                }
            }

            StdDraw.pause(LOOP_PAUSE_MS);
        }

        ui.drawEndScreen(server.isWon());
    }
}
