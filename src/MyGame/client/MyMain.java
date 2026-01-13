package MyGame.client;

import MyGame.adapters.MyPacmanGameAdapter;
import MyGame.server.MyGameServer;
import assignments.Ex3Algo;
import assignments.StdDraw;
import exe.ex3.game.PacManAlgo;

/**
 * Client entry point:
 * - renders UI (StdDraw)
 * - handles input (InputController)
 * - schedules ticks and steps (manual/auto timing)
 *
 * Server-side game rules are in {@link MyGameServer}.
 */
public class MyMain {

    private static final int LOOP_PAUSE_MS  = 10;

    // manual: ghosts keep moving even if pacman doesn't
    private static final int GHOST_TICK_MS  = 120;

    // auto: each step includes pac move + tick
    private static final int AUTO_STEP_MS   = 120;

    public static void main(String[] args) {

        // -------- Server --------
        MyGameServer server = new MyGameServer();
        server.initDefaultLevel();

        // -------- UI --------
        MyGameUI ui = new MyGameUI(36, 40);
        ui.initCanvas(server.getBoard().length, server.getBoard()[0].length);

        // -------- Auto Algo --------
        MyPacmanGameAdapter adapter = new MyPacmanGameAdapter(server);
        PacManAlgo algo = new Ex3Algo();

        // default: MANUAL + STOPPED until SPACE
        boolean autoMode = false;
        boolean running  = false;

        long lastGhostTick = 0;
        long lastAutoStep  = 0;

        InputController input = new InputController();

        while (server.getStatus() == MyGameServer.PLAY) {

            ui.draw(
                    server.getBoard(),
                    server.getPacX(), server.getPacY(), server.getPacDir(),
                    server.getGhosts(),
                    buildHud(server, autoMode, running)
            );

            InputController.Actions a = input.poll(running, autoMode);

            if (a.quit) {
                ui.drawEndScreen(server.isWon());
                return;
            }

            if (a.spaceToggle) {
                running = !running;
                lastGhostTick = 0;
                lastAutoStep  = 0;
                input.resetEdges();
            }

            if (a.modeToggle) {
                autoMode = !autoMode;
                lastGhostTick = 0;
                lastAutoStep  = 0;
                input.resetEdges();
            }

            if (!running) {
                StdDraw.pause(LOOP_PAUSE_MS);
                continue;
            }

            long now = System.currentTimeMillis();

            if (!autoMode) {
                // MANUAL: pacman steps only on input; ghosts tick on timer
                if (a.arrowDir != MyGameServer.STAY) server.movePacByDir(a.arrowDir);
                if (a.wasdDir  != MyGameServer.STAY) server.movePacByDir(a.wasdDir);

                if (lastGhostTick == 0 || now - lastGhostTick >= GHOST_TICK_MS) {
                    server.tick();
                    lastGhostTick = now;
                }
            } else {
                // AUTO: algo decides; adapter.move includes tick
                if (lastAutoStep == 0 || now - lastAutoStep >= AUTO_STEP_MS) {
                    int dir = algo.move(adapter);
                    adapter.move(dir);
                    lastAutoStep = now;
                }
            }

            StdDraw.pause(LOOP_PAUSE_MS);
        }

        ui.drawEndScreen(server.isWon());
    }

    /**
     * Builds a compact top HUD line.
     * Keep UI text here (client), not in the server.
     */
    private static String buildHud(MyGameServer server, boolean autoMode, boolean running) {
        return "Mode: " + (autoMode ? "AUTO" : "MANUAL") +
                " | " + (running ? "RUNNING" : "PAUSED") +
                " | Pink: " + server.getPinkEaten() + "/" + server.getPinkTotal() +
                " (left " + server.getPinkLeft() + ")" +
                " | Score: " + server.getScore() +
                " | keys: SPACE start/pause, M toggle, arrows/WASD manual, Q quit";
    }
}
