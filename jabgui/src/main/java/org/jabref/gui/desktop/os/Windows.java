package org.jabref.gui.desktop.os;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javafx.application.HostServices;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.util.Directories;

import org.jspecify.annotations.NullMarked;

/// This class contains Windows specific implementations for file directories and file/application open handling methods.
///
/// We cannot use a static logger instance here in this class as the Logger first needs to be configured in the [JabKit#initLogging].
/// The configuration of tinylog will become immutable as soon as the first log entry is issued.
/// https://tinylog.org/v2/configuration/
@NullMarked
public class Windows extends NativeDesktop {

    public Windows(HostServices hostServices, GuiPreferences preferences) {
        super(hostServices, preferences);
    }

    @Override
    protected void openFileWithDefaultApplication(String filePath) throws IOException {
        // quote String so explorer handles URL query strings correctly
        startProcess(new ProcessBuilder("explorer.exe", "\"" + filePath + "\""));
    }

    @Override
    public void openUrlWithSystemHandler(String url) throws IOException {
        startProcess(new ProcessBuilder(urlOpenCommand(url)));
    }

    /// Explorer truncates its argument at the Windows path limit of 260 characters, which OAuth login URLs exceed.
    /// The URL protocol handler takes the URL unquoted and without that limit.
    static List<String> urlOpenCommand(String url) {
        if (url.length() > 260) {
            return List.of("rundll32.exe", "url.dll,FileProtocolHandler", url);
        }
        // quote String so explorer handles URL query strings correctly
        return List.of("explorer.exe", "\"" + url + "\"");
    }

    @Override
    public Path getApplicationDirectory() {
        String programDir = System.getenv("ProgramFiles");

        if (programDir != null) {
            return Path.of(programDir);
        }
        return Directories.getUserDirectory();
    }

    @Override
    protected void openFileWithCustomApplication(String filePath, String application) throws IOException {
        // filePath may be a URL; Path.of would throw on query characters and mangle the scheme
        startProcess(new ProcessBuilder(Path.of(application).toString(), filePath));
    }

    @Override
    protected void openFolderAndSelectFileWithDefaultFileBrowser(Path filePath) throws IOException {
        startProcess(new ProcessBuilder("explorer.exe", "/select,", filePath.toString()));
    }

    @Override
    protected void openConsoleWithDefaultTerminal(String absolutePath, DialogService dialogService) throws IOException {
        startProcess(new ProcessBuilder("cmd.exe", "/c", "start").directory(Path.of(absolutePath).toFile()));
    }
}
