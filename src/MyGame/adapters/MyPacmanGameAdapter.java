package MyGame.adapters;

import MyGame.server.MyGameServer;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacmanGame;

/**
 * Adapter between {@link MyGameServer} (your server-side implementation)
 * and the course engine interface {@link PacmanGame}.
 *
 * <p>This class allows the external "Ex3" runner / GUI / algorithm tester
 * (which expects a {@code PacmanGame}) to operate on your own game server
 * without changing the server code.</p>
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li><b>Thin wrapper:</b> Most methods delegate directly to {@link MyGameServer}.</li>
 *   <li><b>Single-step move:</b> {@link #move(int)} performs one Pac-Man step and then advances the world by one tick.</li>
 *   <li><b>Ghost bridging:</b> Server ghosts are wrapped as {@link GhostCL} objects via {@link SimpleGhostCL}.</li>
 * </ul>
 */
public class MyPacmanGameAdapter implements PacmanGame {

    /**
     * Underlying game server that contains the real game state and logic.
     */
    private final MyGameServer g;

    /**
     * Creates a new adapter for the given server instance.
     *
     * @param server the server to adapt (must not be {@code null})
     */
    public MyPacmanGameAdapter(MyGameServer server) {
        this.g = server;
    }

    /**
     * Initializes the game for the external engine.
     *
     * <p>In this adapter, {@code level}, {@code mapStr}, {@code seed},
     * {@code ghostSpeed}, {@code dt}, and {@code something} are currently ignored
     * because the server is responsible for building the default level on its own.</p>
     *
     * <p>The only parameter that is actively respected is {@code cyclic}:
     * if the engine requests cyclic behavior and the server differs,
     * the adapter toggles it to match.</p>
     *
     * @param level       requested level id (currently ignored)
     * @param mapStr      requested map representation (currently ignored)
     * @param cyclic      whether the map should be cyclic (tunnels wrap around)
     * @param seed        random seed (currently ignored)
     * @param ghostSpeed  ghost speed factor (currently ignored)
     * @param dt          engine tick time in ms (currently ignored)
     * @param something   extra engine parameter (currently ignored)
     * @return "OK" if initialization succeeded
     */
    @Override
    public String init(int level, String mapStr, boolean cyclic, long seed, double ghostSpeed, int dt, int something) {
        g.initDefaultLevel();
        if (g.isCyclic() != cyclic) g.toggleCyclic();
        return "OK";
    }

    /**
     * Starts the game loop (engine hook).
     *
     * <p>This adapter does not need to start a separate thread or loop,
     * because the engine drives the game by repeatedly calling {@link #move(int)}.</p>
     */
    @Override
    public void play() { }

    /**
     * Performs one engine step:
     * <ol>
     *   <li>Moves Pac-Man one step in the requested direction.</li>
     *   <li>Advances the server simulation by one {@code tick} (ghost movement, collisions, scoring, etc.).</li>
     * </ol>
     *
     * @param dir direction code as defined by the course engine
     * @return current game snapshot string (delegated to {@link #getData(int)})
     */
    @Override
    public String move(int dir) {
        // one step: pac move + tick
        g.movePacByDir(dir);
        g.tick();
        return getData(0);
    }

    /**
     * Ends the game session.
     *
     * @param code engine-specific code (ignored)
     * @return "DONE" after cleanup
     */
    @Override
    public String end(int code) {
        g.quit();
        return "DONE";
    }

    /**
     * Returns a compact textual snapshot of the game state.
     *
     * <p>Format is intentionally simple for debugging / engine compatibility.</p>
     *
     * @param code engine-specific code (ignored)
     * @return string containing score and remaining pellets (pink)
     */
    @Override
    public String getData(int code) {
        return "Score=" + g.getScore() + " PinkLeft=" + g.getPinkLeft();
    }

    /**
     * Returns the board matrix as expected by the engine.
     *
     * <p>The returned matrix is the server's live board reference.
     * If your engine/GUI ever mutates it, consider returning a deep copy instead.</p>
     *
     * @param code engine-specific code (ignored)
     * @return board as {@code int[][]}
     */
    @Override
    public int[][] getGame(int code) {
        return g.getBoard();
    }

    /**
     * Returns Pac-Man position in the engine's "x,y" string format.
     *
     * @param code engine-specific code (ignored)
     * @return "x,y"
     */
    @Override
    public String getPos(int code) {
        return g.getPacX() + "," + g.getPacY();
    }

    /**
     * Returns all ghosts adapted to {@link GhostCL}.
     *
     * <p>Each server ghost is wrapped by {@link SimpleGhostCL} so the engine
     * can read ghost position/status without knowing server internals.</p>
     *
     * @param code engine-specific code (ignored)
     * @return array of ghosts, never {@code null}
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
     * Maps the server status to the engine status constants ({@link #INIT}, {@link #PLAY}, {@link #DONE}).
     *
     * @return engine-compatible status
     */
    @Override
    public int getStatus() {
        int s = g.getStatus();
        if (s == MyGameServer.PLAY) return PLAY;
        if (s == MyGameServer.DONE) return DONE;
        return INIT;
    }

    /**
     * Indicates whether the game board is cyclic (wrap-around tunnels).
     *
     * @return {@code true} if cyclic
     */
    @Override
    public boolean isCyclic() {
        return g.isCyclic();
    }

    /**
     * Optional engine hook for keyboard input as a character.
     *
     * <p>This adapter does not rely on this method because movement is driven
     * through {@link #move(int)} with direction codes.</p>
     *
     * @return {@code null} (not used)
     */
    @Override
    public Character getKeyChar() { return null; }

    /**
     * Minimal {@link GhostCL} implementation that exposes a server ghost to the engine.
     *
     * <p>Only the subset of methods used by the course engine/GUI are implemented.</p>
     */
    private static class SimpleGhostCL implements GhostCL {

        /**
         * Underlying server ghost (position, timers, released flag).
         */
        private final MyGameServer.Ghost gg;

        /**
         * @param gg server ghost instance to wrap
         */
        SimpleGhostCL(MyGameServer.Ghost gg) {
            this.gg = gg;
        }

        /**
         * Returns the ghost type expected by the engine.
         *
         * <p>Currently always returns {@link #RANDOM_WALK1} as a generic type.</p>
         *
         * @return engine ghost type constant
         */
        @Override public int getType() { return RANDOM_WALK1; }

        /**
         * Returns ghost position in the engine's "x,y" string format.
         *
         * @param code engine-specific code (ignored)
         * @return "x,y"
         */
        @Override public String getPos(int code) { return gg.x + "," + gg.y; }

        /**
         * Human-readable info for debugging or GUI overlays.
         *
         * @return a short info string
         */
        @Override public String getInfo() { return "MyGhost"; }

        /**
         * Returns remaining time (in seconds) the ghost is eatable.
         *
         * <p>If the ghost is not eatable, returns {@code -1.0} (engine convention).</p>
         *
         * @param code engine-specific code (ignored)
         * @return seconds remaining, or {@code -1.0} if not eatable
         */
        @Override
        public double remainTimeAsEatable(int code) {
            long now = System.currentTimeMillis();
            long msLeft = gg.eatableUntilMs - now;
            if (msLeft <= 0) return -1.0;
            return msLeft / 1000.0;
        }

        /**
         * Returns the ghost status for the engine.
         *
         * <p>Maps {@code released} to {@link #PLAY}, otherwise {@link #INIT}.</p>
         *
         * @return engine-compatible status
         */
        @Override
        public int getStatus() {
            return gg.released ? PLAY : INIT;
        }
    }
}
