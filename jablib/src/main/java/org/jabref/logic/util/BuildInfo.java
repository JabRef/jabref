package org.jabref.logic.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

import io.github.darvil.terminal.textformatter.TextFormatter;
import io.github.darvil.terminal.textformatter.color.SimpleColor;

public final class BuildInfo {
    public static final String JABREF_BANNER = TextFormatter.of("""

               &&&    &&&&&    &&&&&&&&   &&&&&&&&   &&&&&&&&& &&&&&&&&&
               &&&    &&&&&    &&&   &&&  &&&   &&&  &&&       &&&
               &&&   &&& &&&   &&&   &&&  &&&   &&&  &&&       &&&
               &&&   &&   &&   &&&&&&&    &&&&&&&&   &&&&&&&&  &&&&&&&
               &&&  &&&&&&&&&  &&&   &&&  &&&   &&&  &&&       &&&
               &&&  &&&   &&&  &&&   &&&  &&&   &&&  &&&       &&&
            &&&&&   &&&   &&&  &&&&&&&&   &&&   &&&  &&&&&&&&& &&&
            """, SimpleColor.MAGENTA).toString() +
            TextFormatter.of("\nVersion: %s\n", SimpleColor.BRIGHT_WHITE).toString() + """
            Staying on top of your literature since 2003 - https://www.jabref.org/
            Please report issues at https://github.com/JabRef/jabref/issues
            """;

    public static final String UNKNOWN_VERSION = "UNKNOWN";
    public static final String OS = System.getProperty("os.name", UNKNOWN_VERSION);
    public static final String OS_VERSION = System.getProperty("os.version", UNKNOWN_VERSION).toLowerCase(Locale.ROOT);
    public static final String OS_ARCH = System.getProperty("os.arch", UNKNOWN_VERSION).toLowerCase(Locale.ROOT);
    public static final String JAVA_VERSION = System.getProperty("java.version", UNKNOWN_VERSION).toLowerCase(Locale.ROOT);

    public final Version version;
    public final String maintainers;
    public final String year;

    public final String astrophysicsDataSystemAPIKey;
    public final String biodiversityHeritageApiKey;
    public final String ieeeAPIKey;
    public final String medlineApiKey;
    public final String scienceDirectApiKey;
    public final String semanticScholarApiKey;
    public final String springerNatureAPIKey;
    public final String unpaywallEmail;

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
            // nothing to do -> default will be set
        }

        version = Version.parse(properties.getProperty("version"));
        year = properties.getProperty("year", "");
        maintainers = properties.getProperty("maintainers", "");

        astrophysicsDataSystemAPIKey = BuildInfo.getValue(properties, "astrophysicsDataSystemAPIKey", "tAhPRKADc6cC26mZUnAoBt3MAjCvKbuCZsB4lI3c");
        biodiversityHeritageApiKey = BuildInfo.getValue(properties, "biodiversityHeritageApiKey", "36b910b6-2eb3-46f2-b64c-9abc149925ba");
        ieeeAPIKey = BuildInfo.getValue(properties, "ieeeAPIKey", "5jv3wyt4tt2bwcwv7jjk7pc3");
        medlineApiKey = BuildInfo.getValue(properties, "medlineApiKey", "");
        unpaywallEmail = BuildInfo.getValue(properties, "unpaywallEmail", "unpaywall-user@jabref.org");
        scienceDirectApiKey = BuildInfo.getValue(properties, "scienceDirectApiKey", "fb82f2e692b3c72dafe5f4f1fa0ac00b");
        semanticScholarApiKey = BuildInfo.getValue(properties, "semanticScholarApiKey", "");
        springerNatureAPIKey = BuildInfo.getValue(properties, "springerNatureAPIKey", "118d90a519d0fc2a01ee9715400054d4");
    }

    private static String getValue(Properties properties, String key, String defaultValue) {
        String result = Optional.ofNullable(properties.getProperty(key))
                                // workaround unprocessed build.properties file --> just remove the reference to some variable used in build.gradle
                                .map(value -> value.replaceAll("\\$\\{.*\\}", ""))
                                .orElse("");
        if (!result.isEmpty()) {
            return result;
        }
        return defaultValue;
    }
}
