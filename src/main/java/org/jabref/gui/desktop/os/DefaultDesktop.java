package org.jabref.gui.desktop.os;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.cli.Launcher;
import org.jabref.gui.DialogService;
import org.jabref.preferences.FilePreferences;

import org.slf4j.LoggerFactory;

/**
 * This class contains some default implementations (if OS is neither linux, windows or osx) file directories and file/application open handling methods <br>
 * We cannot use a static logger instance here in this class as the Logger first needs to be configured in the {@link Launcher#addLogToDisk}
 * The configuration of tinylog will become immutable as soon as the first log entry is issued.
 * https://tinylog.org/v2/configuration/
 **/
@AllowedToUseAwt("Requires AWT to open a file")
public class DefaultDesktop extends NativeDesktop {

    @Override
    public void openFile(String filePath, String fileType, FilePreferences filePreferences) throws IOException {
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
    public void openConsole(String absolutePath, DialogService dialogService) throws IOException {
        LoggerFactory.getLogger(DefaultDesktop.class).error("This feature is not supported by your Operating System.");
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
