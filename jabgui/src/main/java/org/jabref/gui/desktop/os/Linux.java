package org.jabref.gui.desktop.os;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.StreamGobbler;
import org.jabref.logic.util.io.FileUtil;

import org.slf4j.LoggerFactory;

/// This class contains Linux specific implementations for file directories and file/application open handling methods.
///
/// We cannot use a static logger instance here in this class as the Logger first needs to be configured in the {@link JabKit#initLogging}.
/// The configuration of tinylog will become immutable as soon as the first log entry is issued.
/// https://tinylog.org/v2/configuration
@AllowedToUseAwt("Requires AWT to open a file with the native method")
public class Linux extends NativeDesktop {

    private static final String ETC_ALTERNATIVES_X_TERMINAL_EMULATOR = "/etc/alternatives/x-terminal-emulator";

    private void nativeOpenFile(String filePath) {
        HeadlessExecutorService.INSTANCE.execute(() -> {
            try {
                Desktop.getDesktop().open(Path.of(filePath).toFile());
                LoggerFactory.getLogger(Linux.class).debug("Open file in default application with Desktop integration");
            } catch (IllegalArgumentException e) {
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
    public void openFile(String filePath, String fileType, ExternalApplicationsPreferences externalApplicationsPreferences, int pageNumber) throws IOException {
        Optional<ExternalFileType> type = ExternalFileTypes.getExternalFileTypeByExt(fileType, externalApplicationsPreferences);
        String viewer = type.map(ExternalFileType::getOpenWithApplication)
                            .filter(app -> !app.isEmpty())
                            .orElse("");

        if (!viewer.isEmpty()) {
            List<String> commands = new ArrayList<>();
            commands.add(viewer);
            addPdfPageArguments(commands, viewer, filePath, pageNumber);
            commands.add(filePath);

            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            Process process = processBuilder.start();
            StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LoggerFactory.getLogger(Linux.class)::debug);
            StreamGobbler streamGobblerError = new StreamGobbler(process.getErrorStream(), LoggerFactory.getLogger(Linux.class)::debug);

            HeadlessExecutorService.INSTANCE.execute(streamGobblerInput);
            HeadlessExecutorService.INSTANCE.execute(streamGobblerError);
        } else {
            if (isPdfType(fileType) && pageNumber > 1) {
                List<String> command = new ArrayList<>();
                getPdfViewerCommandWithPage(filePath, pageNumber).ifPresent(command::addAll);

                if (!command.isEmpty()) {
                    Process process = new ProcessBuilder(command).start();
                    StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LoggerFactory.getLogger(Linux.class)::debug);
                    StreamGobbler streamGobblerError = new StreamGobbler(process.getErrorStream(), LoggerFactory.getLogger(Linux.class)::debug);
                    HeadlessExecutorService.INSTANCE.execute(streamGobblerInput);
                    HeadlessExecutorService.INSTANCE.execute(streamGobblerError);
                    return;
                }
            }
            nativeOpenFile(filePath);
        }
    }

    @Override
    public void openFileWithApplication(String filePath, String application, int pageNumber) throws IOException {
        // Use the given app if specified, and the universal "xdg-open" otherwise:
        String[] openWith;
        if ((application != null) && !application.isEmpty()) {
            openWith = application.split(" ");
            List<String> commands = new ArrayList<>(List.of(openWith));
            addPdfPageArguments(commands, openWith[0], filePath, pageNumber);
            commands.add(filePath);

            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            Process process = processBuilder.start();

            StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LoggerFactory.getLogger(Linux.class)::debug);
            StreamGobbler streamGobblerError = new StreamGobbler(process.getErrorStream(), LoggerFactory.getLogger(Linux.class)::debug);

            HeadlessExecutorService.INSTANCE.execute(streamGobblerInput);
            HeadlessExecutorService.INSTANCE.execute(streamGobblerError);
        } else {
            nativeOpenFile(filePath);
        }
    }

    private static void addPdfPageArguments(List<String> commands, String application, String filePath, int pageNumber) {
        if (pageNumber <= 1 || !isPdfPath(filePath)) {
            return;
        }

        String executable = Path.of(application).getFileName().toString().toLowerCase(Locale.ROOT);
        if (executable.contains("evince")) {
            commands.add("--page-index=" + pageNumber);
        } else if (executable.contains("okular")) {
            commands.add("-p");
            commands.add(String.valueOf(pageNumber));
        } else if (executable.contains("zathura")) {
            commands.add("-P");
            commands.add(String.valueOf(pageNumber));
        }
    }

    private static Optional<List<String>> getPdfViewerCommandWithPage(String filePath, int pageNumber) {
        return findExecutableOnPath("evince")
                .map(executable -> List.of(executable.toString(), "--page-index=" + pageNumber, filePath))
                .or(() -> findExecutableOnPath("okular")
                        .map(executable -> List.of(executable.toString(), "-p", String.valueOf(pageNumber), filePath)))
                .or(() -> findExecutableOnPath("zathura")
                        .map(executable -> List.of(executable.toString(), "-P", String.valueOf(pageNumber), filePath)));
    }

    private static boolean isPdfType(String fileType) {
        if (fileType == null || fileType.isBlank()) {
            return false;
        }

        return StandardFileType.PDF.getExtensions().stream().anyMatch(fileType::equalsIgnoreCase);
    }

    private static boolean isPdfPath(String filePath) {
        return FileUtil.getFileExtension(filePath)
                       .map(extension -> StandardFileType.PDF.getExtensions().stream().anyMatch(extension::equalsIgnoreCase))
                       .orElse(false);
    }

    private static Optional<Path> findExecutableOnPath(String command) {
        String systemPath = System.getenv("PATH");
        if (systemPath == null || systemPath.isBlank()) {
            return Optional.empty();
        }

        for (String directory : systemPath.split(":")) {
            Path candidate = Path.of(directory, command);
            if (Files.isExecutable(candidate)) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }

    @Override
    public void openFolderAndSelectFile(Path filePath) throws IOException {
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
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        Process process = processBuilder.start();

        StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LoggerFactory.getLogger(Linux.class)::debug);
        StreamGobbler streamGobblerError = new StreamGobbler(process.getErrorStream(), LoggerFactory.getLogger(Linux.class)::debug);

        HeadlessExecutorService.INSTANCE.execute(streamGobblerInput);
        HeadlessExecutorService.INSTANCE.execute(streamGobblerError);
    }

    @Override
    public void openConsole(String absolutePath, DialogService dialogService) throws IOException {

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

                ProcessBuilder builder = new ProcessBuilder(cmd);
                builder.directory(Path.of(absolutePath).toFile());
                Process processTerminal = builder.start();

                StreamGobbler streamGobblerInput = new StreamGobbler(processTerminal.getInputStream(), LoggerFactory.getLogger(Linux.class)::debug);
                StreamGobbler streamGobblerError = new StreamGobbler(processTerminal.getErrorStream(), LoggerFactory.getLogger(Linux.class)::debug);

                HeadlessExecutorService.INSTANCE.execute(streamGobblerInput);
                HeadlessExecutorService.INSTANCE.execute(streamGobblerError);
            }
        }
    }

    @Override
    public Path getApplicationDirectory() {
        return Path.of("/usr/lib/");
    }

    @Override
    public Path getDefaultFileChooserDirectory() {
        String xdgDocumentsDir = System.getenv("XDG_DOCUMENTS_DIR");
        if (xdgDocumentsDir != null) {
            return Path.of(xdgDocumentsDir);
        }

        // Make use of xdg-user-dirs
        // See https://www.freedesktop.org/wiki/Software/xdg-user-dirs/ for details
        try {
            Process process = new ProcessBuilder("xdg-user-dir", "DOCUMENTS").start(); // Package name with 's', command without
            List<String> strings = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))
                    .lines().toList();
            if (strings.isEmpty()) {
                LoggerFactory.getLogger(Linux.class).error("xdg-user-dir returned nothing");
                return Directories.getUserDirectory();
            }
            String documentsDirectory = strings.getFirst();
            Path documentsPath = Path.of(documentsDirectory);
            if (!Files.exists(documentsPath)) {
                LoggerFactory.getLogger(Linux.class).error("xdg-user-dir returned non-existant directory {}", documentsDirectory);
                return Directories.getUserDirectory();
            }
            LoggerFactory.getLogger(Linux.class).debug("Got documents path {}", documentsPath);
            return documentsPath;
        } catch (IOException e) {
            LoggerFactory.getLogger(Linux.class).error("Error while executing xdg-user-dir", e);
        }

        // Fallback
        return Directories.getUserDirectory();
    }
}
