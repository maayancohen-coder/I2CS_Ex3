package assignments;

import exe.ex3.game.*;
import java.awt.*;
import java.util.*;

public class Ex3Algo implements PacManAlgo {

    private int step = 0;
    private int BLUE, PINK, GREEN;

    private int lastDir = -1;
    private Pixel2D lastPos = null;
    private Pixel2D lastPos2 = null; // נגד ABAB

    // Tuning for hard levels (dt=200)
    private static final int DANGER_RADIUS = 7;
    private static final int ESCAPE_TRIGGER = 4;      // מתי נכנסים למצב בריחה
    private static final int SAFE_SPACE_LIMIT = 18;   // יותר שמרני ברמה קשה
    private static final int EDIBLE_TIME_BUFFER = 2;

    @Override
    public String getInfo() {
        return "Pink-first + DangerMap + EscapeMode + PoweredMode (dt200-ready)";
    }

    @Override
    public int move(PacmanGame game) {
        int code = 0;

        int[][] board = game.getGame(code);
        Map map = new Map(board);
        map.setCyclic(GameInfo.CYCLIC_MODE);

        if (step == 0) {
            BLUE  = Game.getIntColor(Color.BLUE, 0);
            PINK  = Game.getIntColor(Color.PINK, 0);
            GREEN = Game.getIntColor(Color.GREEN, 0);
        }

        Pixel2D me = parsePos(game.getPos(code));
        GhostCL[] ghosts = game.getGhosts(code);

        // danger[x][y] = min maze-distance from any "danger" ghost to cell (x,y)
        double[][] danger = buildDangerMap(map, board, ghosts);

        double curDanger = danger[me.getX()][me.getY()];
        boolean powered = isPowered(ghosts);
        boolean escapeMode = (!powered && curDanger <= ESCAPE_TRIGGER);

        int bestDir = -1;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int dir : new int[]{Game.UP, Game.DOWN, Game.LEFT, Game.RIGHT}) {
            Pixel2D next = neighbor(me, dir, map);
            if (!isLegal(next, board)) continue;

            double nextDanger = danger[next.getX()][next.getY()];

            // --- HARD SAFETY FILTERS (dt=200) ---
            if (!powered) {
                if (nextDanger <= 1) continue;           // אל תיכנס למוות ודאי
                if (escapeMode && nextDanger <= 2) continue; // במצב בריחה — עוד יותר קשוח
            }

            double score = evaluate(next, map, board, danger, ghosts, powered);

            // lookahead (כמו “חבר” אבל עם הגיון בטיחות)
            Map2D d2 = map.allDistance(next, BLUE);
            score += 0.5 * futurePinkScore(d2, board, danger);

            // anti-oscillation
            if (dir == lastDir) score += (escapeMode ? 10 : 120);
            if (lastDir != -1 && dir == opposite(lastDir)) score -= (escapeMode ? 600 : 160);

            if (lastPos2 != null && next.getX() == lastPos2.getX() && next.getY() == lastPos2.getY()) {
                score -= 500;
            }

            // --- ESCAPE MODE: dominate by increasing distance from danger ---
            if (escapeMode) {
                // רוצה למקסם nextDanger (כלומר להתרחק מהרוח)
                score += nextDanger * 120000;
            } else {
                // גם לא בבריחה: תעניש תנועה שמקרבת לרוח כשקרובים
                if (!powered && nextDanger < curDanger && curDanger <= DANGER_RADIUS) {
                    score -= (curDanger - nextDanger) * 35000;
                }
            }

            // עוד שכבת בטיחות כללית
            if (!powered && nextDanger < 6) score -= (6 - nextDanger) * 8000;

            if (score > bestScore) {
                bestScore = score;
                bestDir = dir;
            }
        }

        // fallback: תמיד לבחור כיוון חוקי
        if (bestDir == -1) bestDir = firstLegalDir(me, board, map);

        // update memory
        lastPos2 = lastPos;
        lastPos = me;
        lastDir = bestDir;
        step++;

        return bestDir;
    }

    /* =========================
       ========== DANGER MAP =====
       ========================= */

    private double[][] buildDangerMap(Map map, int[][] board, GhostCL[] ghosts) {
        int w = board.length, h = board[0].length;
        double[][] danger = new double[w][h];
        for (double[] r : danger) Arrays.fill(r, Double.POSITIVE_INFINITY);

        for (GhostCL g : ghosts) {
            if (g.getStatus() == 0) continue;

            // מסוכן אם לא אכיל (שלילי) + גם שמרנות אם הטיימר קטן (<=2)
            double t = g.remainTimeAsEatable(0);
            boolean dangerous = (t < 0) || (t <= 2);

            if (!dangerous) continue;

            Pixel2D gp = parsePos(g.getPos(0).toString());
            Map2D dist = map.allDistance(gp, BLUE);

            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    int d = dist.getPixel(x, y);
                    if (d != -1) danger[x][y] = Math.min(danger[x][y], d);
                }
            }
        }
        return danger;
    }

    private boolean isPowered(GhostCL[] ghosts) {
        for (GhostCL g : ghosts) {
            if (g.getStatus() != 0 && g.remainTimeAsEatable(0) > 0) return true;
        }
        return false;
    }

    /* =========================
       ========== EVALUATE =======
       ========================= */

    private double evaluate(Pixel2D pos, Map map, int[][] board, double[][] danger, GhostCL[] ghosts, boolean powered) {
        int x = pos.getX(), y = pos.getY();
        double ghostDist = danger[x][y];

        // hard death avoidance (when not powered)
        if (!powered && ghostDist <= 1) return -1e12;

        double score = 0;

        // 1) Safe space (anti trap)
        int safeSpace = countSafeSpace(pos, map, board, danger, SAFE_SPACE_LIMIT);
        score += safeSpace * 900;

        // 2) PINK is top priority
        Map2D distMap = map.allDistance(pos, BLUE);
        Pixel2D pink = closest(board, distMap, PINK);
        if (pink == null) return 1e12; // win

        int dPink = distMap.getPixel(pink.getX(), pink.getY());
        score += 260000.0 / (dPink + 1);
        if (board[x][y] == PINK) score += 140000;

        // 3) GREEN only when actually threatened (and not powered)
        if (!powered && ghostDist <= DANGER_RADIUS) {
            Pixel2D g = closest(board, distMap, GREEN);
            if (g != null) {
                int dGreen = distMap.getPixel(g.getX(), g.getY());
                if (dGreen != -1 && dGreen <= 4) { // רק ירוק “מציל חיים”
                    score += 160000.0 / (dGreen + 1);
                    if (board[x][y] == GREEN) score += 220000;
                }
            }
        }

        // 4) If powered: can eat ghosts, but do NOT sacrifice pink mission
        if (powered) {
            GhostCL edible = bestEdibleReachable(distMap, ghosts);
            if (edible != null) {
                Pixel2D gp = parsePos(edible.getPos(0).toString());
                int dg = distMap.getPixel(gp);
                if (dg != -1 && dg <= 8) { // לא לרדוף אם רחוק מדי
                    score += 90000.0 / (dg + 1);
                }
            }
        }

        // 5) Extra penalty when close (not powered)
        if (!powered && ghostDist < 6) score -= (6 - ghostDist) * 24000;

        return score;
    }

    private GhostCL bestEdibleReachable(Map2D distFromPos, GhostCL[] ghosts) {
        GhostCL best = null;
        int bestD = Integer.MAX_VALUE;

        for (GhostCL g : ghosts) {
            if (g.getStatus() == 0) continue;
            double t = g.remainTimeAsEatable(0);
            if (t <= 0) continue;

            Pixel2D gp = parsePos(g.getPos(0).toString());
            int d = distFromPos.getPixel(gp);
            if (d == -1) continue;

            if (d + EDIBLE_TIME_BUFFER <= t && d < bestD) {
                bestD = d;
                best = g;
            }
        }
        return best;
    }

    /* =========================
       ========== FUTURE SCORE ===
       ========================= */

    private double futurePinkScore(Map2D dist, int[][] board, double[][] danger) {
        double best = 0;
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                if (board[i][j] == PINK) {
                    int d = dist.getPixel(i, j);
                    if (d != -1 && danger[i][j] > d + 2) {
                        best = Math.max(best, 180000.0 / (d + 1));
                    }
                }
            }
        }
        return best;
    }

    /* =========================
       ========== SAFE SPACE =====
       ========================= */

    private int countSafeSpace(Pixel2D start, Map map, int[][] board, double[][] danger, int limit) {
        Queue<Pixel2D> q = new LinkedList<>();
        HashMap<String, Integer> dist = new HashMap<>();

        q.add(start);
        dist.put(key(start), 0);
        int count = 0;

        while (!q.isEmpty() && count < limit) {
            Pixel2D cur = q.poll();
            int d = dist.get(key(cur));
            count++;

            for (int dir : new int[]{Game.UP, Game.DOWN, Game.LEFT, Game.RIGHT}) {
                Pixel2D n = neighbor(cur, dir, map);
                String k = key(n);

                if (!isLegal(n, board) || dist.containsKey(k)) continue;

                // avoid cells ghosts can reach "soon"
                if (danger[n.getX()][n.getY()] <= d + 2) continue;

                dist.put(k, d + 1);
                q.add(n);
            }
        }
        return count;
    }

    /* =========================
       ========== HELPERS ========
       ========================= */

    private Pixel2D closest(int[][] board, Map2D dist, int color) {
        Pixel2D best = null;
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                if (board[i][j] == color) {
                    int d = dist.getPixel(i, j);
                    if (d != -1 && d < min) {
                        min = d;
                        best = new Index2D(i, j);
                    }
                }
            }
        }
        return best;
    }

    // אצלכם: UP=y+1, DOWN=y-1
    private Pixel2D neighbor(Pixel2D p, int dir, Map map) {
        int x = p.getX(), y = p.getY();
        if (dir == Game.UP) y++;
        if (dir == Game.DOWN) y--;
        if (dir == Game.LEFT) x--;
        if (dir == Game.RIGHT) x++;

        int w = map.getMap().length, h = map.getMap()[0].length;
        return new Index2D((x + w) % w, (y + h) % h);
    }

    private boolean isLegal(Pixel2D p, int[][] board) {
        return board[p.getX()][p.getY()] != BLUE && !isGhostHouse(p, board);
    }

    private boolean isGhostHouse(Pixel2D p, int[][] board) {
        int mx = board.length / 2, my = board[0].length / 2;
        return Math.abs(p.getX() - mx) < 3 &&
                Math.abs(p.getY() - my) < 3 &&
                board[p.getX()][p.getY()] == 0;
    }

    private String key(Pixel2D p) { return p.getX() + "," + p.getY(); }

    private int opposite(int dir) {
        if (dir == Game.UP) return Game.DOWN;
        if (dir == Game.DOWN) return Game.UP;
        if (dir == Game.LEFT) return Game.RIGHT;
        if (dir == Game.RIGHT) return Game.LEFT;
        return -1;
    }

    private int firstLegalDir(Pixel2D me, int[][] board, Map map) {
        for (int dir : new int[]{Game.UP, Game.DOWN, Game.LEFT, Game.RIGHT}) {
            Pixel2D next = neighbor(me, dir, map);
            if (isLegal(next, board)) return dir;
        }
        return Game.LEFT;
    }

    private static Pixel2D parsePos(String s) {
        String[] p = s.trim().split(",");
        int x = Integer.parseInt(p[0].trim());
        int y = Integer.parseInt(p[1].trim());
        return new Index2D(x, y);
    }
}
