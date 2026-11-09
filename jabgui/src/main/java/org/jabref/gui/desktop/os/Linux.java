package org.jabref.gui.desktop.os;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javafx.application.HostServices;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.HeadlessExecutorService;

import com.google.common.annotations.VisibleForTesting;
import org.jspecify.annotations.NullMarked;
import org.slf4j.LoggerFactory;

/// This class contains Linux specific implementations for file directories and file/application open handling methods.
///
/// We cannot use a static logger instance here in this class as the Logger first needs to be configured in the [JabKit#initLogging].
/// The configuration of tinylog will become immutable as soon as the first log entry is issued.
/// https://tinylog.org/v2/configuration
@NullMarked
@AllowedToUseAwt("Requires AWT to open a file with the native method")
public class Linux extends NativeDesktop {

    private static final String ETC_ALTERNATIVES_X_TERMINAL_EMULATOR = "/etc/alternatives/x-terminal-emulator";

    public Linux(HostServices hostServices, GuiPreferences preferences) {
        super(hostServices, preferences);
    }

    @Override
    protected void openFileWithDefaultApplication(String filePath) {
        HeadlessExecutorService.INSTANCE.execute(() -> {
            try {
                Desktop.getDesktop().open(Path.of(filePath).toFile());
                LoggerFactory.getLogger(Linux.class).debug("Open file in default application with Desktop integration");
            } catch (IllegalArgumentException _) {
                LoggerFactory.getLogger(Linux.class).debug("Fail back to xdg-open");
                try {
                    startProcess(new ProcessBuilder("xdg-open", filePath));
                } catch (IOException e2) {
                    LoggerFactory.getLogger(Linux.class).warn("Open operation not successful: ", e2);
                }
            } catch (IOException e) {
                LoggerFactory.getLogger(Linux.class).warn("Native open operation not successful: ", e);
            }
        });
    }

    @Override
    public void openUrlWithSystemHandler(String url) throws IOException {
        startProcess(new ProcessBuilder("xdg-open", url));
    }

    @Override
    protected void openFileWithCustomApplication(String filePath, String application) throws IOException {
        // The application may carry arguments, e.g. "evince --fullscreen"
        List<String> command = new ArrayList<>(List.of(application.split(" ")));
        command.add(filePath);
        startProcess(new ProcessBuilder(command));
    }

    @Override
    protected void openFolderAndSelectFileWithDefaultFileBrowser(Path filePath) throws IOException {
        String desktopSession = System.getenv("DESKTOP_SESSION");

        String absoluteFilePath = filePath.toAbsolutePath().toString();
        String[] cmd = {"xdg-open", filePath.getParent().toString()}; // default is the folder of the file

        if (desktopSession != null) {
            desktopSession = desktopSession.toLowerCase(Locale.ROOT);
            if (desktopSession.contains("gnome")) {
                cmd = new String[] {"nautilus", "--select", absoluteFilePath};
            } else if (desktopSession.contains("kde") || desktopSession.contains("plasma")) {
                cmd = new String[] {"dolphin", "--select", absoluteFilePath};
            } else if (desktopSession.contains("mate")) {
                cmd = new String[] {"caja", "--select", absoluteFilePath};
            } else if (desktopSession.contains("cinnamon")) {
                cmd = new String[] {"nemo", absoluteFilePath}; // Although nemo is based on nautilus it does not support --select, it directly highlights the file
            } else if (desktopSession.contains("xfce")) {
                cmd = new String[] {"thunar", absoluteFilePath};
            }
        }
        LoggerFactory.getLogger(Linux.class).debug("Opening folder and selecting file using {}", String.join(" ", cmd));
        startProcess(new ProcessBuilder(cmd));
    }

    @Override
    protected void openConsoleWithDefaultTerminal(String absolutePath, DialogService dialogService) throws IOException {
        Optional<String> emulatorName = terminalEmulatorName(Path.of(ETC_ALTERNATIVES_X_TERMINAL_EMULATOR));
        if (emulatorName.isEmpty()) {
            dialogService.showErrorDialogAndWait(Localization.lang("Could not detect terminal automatically using '%0'. Please define a custom terminal in the preferences.", ETC_ALTERNATIVES_X_TERMINAL_EMULATOR));
            return;
        }

        String[] cmd = switch (emulatorName.orElseThrow()) {
            case String name when name.contains("gnome") ->
                    new String[] {"gnome-terminal", "--working-directory", absolutePath};
            // xfce4-terminal requires "--working-directory=<directory>" format (one arg)
            case String name when name.contains("xfce4") ->
                    new String[] {"xfce4-terminal", "--working-directory=" + absolutePath};
            case String name when name.contains("konsole") ->
                    new String[] {"konsole", "--workdir", absolutePath};
            case String name ->
                    new String[] {name, absolutePath};
        };

        LoggerFactory.getLogger(Linux.class).debug("Opening terminal using {}", String.join(" ", cmd));

        startProcess(new ProcessBuilder(cmd).directory(Path.of(absolutePath).toFile()));
    }

    /// Returns the file name of the program the Debian alternatives link points to, e.g. `gnome-terminal.wrapper`.
    /// Empty if the link is missing, dangling, or not a link.
    @VisibleForTesting
    static Optional<String> terminalEmulatorName(Path alternativesLink) {
        // Files.exists follows the link, so a dangling link counts as missing
        if (!Files.exists(alternativesLink)) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(Files.readSymbolicLink(alternativesLink).getFileName())
                           .map(Path::toString);
        } catch (IOException e) {
            LoggerFactory.getLogger(Linux.class).debug("Could not read the link {}", alternativesLink, e);
            return Optional.empty();
        }
    }

    @Override
    public Path getApplicationDirectory() {
        return Path.of("/usr/lib/");
    }
}
