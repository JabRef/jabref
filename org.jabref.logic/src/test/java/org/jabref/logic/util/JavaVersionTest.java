package org.jabref.logic.util;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for ensuring we can compare most appearing version strings
 */
public class JavaVersionTest {

    private static List<String> java = new ArrayList<>();
    private static List<String > java9 = new ArrayList<>();

    private final static JavaVersion nullCheck = new JavaVersion(null);
    private final static JavaVersion noSenseCheck = new JavaVersion("U.N.K.N.O.W.N");


    static {
        java.add("1.6.0_10"); // Oracle
        java.add("1.6.0_45"); // Oracle
        java.add("1.7.0_13"); // Oracle
        java.add("1.8.0_76-release"); //openjdk
        java.add("1.8.0_92"); //Oracle
        java.add("1.8.0_111"); //Oracle
        java.add("1.8.0_112-release"); //openjdk
        java.add("1.8.0_152-release"); //openjdk
        java.add("1.8.0_144"); //Oracle

        // Examples http://openjdk.java.net/jeps/223
        // Note that it might be possible that java 9 versions are either 9.1.4+8 or new style 1.9.0_31-b08
        java9.add("9-internal"); // openjdk
        java9.add("1.9.0_20-b62");
        java9.add("1.9.0_20-b62");
        java9.add("9.2.4+45");
    }

    @Test
    public void isJava9() throws Exception {
        // Check that all valid java versions below 9 are recognized as not java 9
        for (String versionString : java) {
            final JavaVersion java8 = new JavaVersion(versionString);
            Assert.assertFalse(java8.isJava9());
        }
        // Check if all valid version 9 strings are recognized as being version 9
        for (String version9String : java9) {
            final JavaVersion java9 = new JavaVersion(version9String);
            Assert.assertTrue(java9.isJava9());
        }

        // For impossible comparisons we assume it's not java 9
        Assert.assertFalse(nullCheck.isJava9());
        Assert.assertFalse(noSenseCheck.isJava9());
    }

    @Test
    public void isAtLeast() throws Exception {
        final JavaVersion java8 = new JavaVersion("1.8");
        for (String version8 : java) {
            Assert.assertTrue(java8.isAtLeast(version8));
            final JavaVersion java8Example = new JavaVersion(version8);
            Assert.assertTrue(java8Example.isAtLeast("1.5"));

            // Check if we optimistically return true if we cannot determine the result
            Assert.assertTrue(java8Example.isAtLeast(null));
            Assert.assertTrue(nullCheck.isAtLeast(version8));
            Assert.assertTrue(noSenseCheck.isAtLeast(version8));
            Assert.assertTrue(java8Example.isAtLeast("useless"));


            // Check against all java 9 entries in both directions
            for (String version9 : java9) {
                Assert.assertFalse(java8Example.isAtLeast(version9));
                final JavaVersion java9 = new JavaVersion(version9);
                Assert.assertTrue(java9.isAtLeast(version8));
            }
        }
    }
}
