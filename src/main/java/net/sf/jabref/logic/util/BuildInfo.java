package net.sf.jabref.logic.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BuildInfo {

    public static final String UNKNOWN_VERSION = "*unknown*";

    public static final String OS = System.getProperty("os.name", UNKNOWN_VERSION).toLowerCase();
    public static final String OS_VERSION = System.getProperty("os.version", UNKNOWN_VERSION).toLowerCase();
    public static final String OS_ARCH = System.getProperty("os.arch", UNKNOWN_VERSION).toLowerCase();
    public static final String JAVA_VERSION = System.getProperty("java.version", UNKNOWN_VERSION).toLowerCase();

    private final Version version;
    private final String authors;
    private final String developers;
    private final String year;

    public BuildInfo() {
        this("/build.properties");
    }

    public BuildInfo(String path) {
        Properties properties = new Properties();

        try (InputStream stream = getClass().getResourceAsStream(path)) {
            if(stream != null) {
                properties.load(stream);
            }
        } catch (IOException ignored) {
            // nothing to do -> default already set
        }

        version = new Version(properties.getProperty("version", UNKNOWN_VERSION));
        authors = properties.getProperty("authors", "");
        year = properties.getProperty("year", "");
        developers = properties.getProperty("developers", "");

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

}
