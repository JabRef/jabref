package org.jabref.logic.util;

import java.io.File;
import java.nio.file.Path;

import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.logic.ai.AiService;
import org.jabref.model.search.SearchFieldConstants;

import net.harawata.appdirs.AppDirsFactory;

/**
 * This collects all directories based on AppDirs.
 *
 * OS-dependent directories are handled in the NativeDesktop class.
 * See e.g. {@link org.jabref.gui.desktop.os.NativeDesktop#getApplicationDirectory()}
 */
public class Directories {
    /**
     * Returns the path to the system's user directory.
     *
     * @return the path
     */
    public static Path getUserDirectory() {
        return Path.of(System.getProperty("user.home"));
    }

    public static Path getLogDirectory() {
        return Path.of(AppDirsFactory.getInstance()
                                     .getUserDataDir(
                                             NativeDesktop.APP_DIR_APP_NAME,
                                             "logs",
                                             NativeDesktop.APP_DIR_APP_AUTHOR))
                   .resolve(new BuildInfo().version.toString());
    }

    public static Path getBackupDirectory() {
        return Path.of(AppDirsFactory.getInstance()
                                     .getUserDataDir(
                                             NativeDesktop.APP_DIR_APP_NAME,
                                             "backups",
                                             NativeDesktop.APP_DIR_APP_AUTHOR));
    }

    public static Path getFulltextIndexBaseDirectory() {
        return Path.of(AppDirsFactory.getInstance()
                                     .getUserDataDir(NativeDesktop.APP_DIR_APP_NAME,
                                             "lucene" + File.separator + SearchFieldConstants.VERSION,
                                             NativeDesktop.APP_DIR_APP_AUTHOR));
    }

    public static Path getAiFilesDirectory() {
        return Path.of(AppDirsFactory.getInstance()
                .getUserDataDir(NativeDesktop.APP_DIR_APP_NAME,
                        "ai" + File.separator + AiService.VERSION,
                        NativeDesktop.APP_DIR_APP_AUTHOR));
    }

    public static Path getSslDirectory() {
        return Path.of(AppDirsFactory.getInstance()
                                     .getUserDataDir(NativeDesktop.APP_DIR_APP_NAME,
                                             "ssl",
                                             NativeDesktop.APP_DIR_APP_AUTHOR));
    }
}
