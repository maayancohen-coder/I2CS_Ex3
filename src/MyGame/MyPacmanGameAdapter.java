package MyGame;

import exe.ex3.game.GhostCL;
import exe.ex3.game.PacmanGame;

public class MyPacmanGameAdapter implements PacmanGame {

    private final MyGame g;

    public MyPacmanGameAdapter(MyGame g) {
        this.g = g;
    }

    @Override
    public Character getKeyChar() {
        return null;
    }

    @Override
    public String getPos(int code) {
        return g.getPacX() + "," + g.getPacY();
    }

    @Override
    public GhostCL[] getGhosts(int code) {
        MyGame.Ghost[] gs = g.getGhosts();
        if (gs == null) return new GhostCL[0];

        int count = 0;
        for (MyGame.Ghost x : gs) {
            if (x != null && x.released) count++;
        }

        GhostCL[] out = new GhostCL[count];
        int i = 0;
        for (MyGame.Ghost x : gs) {
            if (x == null || !x.released) continue;
            out[i++] = new SimpleGhostCL(x);
        }
        return out;
    }

    @Override
    public int[][] getGame(int code) {
        return g.getBoard();
    }

    @Override
    public String move(int dir) {
        g.movePacByDir(dir);
        return "OK";
    }

    @Override public void play() {}
    @Override public String end(int code) { return "END"; }
    @Override public String getData(int code) { return ""; }

    @Override
    public int getStatus() {
        return (g.getStatus() == MyGame.PLAY) ? PacmanGame.PLAY : PacmanGame.DONE;
    }

    @Override
    public boolean isCyclic() {
        return g.isCyclic();
    }

    @Override
    public String init(int id, String level, boolean cyclic, long seed, double ghostSpeed, int dt, int something) {
        return "OK";
    }

    /** Minimal GhostCL wrapper */
    private static class SimpleGhostCL implements GhostCL {
        private final MyGame.Ghost gg;
        SimpleGhostCL(MyGame.Ghost gg) { this.gg = gg; }

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

        @Override public int getStatus() { return PLAY; }
    }
}
