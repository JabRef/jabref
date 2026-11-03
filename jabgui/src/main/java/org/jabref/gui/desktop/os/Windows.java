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

    /// Explorer truncates its argument at this limit, so a longer argument names nothing and Explorer
    /// silently falls back to showing the user's Documents folder. The limit counts the terminating
    /// NUL character, leaving 259 usable characters.
    private static final int MAX_PATH = 260;

    public Windows(HostServices hostServices, GuiPreferences preferences) {
        super(hostServices, preferences);
    }

    @Override
    protected void openFileWithDefaultApplication(String filePath) throws IOException {
        startProcess(new ProcessBuilder(shellOpenCommand(filePath)));
    }

    @Override
    public void openUrlWithSystemHandler(String url) throws IOException {
        startProcess(new ProcessBuilder(shellOpenCommand(url)));
    }

    /// Hands a file path or URL to the registered handler of the shell. Paths of [#MAX_PATH] characters
    /// or more, which JabRef's generated file names reach easily, and OAuth login URLs go to the URL
    /// protocol handler, which takes its argument unquoted and without that limit.
    static List<String> shellOpenCommand(String pathOrUrl) {
        if (pathOrUrl.length() >= MAX_PATH) {
            return List.of("rundll32.exe", "url.dll,FileProtocolHandler", pathOrUrl);
        }
        // quote String so explorer handles URL query strings correctly
        return List.of("explorer.exe", "\"" + pathOrUrl + "\"");
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
        startProcess(new ProcessBuilder(revealCommand(filePath)));
    }

    /// Explorer cannot select a file it cannot address: at [#MAX_PATH] characters or more it ignores the
    /// whole argument and shows the user's Documents folder. Opening the containing folder without the
    /// selection at least arrives where the file is.
    static List<String> revealCommand(Path filePath) {
        String path = filePath.toString();
        Path parent = filePath.getParent();
        if (path.length() < MAX_PATH || parent == null) {
            return List.of("explorer.exe", "/select,", path);
        }
        return shellOpenCommand(parent.toString());
    }

    @Override
    protected void openConsoleWithDefaultTerminal(String absolutePath, DialogService dialogService) throws IOException {
        startProcess(new ProcessBuilder("cmd.exe", "/c", "start").directory(Path.of(absolutePath).toFile()));
    }
}
