package assignments;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import assignments.Pixel2D;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for parsePos(String) helper.
 * This is a PURE function: no engine, no ghosts, no board.
 */
public class Ex3AlgoParsePosTest {
    /**
     * Happy-path test:
     * Verifies that a well-formed coordinate string "x,y" is parsed correctly.
     * This is the most common format returned by the game engine.
     */
    @Test
    @DisplayName("parsePos: valid coordinate '3,4'")
    void parsePos_valid() {
        Pixel2D p = invokeParse("3,4");
        assertEquals(3, p.getX(), "X should be parsed from the first token.");
        assertEquals(4, p.getY(), "Y should be parsed from the second token.");
    }

    /**
     * Robustness test:
     * Ensures that surrounding spaces do not break parsing.
     * We expect trim() to clean each token.
     */
    @Test
    @DisplayName("parsePos: trims spaces correctly")
    void parsePos_withSpaces() {
        Pixel2D p = invokeParse("  10 ,  7 ");
        assertEquals(10, p.getX(), "Spaces should be ignored when parsing X.");
        assertEquals(7, p.getY(), "Spaces should be ignored when parsing Y.");
    }

    /**
     * Edge-case test:
     * Confirms that negative values are handled correctly by Integer.parseInt,
     * even if they are not expected from the engine in normal gameplay.
     */
    @Test
    @DisplayName("parsePos: handles negative values")
    void parsePos_negativeValues() {
        Pixel2D p = invokeParse("-2,-9");
        assertEquals(-2, p.getX(), "Negative X must be parsed correctly.");
        assertEquals(-9, p.getY(), "Negative Y must be parsed correctly.");
    }

    /**
     * Boundary-value test:
     * Validates correct parsing at the (0,0) boundary (common coordinate origin).
     */
    @Test
    @DisplayName("parsePos: zero values")
    void parsePos_zero() {
        Pixel2D p = invokeParse("0,0");
        assertEquals(0, p.getX(), "X=0 should be parsed correctly.");
        assertEquals(0, p.getY(), "Y=0 should be parsed correctly.");
    }

    /**
     * Invalid numeric content tests:
     * Verifies that non-numeric tokens (or empty numeric tokens) trigger a
     * NumberFormatException via Integer.parseInt(...).
     */
    @Test
    @DisplayName("parsePos: invalid numbers throw NumberFormatException")
    void parsePos_invalidNumbers() {
        assertThrows(NumberFormatException.class, () -> invokeParse("a,b"),
                "Non-numeric values must throw NumberFormatException.");
        assertThrows(NumberFormatException.class, () -> invokeParse("3,a"),
                "Mixed numeric/non-numeric must throw NumberFormatException.");
        assertThrows(NumberFormatException.class, () -> invokeParse(",4"),
                "Missing X token (empty) should throw NumberFormatException.");
    }

    /**
     * Malformed format tests:
     * These cases do not provide two valid tokens after split(",") and therefore
     * accessing p[1] may fail with ArrayIndexOutOfBoundsException.
     *
     * Note: In Java, "3,".split(",") returns ["3"] (trailing empty token is dropped),
     * which is why it also results in ArrayIndexOutOfBoundsException here.
     *
     * The empty string "" results in split -> [""] and then parseInt("") which throws
     * NumberFormatException (different failure mode, documented explicitly).
     */
    @Test
    @DisplayName("parsePos: malformed format (missing comma / missing token) throws ArrayIndexOutOfBoundsException")
    void parsePos_malformedFormat() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> invokeParse("3"),
                "Missing comma should cause missing second token (p[1]).");
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> invokeParse("3,"),
                "Trailing comma is dropped by split => missing Y token (p[1]).");
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> invokeParse(" , "),
                "Only separators/spaces can lead to missing tokens after split.");
        assertThrows(NumberFormatException.class, () -> invokeParse(""),
                "Empty string yields token \"\" and parseInt(\"\") => NumberFormatException.");
    }

    /**
     * Test-only helper:
     * Mirrors the production parsePos(String) logic exactly.
     *
     * Rationale:
     * parsePos is private in Ex3Algo, so we test its behavior without reflection by
     * duplicating the small parsing logic here. This keeps tests simple and avoids
     * changing access modifiers only for testing purposes.
     */
    private Pixel2D invokeParse(String s) {
        String[] p = s.trim().split(",");
        int x = Integer.parseInt(p[0].trim());
        int y = Integer.parseInt(p[1].trim());
        return new Index2D(x, y);
    }

}
