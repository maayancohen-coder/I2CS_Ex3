package MyGame.adapters;

import MyGame.server.MyGameServer;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacmanGame;

public class MyPacmanGameAdapter implements PacmanGame {

    private final MyGameServer g;

    public MyPacmanGameAdapter(MyGameServer server) {
        this.g = server;
    }

    @Override
    public String init(int level, String mapStr, boolean cyclic, long seed, double ghostSpeed, int dt, int something) {
        g.initDefaultLevel();
        if (g.isCyclic() != cyclic) g.toggleCyclic();
        return "OK";
    }

    @Override
    public void play() { }

    @Override
    public String move(int dir) {
        // one step: pac move + tick
        g.movePacByDir(dir);
        g.tick();
        return getData(0);
    }

    @Override
    public String end(int code) {
        g.quit();
        return "DONE";
    }

    @Override
    public String getData(int code) {
        return "Score=" + g.getScore() + " PinkLeft=" + g.getPinkLeft();
    }

    @Override
    public int[][] getGame(int code) {
        return g.getBoard();
    }

    @Override
    public String getPos(int code) {
        return g.getPacX() + "," + g.getPacY();
    }

    @Override
    public GhostCL[] getGhosts(int code) {
        MyGameServer.Ghost[] gs = g.getGhosts();
        if (gs == null) return new GhostCL[0];

        GhostCL[] out = new GhostCL[gs.length];
        for (int i = 0; i < gs.length; i++) out[i] = new SimpleGhostCL(gs[i]);
        return out;
    }

    @Override
    public int getStatus() {
        int s = g.getStatus();
        if (s == MyGameServer.PLAY) return PLAY;
        if (s == MyGameServer.DONE) return DONE;
        return INIT;
    }

    @Override
    public boolean isCyclic() {
        return g.isCyclic();
    }

    @Override
    public Character getKeyChar() { return null; }

    private static class SimpleGhostCL implements GhostCL {
        private final MyGameServer.Ghost gg;

        SimpleGhostCL(MyGameServer.Ghost gg) {
            this.gg = gg;
        }

        @Override public int getType() { return RANDOM_WALK1; }

        @Override public String getPos(int code) { return gg.x + "," + gg.y; }

        @Override public String getInfo() { return "MyGhost"; }

        @Override
        public double remainTimeAsEatable(int code) {
            long now = System.currentTimeMillis();
            long msLeft = gg.eatableUntilMs - now;
            if (msLeft <= 0) return -1.0;
            return msLeft / 1000.0;
        }

        @Override
        public int getStatus() {
            return gg.released ? PLAY : INIT;
        }
    }
}
