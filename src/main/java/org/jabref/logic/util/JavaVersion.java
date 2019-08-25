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
public class JavaVersion {

    // See http://openjdk.java.net/jeps/223
    private static final Pattern DELIMITER = Pattern.compile("[._\\-+]");
    private final String JAVA_VERSION;

    public JavaVersion() {
        // Be adventurous and assume that we can always access this property!
        JAVA_VERSION = System.getProperty("java.version");
    }

    public JavaVersion(final String version) {
        JAVA_VERSION = version;
    }

    /**
     * Tries to determine if we are running on Java 9. This test should return false, when we cannot extract the correct
     * Java version. Note that Java 9 has a different version scheme like "9-internal".
     *
     * @return true if Java 9 is used
     */
    public boolean isJava9() {
        if (JAVA_VERSION != null) {
            // Since isAtLeast is very optimistic, we first need to check if we have a "number" in the version string
            // at all. Otherwise we would get false-positives.
            final Scanner scanner = new Scanner(JAVA_VERSION);
            scanner.useDelimiter(DELIMITER);
            if (scanner.hasNextInt()) {
                return isAtLeast("1.9");
            }
        }
        return false;
    }

    /**
     * A very optimistic test for ensuring we at least have a minimal required Java version. It will not fail when we
     * cannot determine the result. In essence, this method splits a version string using {@link
     * JavaVersion#DELIMITER} and compares two version number by number.
     *
     * @param version Should be in the form X.X.X_XXX where X are integers.
     * @return true if the numbers in version available for comparison are all greater-equals the currently running Java
     * version.
     */
    public boolean isAtLeast(final String version) {
        if (JAVA_VERSION == null || version == null) {
            return true;
        }
        final Scanner scannerRunningVersion = new Scanner(JAVA_VERSION);
        final Scanner scannerRequiredVersion = new Scanner(version);
        scannerRunningVersion.useDelimiter(DELIMITER);
        scannerRequiredVersion.useDelimiter(DELIMITER);
        while (scannerRunningVersion.hasNextInt() && scannerRequiredVersion.hasNextInt()) {
            final int running = scannerRunningVersion.nextInt();
            final int required = scannerRequiredVersion.nextInt();
            if (running == required) {
                continue;
            }
            return running >= required;
        }
        return true;
    }

    public String getJavaVersion() {
        return JAVA_VERSION;
    }

    public String getJavaInstallationDirectory() {
        return System.getProperty("java.home");
    }
}
