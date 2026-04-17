package org.jabref.logic.util;

import java.nio.file.Path;

import org.jabref.logic.os.OS;

public class JabRefBaseDirectoryLocator {

    public static Path getBaseDirectoryPath() {
        String appPath = System.getProperty("jpackage.app-path");

        Path cwdPath = Path.of("").toAbsolutePath().normalize();
        if (appPath == null) {
            // fallback: current working directory
            return cwdPath;
        }

        Path jabRefBaseDirectory = Path.of(appPath);

        if (OS.OS_X) {
            while (jabRefBaseDirectory != null
                    && !jabRefBaseDirectory.getFileName().toString().endsWith(".app")) {
                jabRefBaseDirectory = jabRefBaseDirectory.getParent();
            }
        }

        if (jabRefBaseDirectory == null) {
            return cwdPath;
        }

        Path parent = jabRefBaseDirectory.getParent();

        return parent != null ? parent : cwdPath;
    }
}
