package org.jabref.logic.util;

import java.nio.file.Path;

import org.jabref.logic.os.OS;

/// Returns the base directory used for the packaged application.
/// Uses the {@code jpackage.app-path} system property when available.
public class JabRefBaseDirectoryLocator {

    private static Path getMacAppBundle(Path path) {
        while (path != null) {
            Path fileName = path.getFileName();

            if (fileName != null && fileName.toString().endsWith(".app")) {
                return path;
            }

            path = path.getParent();
        }

        return null;
    }

    public static Path getBaseDirectoryPath() {
        String appPath = System.getProperty("jpackage.app-path");

        Path cwdPath = Path.of("").toAbsolutePath().normalize();
        if (appPath == null) {
            // fallback: current working directory
            return cwdPath;
        }

        Path jabRefBaseDirectory = Path.of(appPath);

        if (OS.OS_X) {
            jabRefBaseDirectory = getMacAppBundle(jabRefBaseDirectory);
        }

        if (jabRefBaseDirectory == null) {
            return cwdPath;
        }

        Path parent = jabRefBaseDirectory.getParent();

        return parent != null ? parent : cwdPath;
    }
}
