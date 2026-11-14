package org.jabref.logic.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.ai.AiService;
import org.jabref.logic.os.OS;
import org.jabref.model.search.LinkedFilesConstants;

import net.harawata.appdirs.AppDirsFactory;
import org.jspecify.annotations.NullMarked;

/// This collects all directories based on AppDirs.
/// OS-dependent directories are handled in the NativeDesktop class.
/// See e.g. `org.jabref.gui.desktop.os.NativeDesktop#getApplicationDirectory()`
@NullMarked
public class Directories {
    /// Returns the path to the system's user directory.
    ///
    /// @return the path
    public static Path getUserDirectory() {
        return Path.of(System.getProperty("user.home"));
    }

    /// Falls back to the user directory if there is no documents directory.
    /// Only checks well-known locations: jablib also runs headless, where native calls or external processes are not wanted.
    public static Path getDocumentsDirectory() {
        // Set only if the desktop session exports it; usually, it is defined in ~/.config/user-dirs.dirs only
        return Optional.ofNullable(System.getenv("XDG_DOCUMENTS_DIR"))
                       .map(Path::of)
                       .filter(Files::isDirectory)
                       .or(() -> Optional.of(getUserDirectory().resolve("Documents")).filter(Files::isDirectory))
                       .orElseGet(Directories::getUserDirectory);
    }

    public static Path getLogDirectory(Version version) {
        return Path.of(AppDirsFactory.getInstance()
                                     .getUserDataDir(
                                             OS.APP_DIR_APP_NAME,
                                             "logs",
                                             OS.APP_DIR_APP_AUTHOR))
                   .resolve(version.toString());
    }

    public static Path getBackupDirectory() {
        return Path.of(AppDirsFactory.getInstance()
                                     .getUserDataDir(
                                             OS.APP_DIR_APP_NAME,
                                             "backups",
                                             OS.APP_DIR_APP_AUTHOR));
    }

    public static Path getFulltextIndexBaseDirectory() {
        return Path.of(AppDirsFactory.getInstance()
                                     .getUserDataDir(OS.APP_DIR_APP_NAME,
                                             "lucene" + File.separator + LinkedFilesConstants.VERSION,
                                             OS.APP_DIR_APP_AUTHOR));
    }

    public static Path getAiFilesDirectory() {
        return Path.of(AppDirsFactory.getInstance()
                                     .getUserDataDir(OS.APP_DIR_APP_NAME,
                                             "ai" + File.separator + AiService.VERSION,
                                             OS.APP_DIR_APP_AUTHOR));
    }

    /// Changes made while a shared database was unreachable, one file per database
    /// (see `org.jabref.logic.shared.OfflineChanges`)
    public static Path getSharedDatabaseDirectory() {
        return Path.of(AppDirsFactory.getInstance()
                                     .getUserDataDir(OS.APP_DIR_APP_NAME,
                                             "shared-database",
                                             OS.APP_DIR_APP_AUTHOR));
    }

    public static Path getSslDirectory() {
        return Path.of(AppDirsFactory.getInstance()
                                     .getUserDataDir(OS.APP_DIR_APP_NAME,
                                             "ssl",
                                             OS.APP_DIR_APP_AUTHOR));
    }

    public static Path getCitationsRelationsDirectory() {
        return Path.of(
                AppDirsFactory.getInstance()
                              .getUserDataDir(
                                      OS.APP_DIR_APP_NAME,
                                      "relations",
                                      OS.APP_DIR_APP_AUTHOR));
    }

    public static Path getCoverDirectory() {
        return Path.of(
                AppDirsFactory.getInstance()
                              .getUserDataDir(
                                      OS.APP_DIR_APP_NAME,
                                      "covers",
                                      OS.APP_DIR_APP_AUTHOR));
    }

    public static Path getMscDirectory() {
        return Path.of(
                AppDirsFactory.getInstance()
                              .getUserDataDir(
                                      OS.APP_DIR_APP_NAME,
                                      "msc",
                                      OS.APP_DIR_APP_AUTHOR));
    }
}
