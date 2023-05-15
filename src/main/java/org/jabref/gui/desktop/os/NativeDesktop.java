package org.jabref.gui.desktop.os;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.OS;
import org.jabref.model.pdf.search.SearchFieldConstants;

import net.harawata.appdirs.AppDirsFactory;

public interface NativeDesktop {

    void openFile(String filePath, String fileType) throws IOException;

    /**
     * Opens a file on an Operating System, using the given application.
     *
     * @param filePath    The filename.
     * @param application Link to the app that opens the file.
     */
    void openFileWithApplication(String filePath, String application) throws IOException;

    void openFolderAndSelectFile(Path file) throws IOException;

    void openConsole(String absolutePath, DialogService dialogService) throws IOException;

    String detectProgramPath(String programName, String directoryName);

    /**
     * Returns the path to the system's applications folder.
     *
     * @return the path to the applications folder.
     */
    Path getApplicationDirectory();

    /**
     * Get the user's default file chooser directory
     *
     * @return The path to the directory
     */
     Path getDefaultFileChooserDirectory();

    /**
     * Returns the path to the system's user directory.
     *
     * @return the path to the user directory.
     */
    default Path getUserDirectory() {
        return Path.of(System.getProperty("user.home"));
    }

    default Path getLogDirectory() {
        return Path.of(AppDirsFactory.getInstance()
                                     .getUserDataDir(
                                             OS.APP_DIR_APP_NAME,
                                             "logs",
                                             OS.APP_DIR_APP_AUTHOR))
                   .resolve(new BuildInfo().version.toString());
    }

    default Path getBackupDirectory() {
        return Path.of(AppDirsFactory.getInstance()
                                     .getUserDataDir(
                                             OS.APP_DIR_APP_NAME,
                                             "backups",
                                             OS.APP_DIR_APP_AUTHOR));
    }

    default Path getFulltextIndexBaseDirectory() {
        return Path.of(AppDirsFactory.getInstance()
                                     .getUserDataDir(OS.APP_DIR_APP_NAME,
                                             "lucene" + File.separator + SearchFieldConstants.VERSION,
                                             OS.APP_DIR_APP_AUTHOR));
    }

    default Path getSslDirectory() {
        return Path.of(AppDirsFactory.getInstance()
                                     .getUserDataDir(OS.APP_DIR_APP_NAME,
                                             "ssl",
                                             OS.APP_DIR_APP_AUTHOR));
    }
}
