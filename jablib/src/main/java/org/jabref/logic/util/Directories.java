package org.jabref.logic.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.jabref.logic.ai.AiService;
import org.jabref.logic.os.OS;
import org.jabref.model.search.LinkedFilesConstants;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import net.harawata.appdirs.AppDirsFactory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

/// This collects all directories based on AppDirs.
/// OS-dependent directories are handled in the NativeDesktop class.
/// See e.g. `org.jabref.gui.desktop.os.NativeDesktop#getApplicationDirectory()`
@NullMarked
public class Directories {
    /// `xdg-user-dir` reads the user's configuration in `user-dirs.dirs`, which holds customized and localized folder names.
    /// The lookup starts a process; its result does not change while JabRef runs, and the defaults of the preferences need it
    /// again on every [org.jabref.logic.FilePreferences#getDefault()].
    private static final Supplier<Optional<Path>> XDG_DOCUMENTS_DIRECTORY = Suppliers.memoize(Directories::getXdgDocumentsDirectory);

    /// Returns the path to the system's user directory.
    ///
    /// @return the path
    public static Path getUserDirectory() {
        return Path.of(System.getProperty("user.home"));
    }

    /// Falls back to the user directory if there is no documents directory.
    public static Path getDocumentsDirectory() {
        return getDocumentsDirectory(System.getenv("XDG_DOCUMENTS_DIR"), getUserDirectory(),
                () -> OS.LINUX ? XDG_DOCUMENTS_DIRECTORY.get() : Optional.empty());
    }

    @VisibleForTesting
    static Path getDocumentsDirectory(@Nullable String xdgDocumentsDirectory, Path userDirectory, Supplier<Optional<Path>> xdgLookup) {
        return Optional.ofNullable(xdgDocumentsDirectory)
                       .map(Path::of)
                       .filter(Files::isDirectory)
                       .or(() -> xdgLookup.get().filter(Files::isDirectory))
                       .or(() -> Optional.of(userDirectory.resolve("Documents")).filter(Files::isDirectory))
                       .orElse(userDirectory);
    }

    private static Optional<Path> getXdgDocumentsDirectory() {
        try {
            Process process = new ProcessBuilder("xdg-user-dir", "DOCUMENTS")
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            try (var reader = process.inputReader(StandardCharsets.UTF_8)) {
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    return Optional.empty();
                }
                return Optional.ofNullable(reader.readLine())
                               .filter(line -> !line.isBlank() && process.exitValue() == 0)
                               .map(Path::of);
            } finally {
                process.destroyForcibly();
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(Directories.class).debug("Could not determine XDG documents directory", e);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LoggerFactory.getLogger(Directories.class).debug("Interrupted while determining XDG documents directory", e);
            return Optional.empty();
        }
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
