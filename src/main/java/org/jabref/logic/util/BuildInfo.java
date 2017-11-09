package org.jabref.logic.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

public class BuildInfo {

    public static final String UNKNOWN_VERSION = "*unknown*";

    public static final String OS = System.getProperty("os.name", UNKNOWN_VERSION);
    public static final String OS_VERSION = System.getProperty("os.version", UNKNOWN_VERSION).toLowerCase(Locale.ROOT);
    public static final String OS_ARCH = System.getProperty("os.arch", UNKNOWN_VERSION).toLowerCase(Locale.ROOT);
    public static final String JAVA_VERSION = System.getProperty("java.version", UNKNOWN_VERSION).toLowerCase(Locale.ROOT);

    private final Version version;
    private final String authors;
    private final String developers;
    private final String year;
    private final String azureInstrumentationKey;
    private final String minRequiredJavaVersion;
    private final boolean allowJava9;


    public BuildInfo() {
        this("/build.properties");
    }

    public BuildInfo(String path) {
        Properties properties = new Properties();

        try (InputStream stream = BuildInfo.class.getResourceAsStream(path)) {
            if (stream != null) {
                try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
            }
        } catch (IOException ignored) {
            // nothing to do -> default already set
        }

        version = Version.parse(properties.getProperty("version"));
        authors = properties.getProperty("authors", "");
        year = properties.getProperty("year", "");
        developers = properties.getProperty("developers", "");
        azureInstrumentationKey = properties.getProperty("azureInstrumentationKey", "");
        minRequiredJavaVersion = properties.getProperty("minRequiredJavaVersion", "1.8");
        allowJava9 = "true".equals(properties.getProperty("allowJava9", ""));
    }

    public Version getVersion() {
        return version;
    }

    public String getAuthors() {
        return authors;
    }

    public String getDevelopers() {
        return developers;
    }

    public String getYear() {
        return year;
    }

    public String getAzureInstrumentationKey() {
        return azureInstrumentationKey;
    }

    public String getMinRequiredJavaVersion() {
        return minRequiredJavaVersion;
    }

    public boolean isAllowJava9() {
        return allowJava9;
    }

    /**
     * Tests if we are running an acceptable Java. This test uses the requirements for the Java version as specified in
     * <code>gradle.build</code>. It is possible to define a minimum version up to the built number and to indicate whether
     * Java 9 can be use (which it currently can't). It tries to compare this version number to the version of the currently
     * running JVM. The check is optimistic and will rather return true even if we could not exactly determine the version.
     *
     * @return false if we are sure the version is not sufficient; otherwise it returns true
     */
    public boolean isAcceptableJavaVersion() {
        return (!JavaVersionCheck.isJava9() || allowJava9) && JavaVersionCheck.isAtLeast(minRequiredJavaVersion);
    }
}
