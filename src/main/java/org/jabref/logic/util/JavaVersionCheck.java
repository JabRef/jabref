package org.jabref.logic.util;

import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Provides simple checks to ensure the correct version for JabRef is available. Currently, we need to make sure that we
 * have Java 1.8 but not Java 9. The functions here are not intended for direct use. Instead, they are called inside
 * {@link BuildInfo}, which has the required java version string (e.g. 1.8.0_144) available through the build system.
 * The version check should always happen through the <code>Globals#BUILD_INFO</code> instance which is available at a
 * very early stage in the JabRef startup.
 */
public class JavaVersionCheck {

    // We expect version in the form 1.8.0_101 and need . and _ as delimiter to separate numbers
    private static final Pattern DELIMITER = Pattern.compile("[._]");
    // Be adventurous and assume that we can access this property always!
    private static final String JAVA_VERSION = System.getProperty("java.version");

    private JavaVersionCheck() {
    }

    /**
     * Tries to determine if we are running on Java 9. This test should return false, when we cannot extract the correct
     * Java version.
     *
     * @return true if Java 9 is used
     */
    static boolean isJava9() {
        final String[] toParse = JAVA_VERSION.split(DELIMITER.pattern());
        if (toParse.length >= 2) {
            try {
                final float versionNumber = Float.parseFloat(toParse[0] + '.' + toParse[1]);
                return versionNumber >= 1.9;
            } catch (final NumberFormatException nfe) {
                // assume it's not Java 9
            }
        }
        return false;
    }

    /**
     * A very optimistic test for ensuring we at least have a minimal required Java version.
     *
     * @param version Should be in the form X.X.X_XXX where X are integers.
     * @return true if the numbers in version available for comparison are all greater-equals the currently running Java
     * version.
     */
    static boolean isAtLeast(final String version) {
        final Scanner scannerRunningVersion = new Scanner(JAVA_VERSION);
        final Scanner scannerRequiredVersion = new Scanner(version);
        scannerRunningVersion.useDelimiter(DELIMITER);
        scannerRequiredVersion.useDelimiter(DELIMITER);
        while (scannerRunningVersion.hasNextInt() && scannerRequiredVersion.hasNextInt()) {
            final int running = scannerRunningVersion.nextInt();
            final int required = scannerRequiredVersion.nextInt();
            if (running < required) {
                return false;
            }
        }
        return true;
    }

    public static String getJavaVersion() {
        return JAVA_VERSION;
    }
}
