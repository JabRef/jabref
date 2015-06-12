package net.sf.jabref;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AssertUtil {
    /**
     * Will check if two paths are the same.
     */
    public static void assertEqualPaths(String path1, String path2) {
        assertNotNull("first path must not be null", path1);
        assertNotNull("second path must not be null", path2);
        assertEquals(path1.replaceAll("\\\\", "/"), path2.replaceAll("\\\\", "/"));
    }
}
