package assignments;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Ex3AlgoGetInfoTest {

    @Test
    @DisplayName("getInfo: returns non-null, non-empty string")
    void getInfo_nonNullNonEmpty() {
        Ex3Algo algo = new Ex3Algo();
        String s = algo.getInfo();

        assertNotNull(s);
        assertFalse(s.trim().isEmpty());
    }

    @Test
    @DisplayName("getInfo: stable output (same call returns same string)")
    void getInfo_stable() {
        Ex3Algo algo = new Ex3Algo();
        String a = algo.getInfo();
        String b = algo.getInfo();

        assertEquals(a, b);
    }
}
