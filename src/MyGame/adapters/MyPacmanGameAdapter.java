package MyGame.adapters;

import MyGame.server.MyGameServer;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacmanGame;

/**
 * Adapter between the server implementation and the course engine interface.
 *
 * OVERVIEW
 * --------
 * The course framework expects a PacmanGame object.
 * This class wraps MyGameServer and exposes it through the PacmanGame interface,
 * so external code can run on top of your server without changing the server itself.
 *
 * RESPONSIBILITIES
 * ---------------
 * Initialize the server according to the engine request.
 * Delegate board and entity queries to the server.
 * Perform a single step when move is called.
 * Convert server ghosts into GhostCL objects expected by the engine.
 *
 * SINGLE STEP POLICY
 * ------------------
 * move(dir) performs exactly one logical step:
 * 1. Move Pac-Man one step in the requested direction.
 * 2. Advance the simulation by one tick on the server.
 *
 * STATUS MAPPING
 * --------------
 * The server status is mapped to the engine constants:
 * INIT, PLAY, DONE.
 *
 * NOTES
 * -----
 * Some engine parameters in init are accepted for compatibility but are not used here,
 * because the server is responsible for building its own default level.
 */
public class MyPacmanGameAdapter implements PacmanGame {

    private final MyGameServer g;

    public MyPacmanGameAdapter(MyGameServer server) {
        this.g = server;
    }

    /**
     * Initializes the game session for the engine.
     *
     * Behavior:
     * The server initializes its default level.
     * The cyclic flag is synchronized with the engine request.
     *
     * Parameters:
     * level, mapStr, seed, ghostSpeed, dt, and something are currently ignored by this adapter.
     *
     * @return "OK" on success
     */
    @Override
    public String init(int level, String mapStr, boolean cyclic, long seed, double ghostSpeed, int dt, int something) {
        g.initDefaultLevel();
        if (g.isCyclic() != cyclic) g.toggleCyclic();
        return "OK";
    }

    /**
     * Engine hook for starting gameplay.
     *
     * This adapter does not need to run its own loop.
     * The engine advances the game by calling move repeatedly.
     */
    @Override
    public void play() { }

    /**
     * Executes one engine step.
     *
     * Step definition:
     * Move Pac-Man by direction.
     * Advance the server simulation by one tick.
     *
     * @param dir direction code provided by the engine
     * @return a compact textual snapshot produced by getData(0)
     */
    @Override
    public String move(int dir) {
        // one step: pac move + tick
        g.movePacByDir(dir);
        g.tick();
        return getData(0);
    }

    /**
     * Ends the session.
     *
     * @param code engine-specific code (not used)
     * @return "DONE" after server cleanup
     */
    @Override
    public String end(int code) {
        g.quit();
        return "DONE";
    }

    /**
     * Returns a compact textual snapshot of the current game state.
     *
     * Current format includes:
     * score and remaining pink pellets.
     *
     * @param code engine-specific code (not used)
     * @return snapshot string for debugging and compatibility
     */
    @Override
    public String getData(int code) {
        return "Score=" + g.getScore() + " PinkLeft=" + g.getPinkLeft();
    }

    /**
     * Returns the board matrix as expected by the engine.
     *
     * @param code engine-specific code (not used)
     * @return the server board
     */
    @Override
    public int[][] getGame(int code) {
        return g.getBoard();
    }

    /**
     * Returns Pac-Man position formatted as x,y.
     *
     * @param code engine-specific code (not used)
     * @return position string x,y
     */
    @Override
    public String getPos(int code) {
        return g.getPacX() + "," + g.getPacY();
    }

    /**
     * Returns ghosts adapted to the engine GhostCL interface.
     *
     * Behavior:
     * Each server ghost is wrapped by SimpleGhostCL.
     * If the server returns null, an empty array is returned.
     *
     * @param code engine-specific code (not used)
     * @return array of GhostCL objects, never null
     */
    @Override
    public GhostCL[] getGhosts(int code) {
        MyGameServer.Ghost[] gs = g.getGhosts();
        if (gs == null) return new GhostCL[0];

        GhostCL[] out = new GhostCL[gs.length];
        for (int i = 0; i < gs.length; i++) out[i] = new SimpleGhostCL(gs[i]);
        return out;
    }

    /**
     * Maps server status to engine status constants.
     *
     * @return INIT, PLAY, or DONE
     */
    @Override
    public int getStatus() {
        int s = g.getStatus();
        if (s == MyGameServer.PLAY) return PLAY;
        if (s == MyGameServer.DONE) return DONE;
        return INIT;
    }

    /**
     * @return true if the server board is cyclic
     */
    @Override
    public boolean isCyclic() {
        return g.isCyclic();
    }

    /**
     * Optional engine hook.
     * This adapter does not use key characters because movement is driven by move(dir).
     *
     * @return null
     */
    @Override
    public Character getKeyChar() { return null; }

    /**
     * Minimal GhostCL wrapper around a server ghost.
     *
     * OVERVIEW
     * --------
     * The engine expects GhostCL objects.
     * The server uses its own Ghost structure.
     * This wrapper exposes only the fields and behaviors that the engine needs:
     * type, position, info string, eatable timer, and status.
     *
     * STATUS POLICY
     * -------------
     * If the ghost is released, status is PLAY.
     * Otherwise, status is INIT.
     */
    private static class SimpleGhostCL implements GhostCL {
        private final MyGameServer.Ghost gg;

        SimpleGhostCL(MyGameServer.Ghost gg) {
            this.gg = gg;
        }

        /**
         * Returns a generic engine ghost type.
         *
         * @return RANDOM_WALK1
         */
        @Override public int getType() { return RANDOM_WALK1; }

        /**
         * @return position formatted as x,y
         */
        @Override public String getPos(int code) { return gg.x + "," + gg.y; }

        /**
         * @return short info string for debugging and overlays
         */
        @Override public String getInfo() { return "MyGhost"; }

        /**
         * Returns the remaining eatable time in seconds.
         *
         * Behavior:
         * If the ghost is not eatable, return -1.0.
         * Otherwise return the remaining seconds until eatableUntilMs.
         *
         * @return seconds remaining, or -1.0 if not eatable
         */
        @Override
        public double remainTimeAsEatable(int code) {
            long now = System.currentTimeMillis();
            long msLeft = gg.eatableUntilMs - now;
            if (msLeft <= 0) return -1.0;
            return msLeft / 1000.0;
        }

        /**
         * @return PLAY if released, otherwise INIT
         */
        @Override
        public int getStatus() {
            return gg.released ? PLAY : INIT;
        }
    }
}
