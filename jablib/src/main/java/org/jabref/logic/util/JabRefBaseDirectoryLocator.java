package org.jabref.logic.util;

import java.nio.file.Path;

import org.jabref.logic.os.OS;

// Locate and return base directory.
public class JabRefBaseDirectoryLocator {

    public static Path getBaseDirectoryPath() {
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath == null) {
            return null;
        }

        Path jabRefBaseDirectory = Path.of(appPath);
        if (OS.OS_X) {
            while (jabRefBaseDirectory != null && !jabRefBaseDirectory.getFileName().toString().endsWith(".app")) {
                jabRefBaseDirectory = jabRefBaseDirectory.getParent();
            }
        }

        return jabRefBaseDirectory == null ? Path.of("") : jabRefBaseDirectory.getParent();
    }
}
