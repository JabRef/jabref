package org.jabref.gui.desktop.os;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.gui.DialogService;

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
}
