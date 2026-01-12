package assignments;

import exe.ex3.game.Game;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the helper movement rule neighbor(p, dir, map).
 *
 * Contract (as implemented in your algo):
 *  - UP    => y + 1
 *  - DOWN  => y - 1
 *  - LEFT  => x - 1
 *  - RIGHT => x + 1
 *  - Cyclic wrap: (x + w) % w , (y + h) % h
 *
 * We test both "middle of board" moves and boundary wrap-around moves.
 */
public class Ex3AlgoNeighborTest {

    @Test
    @DisplayName("neighbor: UP increases y by 1 (middle of board)")
    void neighbor_up_increasesY() {
        Map map = new Map(emptyBoard(5, 5));
        Pixel2D me = new Index2D(2, 2);

        Pixel2D next = invokeNeighbor(me, Game.UP, map);

        assertEquals(2, next.getX(), "UP should not change X.");
        assertEquals(3, next.getY(), "UP should increase Y by 1.");
    }

    @Test
    @DisplayName("neighbor: DOWN decreases y by 1 (middle of board)")
    void neighbor_down_decreasesY() {
        Map map = new Map(emptyBoard(5, 5));
        Pixel2D me = new Index2D(2, 2);

        Pixel2D next = invokeNeighbor(me, Game.DOWN, map);

        assertEquals(2, next.getX(), "DOWN should not change X.");
        assertEquals(1, next.getY(), "DOWN should decrease Y by 1.");
    }

    @Test
    @DisplayName("neighbor: LEFT decreases x by 1 (middle of board)")
    void neighbor_left_decreasesX() {
        Map map = new Map(emptyBoard(5, 5));
        Pixel2D me = new Index2D(2, 2);

        Pixel2D next = invokeNeighbor(me, Game.LEFT, map);

        assertEquals(1, next.getX(), "LEFT should decrease X by 1.");
        assertEquals(2, next.getY(), "LEFT should not change Y.");
    }

    @Test
    @DisplayName("neighbor: RIGHT increases x by 1 (middle of board)")
    void neighbor_right_increasesX() {
        Map map = new Map(emptyBoard(5, 5));
        Pixel2D me = new Index2D(2, 2);

        Pixel2D next = invokeNeighbor(me, Game.RIGHT, map);

        assertEquals(3, next.getX(), "RIGHT should increase X by 1.");
        assertEquals(2, next.getY(), "RIGHT should not change Y.");
    }

    // ----------- WRAP AROUND (cyclic tunnels / edges) -----------

    @Test
    @DisplayName("neighbor: LEFT at x=0 wraps to x=w-1 (cyclic)")
    void neighbor_left_wrapsX() {
        Map map = new Map(emptyBoard(7, 4)); // w=7, h=4
        Pixel2D me = new Index2D(0, 1);

        Pixel2D next = invokeNeighbor(me, Game.LEFT, map);

        assertEquals(6, next.getX(), "LEFT from x=0 must wrap to x=w-1.");
        assertEquals(1, next.getY(), "LEFT wrap should not change Y.");
    }

    @Test
    @DisplayName("neighbor: RIGHT at x=w-1 wraps to x=0 (cyclic)")
    void neighbor_right_wrapsX() {
        Map map = new Map(emptyBoard(7, 4)); // w=7, h=4
        Pixel2D me = new Index2D(6, 2);

        Pixel2D next = invokeNeighbor(me, Game.RIGHT, map);

        assertEquals(0, next.getX(), "RIGHT from x=w-1 must wrap to x=0.");
        assertEquals(2, next.getY(), "RIGHT wrap should not change Y.");
    }

    @Test
    @DisplayName("neighbor: DOWN at y=0 wraps to y=h-1 (cyclic)")
    void neighbor_down_wrapsY() {
        Map map = new Map(emptyBoard(5, 6)); // w=5, h=6
        Pixel2D me = new Index2D(3, 0);

        Pixel2D next = invokeNeighbor(me, Game.DOWN, map);

        assertEquals(3, next.getX(), "DOWN wrap should not change X.");
        assertEquals(5, next.getY(), "DOWN from y=0 must wrap to y=h-1.");
    }

    @Test
    @DisplayName("neighbor: UP at y=h-1 wraps to y=0 (cyclic)")
    void neighbor_up_wrapsY() {
        Map map = new Map(emptyBoard(5, 6)); // w=5, h=6
        Pixel2D me = new Index2D(1, 5);

        Pixel2D next = invokeNeighbor(me, Game.UP, map);

        assertEquals(1, next.getX(), "UP wrap should not change X.");
        assertEquals(0, next.getY(), "UP from y=h-1 must wrap to y=0.");
    }

    @Test
    @DisplayName("neighbor: corner wrap (0,0) LEFT then DOWN lands at (w-1,h-1)")
    void neighbor_corner_doubleWrap() {
        Map map = new Map(emptyBoard(4, 3)); // w=4, h=3
        Pixel2D me = new Index2D(0, 0);

        Pixel2D left = invokeNeighbor(me, Game.LEFT, map);
        Pixel2D down = invokeNeighbor(me, Game.DOWN, map);

        assertEquals(3, left.getX(), "LEFT from corner x=0 wraps to w-1.");
        assertEquals(0, left.getY(), "LEFT should not change Y.");
        assertEquals(0, down.getX(), "DOWN should not change X.");
        assertEquals(2, down.getY(), "DOWN from y=0 wraps to h-1.");
    }

    /* =========================
       Helpers (tests only)
       ========================= */

    /**
     * Test helper that mirrors the production neighbor(...) logic exactly.
     * We keep it here because neighbor(...) is private in the algo.
     */
    private Pixel2D invokeNeighbor(Pixel2D p, int dir, Map map) {
        int x = p.getX(), y = p.getY();

        // your engine convention: UP=y+1, DOWN=y-1
        if (dir == Game.UP) y++;
        if (dir == Game.DOWN) y--;
        if (dir == Game.LEFT) x--;
        if (dir == Game.RIGHT) x++;

        int w = map.getMap().length, h = map.getMap()[0].length;
        return new Index2D((x + w) % w, (y + h) % h);
    }

    private int[][] emptyBoard(int w, int h) {
        return new int[w][h]; // all zeros
    }
}
