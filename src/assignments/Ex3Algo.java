package assignments;

import exe.ex3.game.*;
import java.awt.*;
import java.util.*;

/**
 * Pac-Man decision algorithm.
 *
 * OVERVIEW
 * --------
 * This class implements a defensive, pellet-focused Pac-Man strategy designed to work well
 * on harder settings with slower update rates.
 *
 * The algorithm combines:
 * Pink-first navigation toward the nearest pink pellet
 * A danger map that estimates how close dangerous ghosts can get to each cell
 * Escape mode when a ghost is too close and Pac-Man is not powered
 * Powered mode behavior when ghosts are currently eatable
 * Anti-oscillation memory to reduce back-and-forth movement
 *
 * HIGH-LEVEL BEHAVIOR
 * -------------------
 * 1. Read the board, Pac-Man position, and ghosts.
 * 2. Build a danger map:
 *    For every cell, store the minimum maze distance from any dangerous ghost.
 * 3. Decide whether Pac-Man is powered and whether escape mode should activate.
 * 4. Score each candidate move (up, down, left, right) using safety and objective terms.
 * 5. Pick the move with the highest score, with a legal fallback if needed.
 *
 * MODES
 * -----
 * Powered:
 * A ghost is considered eatable if remainTimeAsEatable is positive.
 * In powered mode, the algorithm may approach nearby eatable ghosts but does not abandon the pink mission.
 *
 * Escape mode:
 * If not powered and the current danger distance is at or below ESCAPE_TRIGGER,
 * the algorithm strongly prefers moves that increase distance from danger.
 *
 * SAFETY AND TRAP AVOIDANCE
 * ------------------------
 * Hard safety filters reject moves that lead into immediate danger when not powered.
 * A safe-space BFS counts how many safe cells are reachable from a candidate position
 * before hitting a limit, discouraging dead ends and tight traps.
 *
 * MEMORY AND STABILITY
 * --------------------
 * lastDir favors continuing the same direction when reasonable.
 * A strong penalty discourages immediate reversal.
 * lastPos and lastPos2 reduce ABAB oscillations by penalizing returning to the position two steps ago.
 *
 * COORDINATES AND MOVEMENT
 * ------------------------
 * Board indexing is board[x][y].
 * Movement uses a neighbor function with wrap-around behavior.
 * Legality checks exclude walls and the ghost house region.
 */
public class Ex3Algo implements PacManAlgo {

    private int step = 0;
    private int BLUE, PINK, GREEN;

    private int lastDir = -1;
    private Pixel2D lastPos = null;
    private Pixel2D lastPos2 = null; // anti ABAB

    // Tuning for hard levels (dt=200)
    private static final int DANGER_RADIUS = 7;
    private static final int ESCAPE_TRIGGER = 4;      // enter escape mode threshold
    private static final int SAFE_SPACE_LIMIT = 18;   // conservative on hard levels
    private static final int EDIBLE_TIME_BUFFER = 2;

    /**
     * Returns a short description of the algorithm.
     *
     * @return algorithm summary string
     */
    @Override
    public String getInfo() {
        return "Pink-first + DangerMap + EscapeMode + PoweredMode (dt200-ready)";
    }

    /**
     * Selects the next move direction for Pac-Man.
     *
     * Decision pipeline:
     * Read board and entity state.
     * Build the danger map from ghosts.
     * Determine powered state and escape mode.
     * Evaluate all legal directions and return the best.
     *
     * @param game engine game interface
     * @return one of Game.UP, Game.DOWN, Game.LEFT, Game.RIGHT
     */
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

        // danger[x][y] is the minimum maze-distance from any dangerous ghost to cell (x,y)
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

            // Hard safety filters
            if (!powered) {
                if (nextDanger <= 1) continue;
                if (escapeMode && nextDanger <= 2) continue;
            }

            double score = evaluate(next, map, board, danger, ghosts, powered);

            // Lookahead: estimate future pink opportunities from the next cell
            Map2D d2 = map.allDistance(next, BLUE);
            score += 0.5 * futurePinkScore(d2, board, danger);

            // Anti-oscillation and stability
            if (dir == lastDir) score += (escapeMode ? 10 : 120);
            if (lastDir != -1 && dir == opposite(lastDir)) score -= (escapeMode ? 600 : 160);

            if (lastPos2 != null && next.getX() == lastPos2.getX() && next.getY() == lastPos2.getY()) {
                score -= 500;
            }

            // Escape mode: strongly prioritize increasing distance from danger
            if (escapeMode) {
                score += nextDanger * 120000;
            } else {
                // If close to danger, penalize moves that reduce the danger distance
                if (!powered && nextDanger < curDanger && curDanger <= DANGER_RADIUS) {
                    score -= (curDanger - nextDanger) * 35000;
                }
            }

            // Additional safety penalty near danger
            if (!powered && nextDanger < 6) score -= (6 - nextDanger) * 8000;

            if (score > bestScore) {
                bestScore = score;
                bestDir = dir;
            }
        }

        // Fallback: always return a legal direction
        if (bestDir == -1) bestDir = firstLegalDir(me, board, map);

        // Update memory
        lastPos2 = lastPos;
        lastPos = me;
        lastDir = bestDir;
        step++;

        return bestDir;
    }

    /* =========================
       ========== DANGER MAP =====
       ========================= */

    /**
     * Builds a danger map where each cell stores the minimum maze distance from any dangerous ghost.
     *
     * A ghost is considered dangerous if:
     * remainTimeAsEatable is negative, meaning it is not eatable, or
     * remainTimeAsEatable is very small, treated conservatively as still dangerous
     *
     * @param map    maze helper for distance computation
     * @param board  board tile matrix
     * @param ghosts ghosts reported by the engine
     * @return danger map with distances, infinity if no dangerous ghost can reach the cell
     */
    private double[][] buildDangerMap(Map map, int[][] board, GhostCL[] ghosts) {
        int w = board.length, h = board[0].length;
        double[][] danger = new double[w][h];
        for (double[] r : danger) Arrays.fill(r, Double.POSITIVE_INFINITY);

        for (GhostCL g : ghosts) {
            if (g.getStatus() == 0) continue;

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

    /**
     * Detects whether Pac-Man is currently powered.
     *
     * @param ghosts ghosts array
     * @return true if at least one active ghost has positive eatable time remaining
     */
    private boolean isPowered(GhostCL[] ghosts) {
        for (GhostCL g : ghosts) {
            if (g.getStatus() != 0 && g.remainTimeAsEatable(0) > 0) return true;
        }
        return false;
    }

    /* =========================
       ========== EVALUATE =======
       ========================= */

    /**
     * Scores a candidate next position.
     *
     * Major components:
     * Safe-space reachability score (anti-trap)
     * Pink pellet objective (primary goal)
     * Green pellet objective (only when threatened and not powered)
     * Optional edible ghost pursuit in powered mode (secondary)
     * Extra penalty when near danger (not powered)
     *
     * @param pos     candidate next position
     * @param map     maze helper
     * @param board   board matrix
     * @param danger  danger map
     * @param ghosts  ghosts array
     * @param powered current powered state
     * @return score, higher is better
     */
    private double evaluate(Pixel2D pos, Map map, int[][] board, double[][] danger, GhostCL[] ghosts, boolean powered) {
        int x = pos.getX(), y = pos.getY();
        double ghostDist = danger[x][y];

        // Hard avoidance of immediate death when not powered
        if (!powered && ghostDist <= 1) return -1e12;

        double score = 0;

        // 1) Safe space (anti-trap)
        int safeSpace = countSafeSpace(pos, map, board, danger, SAFE_SPACE_LIMIT);
        score += safeSpace * 900;

        // 2) Pink is top priority
        Map2D distMap = map.allDistance(pos, BLUE);
        Pixel2D pink = closest(board, distMap, PINK);
        if (pink == null) return 1e12;

        int dPink = distMap.getPixel(pink.getX(), pink.getY());
        score += 260000.0 / (dPink + 1);
        if (board[x][y] == PINK) score += 140000;

        // 3) Green only when threatened (and not powered)
        if (!powered && ghostDist <= DANGER_RADIUS) {
            Pixel2D g = closest(board, distMap, GREEN);
            if (g != null) {
                int dGreen = distMap.getPixel(g.getX(), g.getY());
                if (dGreen != -1 && dGreen <= 4) {
                    score += 160000.0 / (dGreen + 1);
                    if (board[x][y] == GREEN) score += 220000;
                }
            }
        }

        // 4) Powered: may approach an edible ghost if it is reasonably close
        if (powered) {
            GhostCL edible = bestEdibleReachable(distMap, ghosts);
            if (edible != null) {
                Pixel2D gp = parsePos(edible.getPos(0).toString());
                int dg = distMap.getPixel(gp);
                if (dg != -1 && dg <= 8) {
                    score += 90000.0 / (dg + 1);
                }
            }
        }

        // 5) Extra penalty when close (not powered)
        if (!powered && ghostDist < 6) score -= (6 - ghostDist) * 24000;

        return score;
    }

    /**
     * Selects the best edible ghost that can be reached before its eatable timer expires.
     *
     * @param distFromPos distance map from the candidate position
     * @param ghosts      ghosts array
     * @return a ghost to pursue, or null if none is feasible
     */
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

    /**
     * Estimates future value of approaching pink pellets from the next position.
     *
     * A pink pellet contributes only if it is reachable and appears safe relative to danger.
     *
     * @param dist   distance map from a candidate position
     * @param board  board matrix
     * @param danger danger map
     * @return future score contribution
     */
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

    /**
     * Counts how many safe cells are reachable from a start position using BFS,
     * capped by a maximum limit.
     *
     * A neighbor is considered safe if the danger map indicates that a dangerous ghost
     * cannot reach it too soon relative to the BFS depth.
     *
     * @param start start position
     * @param map   maze helper
     * @param board board matrix
     * @param danger danger map
     * @param limit maximum number of cells to count
     * @return number of safe reachable cells, up to limit
     */
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

                // Avoid cells that ghosts can reach "soon"
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

    /**
     * Finds the closest cell of a given color using a precomputed distance map.
     *
     * @param board board matrix
     * @param dist  distance map from a source cell
     * @param color target tile code
     * @return the closest matching position, or null if none exists
     */
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

    /**
     * Returns the neighbor position for a move direction.
     *
     * The neighbor wraps around the board boundaries.
     *
     * @param p   current position
     * @param dir direction constant
     * @param map maze helper
     * @return wrapped neighbor position
     */
    private Pixel2D neighbor(Pixel2D p, int dir, Map map) {
        int x = p.getX(), y = p.getY();
        if (dir == Game.UP) y++;
        if (dir == Game.DOWN) y--;
        if (dir == Game.LEFT) x--;
        if (dir == Game.RIGHT) x++;

        int w = map.getMap().length, h = map.getMap()[0].length;
        return new Index2D((x + w) % w, (y + h) % h);
    }

    /**
     * Checks whether a position is legal for Pac-Man to enter.
     *
     * Illegal cells:
     * walls (BLUE)
     * ghost house region
     *
     * @param p     position
     * @param board board matrix
     * @return true if legal
     */
    private boolean isLegal(Pixel2D p, int[][] board) {
        return board[p.getX()][p.getY()] != BLUE && !isGhostHouse(p, board);
    }

    /**
     * Approximates the ghost house area as a small centered rectangle.
     *
     * @param p     position
     * @param board board matrix
     * @return true if position is considered inside the ghost house
     */
    private boolean isGhostHouse(Pixel2D p, int[][] board) {
        int mx = board.length / 2, my = board[0].length / 2;
        return Math.abs(p.getX() - mx) < 3 &&
                Math.abs(p.getY() - my) < 3 &&
                board[p.getX()][p.getY()] == 0;
    }

    /**
     * @param p position
     * @return stable string key in the format x,y
     */
    private String key(Pixel2D p) { return p.getX() + "," + p.getY(); }

    /**
     * Returns the opposite direction constant.
     *
     * @param dir direction
     * @return opposite direction, or -1 if unknown
     */
    private int opposite(int dir) {
        if (dir == Game.UP) return Game.DOWN;
        if (dir == Game.DOWN) return Game.UP;
        if (dir == Game.LEFT) return Game.RIGHT;
        if (dir == Game.RIGHT) return Game.LEFT;
        return -1;
    }

    /**
     * Returns the first legal direction in a fixed priority order.
     *
     * @param me   current position
     * @param board board matrix
     * @param map  maze helper
     * @return a legal direction, or Game.LEFT as a last resort
     */
    private int firstLegalDir(Pixel2D me, int[][] board, Map map) {
        for (int dir : new int[]{Game.UP, Game.DOWN, Game.LEFT, Game.RIGHT}) {
            Pixel2D next = neighbor(me, dir, map);
            if (isLegal(next, board)) return dir;
        }
        return Game.LEFT;
    }

    /**
     * Parses a position string formatted as x,y into a Pixel2D.
     *
     * @param s position string
     * @return parsed position
     */
    private static Pixel2D parsePos(String s) {
        String[] p = s.trim().split(",");
        int x = Integer.parseInt(p[0].trim());
        int y = Integer.parseInt(p[1].trim());
        return new Index2D(x, y);
    }
}
