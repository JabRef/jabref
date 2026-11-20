package org.jabref.gui.desktop.os;

import java.io.IOException;
import java.nio.file.Path;

import javafx.application.HostServices;

import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.os.ProcessLauncher;
import org.jabref.logic.util.Directories;

import org.jspecify.annotations.NullMarked;

/// This class contains some default implementations (if OS is neither linux, windows or osx) file directories and file/application open handling methods.
///
/// We cannot use a static logger instance here in this class as the Logger first needs to be configured in the [JabKit#initLogging].
/// The configuration of tinylog will become immutable as soon as the first log entry is issued.
/// https://tinylog.org/v2/configuration/
@NullMarked
public class DefaultDesktop extends NativeDesktop {

    public DefaultDesktop(HostServices hostServices, GuiPreferences preferences, ClipBoardManager clipBoardManager) {
        super(hostServices, preferences, clipBoardManager);
    }

    @Override
    protected void openFileWithDefaultApplication(String filePath) {
        showDocument(Path.of(filePath).toUri().toString());
    }

    @Override
    protected void openUrlWithSystemHandler(String url) {
        // No native URL handler known for this platform
        showDocument(url);
    }

    @Override
    protected void openFileWithCustomApplication(String filePath, String application) throws IOException {
        ProcessLauncher.startDetached(new ProcessBuilder(application, filePath));
    }

    @Override
    protected void openFolderAndSelectFileWithDefaultFileBrowser(Path filePath) {
        showDocument(filePath.getParent().toUri().toString());
    }

    @Override
    protected void openConsoleWithDefaultTerminal(Path directory) throws IOException {
        throw new IOException("No default terminal is known for this operating system");
    }

    @Override
    public Path getApplicationDirectory() {
        return Directories.getUserDirectory();
    }
}
