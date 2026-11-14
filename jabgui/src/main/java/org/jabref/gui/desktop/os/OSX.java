package org.jabref.gui.desktop.os;

import java.io.IOException;
import java.nio.file.Path;

import javafx.application.HostServices;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;

import org.jspecify.annotations.NullMarked;

/// This class contains macOS (OSX) specific implementations for file directories and file/application open handling methods.
///
/// We cannot use a static logger instance here in this class as the Logger first needs to be configured in the [JabKit#initLogging].
/// The configuration of tinylog will become immutable as soon as the first log entry is issued.
/// https://tinylog.org/v2/configuration/
@NullMarked
public class OSX extends NativeDesktop {

    public OSX(HostServices hostServices, GuiPreferences preferences) {
        super(hostServices, preferences);
    }

    @Override
    protected void openFileWithDefaultApplication(String filePath) throws IOException {
        startProcess(new ProcessBuilder("/usr/bin/open", filePath));
    }

    @Override
    public void openUrlWithSystemHandler(String url) throws IOException {
        startProcess(new ProcessBuilder("/usr/bin/open", url));
    }

    @Override
    protected void openFileWithCustomApplication(String filePath, String application) throws IOException {
        startProcess(new ProcessBuilder("/usr/bin/open", "-a", application, filePath));
    }

    @Override
    protected void openFolderAndSelectFileWithDefaultFileBrowser(Path file) throws IOException {
        startProcess(new ProcessBuilder("/usr/bin/open", "-R", file.toString()));
    }

    @Override
    protected void openConsoleWithDefaultTerminal(String absolutePath, DialogService dialogService) throws IOException {
        startProcess(new ProcessBuilder("open", "-a", "Terminal", absolutePath));
    }

    @Override
    public Path getApplicationDirectory() {
        return Path.of("/Applications");
    }
}
