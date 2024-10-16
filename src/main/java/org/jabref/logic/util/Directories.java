package org.jabref.logic.util;

import java.io.File;
import java.nio.file.Path;

import org.jabref.logic.ai.AiService;
import org.jabref.logic.os.OS;
import org.jabref.model.search.LinkedFilesConstants;

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

    public static Path getSslDirectory() {
        return Path.of(AppDirsFactory.getInstance()
                                     .getUserDataDir(OS.APP_DIR_APP_NAME,
                                             "ssl",
                                             OS.APP_DIR_APP_AUTHOR));
    }
}
