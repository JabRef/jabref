package org.jabref.gui.desktop.os;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.application.HostServices;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.CommandLineParser;
import org.jabref.logic.os.ProcessLauncher;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.strings.StringUtil;

import com.google.common.annotations.VisibleForTesting;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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
    private static final String XDG_TERMINAL_EXEC = "xdg-terminal-exec";

    public Linux(HostServices hostServices, GuiPreferences preferences, ClipBoardManager clipBoardManager) {
        super(hostServices, preferences, clipBoardManager);
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
                    ProcessLauncher.startDetached(new ProcessBuilder("xdg-open", filePath));
                } catch (IOException e2) {
                    LoggerFactory.getLogger(Linux.class).warn("Open operation not successful: ", e2);
                }
            } catch (IOException e) {
                LoggerFactory.getLogger(Linux.class).warn("Native open operation not successful: ", e);
            }
        });
    }

    @Override
    protected void openUrlWithSystemHandler(String url) throws IOException {
        ProcessLauncher.startDetached(new ProcessBuilder("xdg-open", url));
    }

    @Override
    protected void openFileWithCustomApplication(String filePath, String application) throws IOException {
        List<String> command = new ArrayList<>(applicationCommand(application));
        command.add(filePath);
        ProcessLauncher.startDetached(new ProcessBuilder(command));
    }

    /// The configured application is either the path of an executable, which may contain spaces,
    /// or a command line with arguments, such as `evince --fullscreen` or `"/opt/My Viewer/viewer" --flag`.
    @VisibleForTesting
    static List<String> applicationCommand(String application) {
        if (Files.isExecutable(Path.of(application))) {
            return List.of(application);
        }
        List<String> command = CommandLineParser.splitLegacyCommandLine(application);
        // Unbalanced quotes yield no tokens
        return command.isEmpty() ? List.of(application) : command;
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
        ProcessLauncher.startDetached(new ProcessBuilder(cmd));
    }

    @Override
    protected void openConsoleWithDefaultTerminal(Path directory) throws IOException {
        List<String> command = defaultTerminalCommand(
                terminalEmulatorName(Path.of(ETC_ALTERNATIVES_X_TERMINAL_EMULATOR)),
                Linux::isOnPath,
                System.getenv("TERMINAL"),
                desktopName(),
                directory.toString())
                .orElseThrow(() -> new IOException(Localization.lang("Could not detect a terminal. Please define a custom terminal in the preferences.")));

        LoggerFactory.getLogger(Linux.class).debug("Opening terminal using {}", String.join(" ", command));

        // Terminals started without a directory argument open their shell in the working directory
        ProcessLauncher.startDetached(new ProcessBuilder(command).directory(directory.toFile()));
    }

    /// Debian's alternatives link comes first, as it is the configured default there. Other distributions
    /// have no such link; then the launcher of the draft freedesktop specification
    /// (https://gitlab.freedesktop.org/terminal-wg/specifications/-/merge_requests/3), `$TERMINAL`, and the
    /// terminal of the desktop environment are tried.
    ///
    /// @param alternativesName the program the alternatives link points to, see [#terminalEmulatorName(Path)]
    /// @param desktop          the lower-case names of the desktop environment
    @VisibleForTesting
    static Optional<List<String>> defaultTerminalCommand(Optional<String> alternativesName,
                                                         Predicate<String> isOnPath,
                                                         @Nullable String terminalVariable,
                                                         String desktop,
                                                         String directory) {
        return alternativesName.map(name -> terminalCommand(name, directory))
                               .or(() -> Optional.of(XDG_TERMINAL_EXEC).filter(isOnPath).map(List::of))
                               .or(() -> Optional.ofNullable(terminalVariable).filter(StringUtil::isNotBlank).map(List::of))
                               .or(() -> desktopTerminals(desktop).stream()
                                                                  .filter(isOnPath)
                                                                  .findFirst()
                                                                  .map(name -> terminalCommand(name, directory)));
    }

    private static List<String> terminalCommand(String name, String directory) {
        return switch (name) {
            case String program when program.contains("gnome") ->
                    List.of("gnome-terminal", "--working-directory", directory);
            // xfce4-terminal requires "--working-directory=<directory>" format (one arg)
            case String program when program.contains("xfce4") ->
                    List.of("xfce4-terminal", "--working-directory=" + directory);
            case String program when program.contains("konsole") ->
                    List.of("konsole", "--workdir", directory);
            case String program when program.contains("ptyxis") || program.contains("kgx") ->
                    List.of(program, "--working-directory=" + directory);
            case String program ->
                    List.of(program, directory);
        };
    }

    /// The default terminals of the desktop environments, newest first: Ptyxis replaced the GNOME Console (kgx),
    /// which replaced the GNOME Terminal.
    private static List<String> desktopTerminals(String desktop) {
        if (desktop.contains("kde") || desktop.contains("plasma")) {
            return List.of("konsole");
        } else if (desktop.contains("gnome")) {
            return List.of("ptyxis", "kgx", "gnome-terminal");
        } else if (desktop.contains("xfce")) {
            return List.of("xfce4-terminal");
        }
        return List.of();
    }

    private static String desktopName() {
        return Stream.concat(Stream.ofNullable(System.getenv("XDG_CURRENT_DESKTOP")), Stream.ofNullable(System.getenv("DESKTOP_SESSION")))
                     .collect(Collectors.joining(":"))
                     .toLowerCase(Locale.ROOT);
    }

    private static boolean isOnPath(String program) {
        return Stream.ofNullable(System.getenv("PATH"))
                     .flatMap(path -> Arrays.stream(path.split(File.pathSeparator)))
                     .filter(StringUtil::isNotBlank)
                     .anyMatch(directory -> Files.isExecutable(Path.of(directory, program)));
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
