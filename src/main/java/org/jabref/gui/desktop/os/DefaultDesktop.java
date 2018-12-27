package org.jabref.gui.desktop.os;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultDesktop implements NativeDesktop {
    private static final Logger LOGGER = LoggerFactory.getLogger(NativeDesktop.class);

    @Override
    public void openFile(String filePath, String fileType) throws IOException {
        Desktop.getDesktop().open(new File(filePath));
    }

    @Override
    public void openFileWithApplication(String filePath, String application) throws IOException {
        Desktop.getDesktop().open(new File(filePath));
    }

    @Override
    public void openFolderAndSelectFile(Path filePath) throws IOException {
        File file = filePath.toAbsolutePath().getParent().toFile();
        Desktop.getDesktop().open(file);
    }

    @Override
    public void openConsole(String absolutePath) throws IOException {
        LOGGER.error("This feature is not supported by your Operating System.");
    }

    @Override
    public void openPdfWithParameters(String filePath, List<String> parameters) throws IOException {
        //TODO imlement default
    }

    @Override
    public String detectProgramPath(String programName, String directoryName) {
        return programName;
    }

    @Override
    public Path getApplicationDirectory() {
        return getUserDirectory();
    }
}
