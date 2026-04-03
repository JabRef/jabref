package org.jabref.logic.util;

import java.nio.file.Path;
import java.util.Locale;

public class JabRefBaseDirectoryLocator {

    public static Path getBaseDirectoryPath() {
        String appPath = System.getProperty("jpackage.app-path");
        String os = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);

        if (appPath == null) {
            return null;
        }

        Path jabRefBaseDirectory = Path.of(appPath);
        if (os.contains("mac")) {
            while (jabRefBaseDirectory != null && !jabRefBaseDirectory.getFileName().toString().endsWith(".app")) {
                jabRefBaseDirectory = jabRefBaseDirectory.getParent();
            }
        }

        return jabRefBaseDirectory == null ? Path.of("") : jabRefBaseDirectory.getParent();
    }
}
