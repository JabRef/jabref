package net.sf.jabref.testutils;

import org.junit.Assert;

public class AssertUtil {

    /**
     * Will check if two paths are the same.
     */
    public static void assertEqualPaths(String path1, String path2) {
        Assert.assertNotNull("first path must not be null", path1);
        Assert.assertNotNull("second path must not be null", path2);
        Assert.assertEquals(path1.replaceAll("\\\\", "/"), path2.replaceAll("\\\\", "/"));
    }
}
