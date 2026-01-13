package MyGame;

import assignments.Ex3Algo;
import assignments.StdDraw;

import java.awt.event.KeyEvent;

public class MyMain {
    public static void main(String[] args) {

        int cell = 36;
        int hud  = 40;

        MyGame game = new MyGame();
        game.initDefaultLevel();

        MyGameUI ui = new MyGameUI(cell, hud);
        ui.initCanvas(game.getBoard().length, game.getBoard()[0].length);

        Ex3Algo algo = new Ex3Algo();
        MyPacmanGameAdapter adapter = new MyPacmanGameAdapter(game);

        int autoStepMs = 140;

        boolean prevW=false, prevA=false, prevS=false, prevD=false;
        boolean prevSpace=false, prevC=false, prevQ=false, prevM=false, prevEnter=false;

        long lastAutoMove = 0;

        while (game.getStatus() == MyGame.PLAY) {

            boolean w = StdDraw.isKeyPressed(KeyEvent.VK_W);
            boolean a = StdDraw.isKeyPressed(KeyEvent.VK_A);
            boolean s = StdDraw.isKeyPressed(KeyEvent.VK_S);
            boolean d = StdDraw.isKeyPressed(KeyEvent.VK_D);

            boolean space = StdDraw.isKeyPressed(KeyEvent.VK_SPACE);
            boolean c     = StdDraw.isKeyPressed(KeyEvent.VK_C);
            boolean q     = StdDraw.isKeyPressed(KeyEvent.VK_Q);
            boolean m     = StdDraw.isKeyPressed(KeyEvent.VK_M);
            boolean enter = StdDraw.isKeyPressed(KeyEvent.VK_ENTER);

            if (w && !prevW) game.handleKeyOnce('w');
            if (a && !prevA) game.handleKeyOnce('a');
            if (s && !prevS) game.handleKeyOnce('s');
            if (d && !prevD) game.handleKeyOnce('d');

            if (space && !prevSpace) game.handleKeyOnce(' ');
            if (c && !prevC)         game.handleKeyOnce('c');
            if (m && !prevM)         game.handleKeyOnce('m');
            if (q && !prevQ)         game.handleKeyOnce('q');

            // ENTER = צעד אחד של האלגוריתם
            if (enter && !prevEnter && !game.isPaused()) {
                int dir = algo.move(adapter);
                game.movePacByDir(dir);
            }

            prevW=w; prevA=a; prevS=s; prevD=d;
            prevSpace=space; prevC=c; prevQ=q; prevM=m; prevEnter=enter;

            // AUTO = צעד אלגוריתם כל autoStepMs
            if (!game.isPaused() && game.getControlMode() == MyGame.ControlMode.AUTO) {
                long now = System.currentTimeMillis();
                if (now - lastAutoMove >= autoStepMs) {
                    lastAutoMove = now;
                    int dir = algo.move(adapter);
                    game.movePacByDir(dir);
                }
            }

            game.tick();

            // UI שלך מצפה pacDir כ-int של MyGame.LEFT/RIGHT/UP/DOWN
            int pacDirInt = MyGame.LEFT;
            Direction pd = game.getPacDir();
            if (pd == Direction.RIGHT) pacDirInt = MyGame.RIGHT;
            else if (pd == Direction.UP) pacDirInt = MyGame.UP;
            else if (pd == Direction.DOWN) pacDirInt = MyGame.DOWN;

            ui.draw(game.getBoard(), game.getPacX(), game.getPacY(), pacDirInt,
                    game.getGhosts(), game.hudLine());

            StdDraw.pause(20);
        }

        ui.drawEndScreen(game.isWon());

        while (true) {
            if (StdDraw.isKeyPressed(KeyEvent.VK_Q)) break;
            if (StdDraw.hasNextKeyTyped()) {
                char ch = Character.toLowerCase(StdDraw.nextKeyTyped());
                if (ch == 'q') break;
            }
            StdDraw.pause(30);
        }
    }
}
