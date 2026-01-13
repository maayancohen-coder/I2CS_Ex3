package MyGame.client;

import MyGame.adapters.MyPacmanGameAdapter;
import MyGame.server.MyGameServer;
import assignments.Ex3Algo;
import assignments.StdDraw;
import exe.ex3.game.PacManAlgo;

/**
 * Application entry point (client).
 *
 * <p>This class is responsible only for the <b>runtime loop</b>:</p>
 * <ul>
 *   <li>Bootstraps the {@link MyGameServer} (game state + rules).</li>
 *   <li>Initializes {@link MyGameUI} (rendering with {@link StdDraw}).</li>
 *   <li>Reads input via {@link InputController}.</li>
 *   <li>Runs either <b>MANUAL</b> mode (player controls Pac-Man) or <b>AUTO</b> mode (algorithm controls Pac-Man).</li>
 * </ul>
 *
 * <h2>Timing model</h2>
 * <ul>
 *   <li><b>Manual mode:</b> Pac-Man moves only on input; ghosts advance by calling {@link MyGameServer#tick()}
 *       on a fixed timer ({@link #GHOST_TICK_MS}).</li>
 *   <li><b>Auto mode:</b> The algorithm chooses direction via {@link PacManAlgo#move(exe.ex3.game.PacmanGame)}.
 *       The adapter {@link MyPacmanGameAdapter#move(int)} performs the step (Pac move + tick) on a fixed timer
 *       ({@link #AUTO_STEP_MS}).</li>
 * </ul>
 *
 * <h2>Controls</h2>
 * <ul>
 *   <li><b>SPACE</b> - start / pause</li>
 *   <li><b>M</b> - toggle MANUAL/AUTO</li>
 *   <li><b>Arrows / WASD</b> - manual direction</li>
 *   <li><b>Q</b> - quit (shows end screen)</li>
 * </ul>
 */
public class MyMain {

    /**
     * Small loop pause to reduce CPU usage and keep rendering responsive.
     * This is not the gameplay tick rate (see {@link #GHOST_TICK_MS} and {@link #AUTO_STEP_MS}).
     */
    private static final int LOOP_PAUSE_MS  = 10;

    /**
     * Manual mode: ghost/world tick interval (ms).
     * Pac-Man moves immediately on input; {@link MyGameServer#tick()} advances ghosts/collisions/score.
     */
    private static final int GHOST_TICK_MS  = 120;

    /**
     * Auto mode: interval (ms) between algorithm steps.
     * Each auto step calls {@code algo.move(adapter)} and then {@code adapter.move(dir)} (which includes a tick).
     */
    private static final int AUTO_STEP_MS   = 120;

    /**
     * Program entry.
     *
     * <p>Creates server + UI + optional algorithm and runs the main loop
     * until the server status leaves {@link MyGameServer#PLAY}.</p>
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {

        // -------- Server --------
        MyGameServer server = new MyGameServer();
        server.initDefaultLevel();

        // -------- UI --------
        MyGameUI ui = new MyGameUI(36, 40);
        ui.initCanvas(server.getBoard().length, server.getBoard()[0].length);

        // -------- Auto Algo --------
        // Adapter makes our server compatible with the engine's PacmanGame API expected by Ex3Algo.
        MyPacmanGameAdapter adapter = new MyPacmanGameAdapter(server);
        PacManAlgo algo = new Ex3Algo();

        // Default: MANUAL + PAUSED (must press SPACE to start).
        boolean autoMode = false;
        boolean running  = false;

        // Timing state (in ms)
        long lastGhostTick = 0;
        long lastAutoStep  = 0;

        // Input handling (edge detection for toggles)
        InputController input = new InputController();

        // -------- Main loop --------
        while (server.getStatus() == MyGameServer.PLAY) {

            // Render a frame
            ui.draw(
                    server.getBoard(),
                    server.getPacX(), server.getPacY(), server.getPacDir(),
                    server.getGhosts(),
                    buildHud(server, autoMode, running)
            );

            // Read user input (depends on current state)
            InputController.Actions a = input.poll(running, autoMode);

            // Quit immediately (show final screen)
            if (a.quit) {
                ui.drawEndScreen(server.isWon());
                return;
            }

            // Start/Pause toggle
            if (a.spaceToggle) {
                running = !running;
                lastGhostTick = 0;
                lastAutoStep  = 0;
                input.resetEdges();
            }

            // Manual/Auto toggle
            if (a.modeToggle) {
                autoMode = !autoMode;
                lastGhostTick = 0;
                lastAutoStep  = 0;
                input.resetEdges();
            }

            // If paused, do not advance simulation
            if (!running) {
                StdDraw.pause(LOOP_PAUSE_MS);
                continue;
            }

            long now = System.currentTimeMillis();

            if (!autoMode) {
                // MANUAL: Pac-Man steps only on input; ghosts tick on timer
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

            // Small delay for responsiveness and CPU friendliness
            StdDraw.pause(LOOP_PAUSE_MS);
        }

        // Game ended (won or lost)
        ui.drawEndScreen(server.isWon());
    }

    /**
     * Builds the HUD line shown at the top of the screen.
     *
     * <p>UI text lives on the client side (here) to keep the server purely game-logic.</p>
     *
     * @param server   current server instance
     * @param autoMode {@code true} if AUTO mode is active
     * @param running  {@code true} if simulation is currently running (not paused)
     * @return a compact, human-readable status line
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
