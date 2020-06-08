package org.jabref.logic.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

public final class BuildInfo {

    public static final String UNKNOWN_VERSION = "*unknown*";

    public static final String OS = System.getProperty("os.name", UNKNOWN_VERSION);
    public static final String OS_VERSION = System.getProperty("os.version", UNKNOWN_VERSION).toLowerCase(Locale.ROOT);
    public static final String OS_ARCH = System.getProperty("os.arch", UNKNOWN_VERSION).toLowerCase(Locale.ROOT);
    public static final String JAVA_VERSION = System.getProperty("java.version", UNKNOWN_VERSION).toLowerCase(Locale.ROOT);

    public final Version version;
    public final String authors;
    public final String developers;
    public final String year;
    public final String azureInstrumentationKey;
    public final String springerNatureAPIKey;
    public final String astrophysicsDataSystemAPIKey;
    public final String ieeeAPIKey;
    public final String minRequiredJavaVersion;
    public final boolean allowJava9;

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
        springerNatureAPIKey = properties.getProperty("springerNatureAPIKey", "");
        astrophysicsDataSystemAPIKey = properties.getProperty("astrophysicsDataSystemAPIKey", "");
        ieeeAPIKey = properties.getProperty("ieeeAPIKey", "");
        minRequiredJavaVersion = properties.getProperty("minRequiredJavaVersion", "1.8");
        allowJava9 = "true".equals(properties.getProperty("allowJava9", ""));
    }
}
