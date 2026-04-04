package org.jabref.logic.util;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.os.OS;

// Locate and return base directory.
public class JabRefBaseDirectoryLocator {

    public static Optional<Path> getBaseDirectoryPath() {
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath == null) {
            return Optional.empty();
        }

        Path jabRefBaseDirectory = Path.of(appPath);

        if (OS.OS_X) {
            while (jabRefBaseDirectory != null
                    && !jabRefBaseDirectory.getFileName().toString().endsWith(".app")) {
                jabRefBaseDirectory = jabRefBaseDirectory.getParent();
            }
        }

        if (jabRefBaseDirectory == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(jabRefBaseDirectory.getParent());
    }
}
