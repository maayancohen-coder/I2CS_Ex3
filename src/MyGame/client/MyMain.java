package MyGame.client;

import MyGame.adapters.MyPacmanGameAdapter;
import MyGame.server.MyGameServer;
import assignments.Ex3Algo;
import assignments.StdDraw;
import exe.ex3.game.PacManAlgo;

/**
 * Application entry point of the game client.
 *
 * HOW TO PLAY
 * -----------
 * SPACE  – start or pause the game
 * M      – toggle between manual and automatic mode
 *
 * MANUAL – control Pac-Man using the arrow keys or WASD
 * AUTO   – Pac-Man is controlled automatically by the algorithm
 *
 * Goal:
 * Eat all pink pellets to win the game.
 * Green pellets make ghosts eatable for a short time.
 *
 * Q – quit the game and show the end screen
 *
 * --------------------------------------------------
 *
 * OVERVIEW
 * --------
 * This class manages the runtime loop on the client side only.
 * It is responsible for rendering, input polling, and deciding when to advance the simulation.
 * All game rules, collisions, scoring, and state transitions are handled by MyGameServer.
 *
 * MAIN COMPONENTS
 * ---------------
 * MyGameServer
 *   Owns the game state and implements the rules.
 *
 * MyGameUI
 *   Renders the current state using StdDraw.
 *
 * InputController
 *   Polls keyboard input and returns user intent (pause, mode toggle, directions, quit).
 *
 * Auto algorithm and adapter
 *   In AUTO mode, Ex3Algo expects a PacmanGame interface.
 *   MyPacmanGameAdapter bridges MyGameServer to that interface.
 *
 * TIMING MODEL
 * ------------
 * Manual mode:
 *   Pac-Man moves only when the user provides direction input.
 *   Ghosts and world updates advance by calling server.tick() periodically
 *   according to GHOST_TICK_MS.
 *
 * Auto mode:
 *   The algorithm selects a direction using algo.move(adapter).
 *   The adapter performs adapter.move(dir), which includes a Pac-Man step and a server tick,
 *   according to AUTO_STEP_MS.
 *
 * Notes:
 *   LOOP_PAUSE_MS is a small delay for responsiveness and lower CPU usage.
 *   It is not the gameplay tick rate.
 */
public class MyMain {

    /**
     * Small loop pause to reduce CPU usage and keep rendering responsive.
     * This is not the gameplay tick rate.
     */
    private static final int LOOP_PAUSE_MS  = 10;

    /**
     * Manual mode tick interval in milliseconds.
     * Controls how often server.tick() is called while the game is running in MANUAL mode.
     */
    private static final int GHOST_TICK_MS  = 120;

    /**
     * Auto mode step interval in milliseconds.
     * Controls how often the algorithm selects a move and the adapter advances the simulation.
     */
    private static final int AUTO_STEP_MS   = 120;

    /**
     * Program entry point.
     *
     * Execution flow:
     * 1. Create and initialize the server level.
     * 2. Create the UI and initialize the canvas according to the board dimensions.
     * 3. Create an adapter and an algorithm for AUTO mode.
     * 4. Enter the main loop:
     *    - draw current frame
     *    - poll input
     *    - apply mode toggles and pause toggles
     *    - advance the simulation according to the active mode and timing
     * 5. When the server is no longer in PLAY status, show the end screen.
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

        // -------- Auto Algorithm --------
        // Ex3Algo works with the PacmanGame interface, so we wrap our server with an adapter.
        MyPacmanGameAdapter adapter = new MyPacmanGameAdapter(server);
        PacManAlgo algo = new Ex3Algo();

        // Default state: manual control, paused until SPACE is pressed.
        boolean autoMode = false;
        boolean running  = false;

        // Timing state used for periodic ticking / stepping.
        long lastGhostTick = 0;
        long lastAutoStep  = 0;

        // Input polling and edge detection.
        InputController input = new InputController();

        // -------- Main loop --------
        while (server.getStatus() == MyGameServer.PLAY) {

            // Render one frame based on the current server state.
            ui.draw(
                    server.getBoard(),
                    server.getPacX(), server.getPacY(), server.getPacDir(),
                    server.getGhosts(),
                    buildHud(server, autoMode, running)
            );

            // Poll input for this frame (directions, toggles, quit).
            InputController.Actions a = input.poll(running, autoMode);

            // Quit immediately and show the end screen.
            if (a.quit) {
                ui.drawEndScreen(server.isWon());
                return;
            }

            // Toggle running state (pause/resume).
            if (a.spaceToggle) {
                running = !running;
                lastGhostTick = 0;
                lastAutoStep  = 0;
                input.resetEdges();
            }

            // Toggle mode (manual/auto).
            if (a.modeToggle) {
                autoMode = !autoMode;
                lastGhostTick = 0;
                lastAutoStep  = 0;
                input.resetEdges();
            }

            // If paused, do not advance simulation.
            if (!running) {
                StdDraw.pause(LOOP_PAUSE_MS);
                continue;
            }

            long now = System.currentTimeMillis();

            if (!autoMode) {
                // MANUAL:
                // Pac-Man moves one step per user input.
                // Ghosts/world advance periodically via server.tick().
                if (a.arrowDir != MyGameServer.STAY) server.movePacByDir(a.arrowDir);
                if (a.wasdDir  != MyGameServer.STAY) server.movePacByDir(a.wasdDir);

                if (lastGhostTick == 0 || now - lastGhostTick >= GHOST_TICK_MS) {
                    server.tick();
                    lastGhostTick = now;
                }
            } else {
                // AUTO:
                // The algorithm selects a direction; adapter.move(dir) applies the move and advances time.
                if (lastAutoStep == 0 || now - lastAutoStep >= AUTO_STEP_MS) {
                    int dir = algo.move(adapter);
                    adapter.move(dir);
                    lastAutoStep = now;
                }
            }

            // Small delay for responsiveness and CPU friendliness.
            StdDraw.pause(LOOP_PAUSE_MS);
        }

        // Game ended (won or lost).
        ui.drawEndScreen(server.isWon());
    }

    /**
     * Builds a compact HUD line displayed at the top of the screen.
     *
     * This method intentionally stays on the client side:
     * the server should remain focused on game logic and state, not UI strings.
     *
     * @param server   active server instance
     * @param autoMode true if AUTO mode is enabled
     * @param running  true if the simulation is currently running (not paused)
     * @return a human-readable HUD string
     */
    private static String buildHud(MyGameServer server, boolean autoMode, boolean running) {
        return "Mode: " + (autoMode ? "AUTO" : "MANUAL") +
                " | " + (running ? "RUNNING" : "PAUSED") +
                " | Pink: " + server.getPinkEaten() + "/" + server.getPinkTotal() +
                " | Score: " + server.getScore();

    }
}
