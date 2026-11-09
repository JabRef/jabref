package org.jabref.gui.desktop.os;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.application.HostServices;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.StreamGobbler;

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

    /// Starts the process and drains its output into the debug log, so it cannot block on a full pipe
    private static void startLoggingOutput(ProcessBuilder processBuilder) throws IOException {
        Process process = processBuilder.start();
        // The started program gets no input; closing its stdin pipe releases the handle instead of keeping it until garbage collection
        process.getOutputStream().close();
        HeadlessExecutorService.INSTANCE.execute(new StreamGobbler(process.getInputStream(), LoggerFactory.getLogger(Linux.class)::debug));
        HeadlessExecutorService.INSTANCE.execute(new StreamGobbler(process.getErrorStream(), LoggerFactory.getLogger(Linux.class)::debug));
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
                    String[] cmd = {"xdg-open", filePath};
                    Runtime.getRuntime().exec(cmd);
                } catch (Exception e2) {
                    LoggerFactory.getLogger(Linux.class).warn("Open operation not successful: ", e2);
                }
            } catch (IOException e) {
                LoggerFactory.getLogger(Linux.class).warn("Native open operation not successful: ", e);
            }
        });
    }

    @Override
    public void openUrlWithSystemHandler(String url) throws IOException {
        new ProcessBuilder("xdg-open", url).start();
    }

    @Override
    protected void openFileWithCustomApplication(String filePath, String application) throws IOException {
        // The application may carry arguments, e.g. "evince --fullscreen"
        List<String> command = new ArrayList<>(List.of(application.split(" ")));
        command.add(filePath);
        startLoggingOutput(new ProcessBuilder(command));
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
        startLoggingOutput(new ProcessBuilder(cmd));
    }

    @Override
    protected void openConsoleWithDefaultTerminal(String absolutePath, DialogService dialogService) throws IOException {

        if (!Files.exists(Path.of(ETC_ALTERNATIVES_X_TERMINAL_EMULATOR))) {
            dialogService.showErrorDialogAndWait(Localization.lang("Could not detect terminal automatically using '%0'. Please define a custom terminal in the preferences.", ETC_ALTERNATIVES_X_TERMINAL_EMULATOR));
            return;
        }

        ProcessBuilder processBuilder = new ProcessBuilder("readlink", ETC_ALTERNATIVES_X_TERMINAL_EMULATOR);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String emulatorName = reader.readLine();
            if (emulatorName != null) {
                emulatorName = emulatorName.substring(emulatorName.lastIndexOf(File.separator) + 1);

                String[] cmd;
                if (emulatorName.contains("gnome")) {
                    cmd = new String[] {"gnome-terminal", "--working-directory", absolutePath};
                } else if (emulatorName.contains("xfce4")) {
                    // xfce4-terminal requires "--working-directory=<directory>" format (one arg)
                    cmd = new String[] {"xfce4-terminal", "--working-directory=" + absolutePath};
                } else if (emulatorName.contains("konsole")) {
                    cmd = new String[] {"konsole", "--workdir", absolutePath};
                } else {
                    cmd = new String[] {emulatorName, absolutePath};
                }

                LoggerFactory.getLogger(Linux.class).debug("Opening terminal using {}", String.join(" ", cmd));

                startLoggingOutput(new ProcessBuilder(cmd).directory(Path.of(absolutePath).toFile()));
            }
        }
    }

    @Override
    public Path getApplicationDirectory() {
        return Path.of("/usr/lib/");
    }
}
