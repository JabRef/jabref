package org.jabref.gui.desktop.os;

import java.io.IOException;
import java.nio.file.Path;

import javafx.application.HostServices;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.util.Directories;

import org.jspecify.annotations.NullMarked;
import org.slf4j.LoggerFactory;

/// This class contains some default implementations (if OS is neither linux, windows or osx) file directories and file/application open handling methods.
///
/// We cannot use a static logger instance here in this class as the Logger first needs to be configured in the [JabKit#initLogging].
/// The configuration of tinylog will become immutable as soon as the first log entry is issued.
/// https://tinylog.org/v2/configuration/
@NullMarked
public class DefaultDesktop extends NativeDesktop {

    public DefaultDesktop(HostServices hostServices, GuiPreferences preferences) {
        super(hostServices, preferences);
    }

    @Override
    protected void openFileWithDefaultApplication(String filePath) {
        showDocument(Path.of(filePath).toUri().toString());
    }

    @Override
    public void openUrlWithSystemHandler(String url) {
        // No native URL handler known for this platform
        showDocument(url);
    }

    @Override
    protected void openFileWithCustomApplication(String filePath, String application) throws IOException {
        startProcess(new ProcessBuilder(application, filePath));
    }

    @Override
    protected void openFolderAndSelectFileWithDefaultFileBrowser(Path filePath) {
        showDocument(filePath.toAbsolutePath().getParent().toUri().toString());
    }

    @Override
    protected void openConsoleWithDefaultTerminal(String absolutePath, DialogService dialogService) throws IOException {
        LoggerFactory.getLogger(DefaultDesktop.class).error("This feature is not supported by your Operating System.");
    }

    @Override
    public Path getApplicationDirectory() {
        return Directories.getUserDirectory();
    }
}
