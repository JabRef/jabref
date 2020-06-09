package org.jabref.gui.desktop.os;

import java.io.IOException;
import java.nio.file.Path;

public interface NativeDesktop {
    void openFile(String filePath, String fileType) throws IOException;

    /**
     * Opens a file on an Operating System, using the given application.
     *
     * @param filePath    The filename.
     * @param application Link to the app that opens the file.
     * @throws IOException
     */
    void openFileWithApplication(String filePath, String application) throws IOException;

    void openFolderAndSelectFile(Path file) throws IOException;

    void openConsole(String absolutePath) throws IOException;

    String detectProgramPath(String programName, String directoryName);

    /**
     * Returns the path to the system's applications folder.
     *
     * @return the path to the applications folder.
     */
    Path getApplicationDirectory();

    /**
     * Returns the path to the system's user directory.
     *
     * @return the path to the user directory.
     */
    default Path getUserDirectory() {
        return Path.of(System.getProperty("user.home"));
    }
}
