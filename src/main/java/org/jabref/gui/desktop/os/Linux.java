package org.jabref.gui.desktop.os;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.cli.Launcher;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefExecutorService;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.util.StreamGobbler;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.FilePreferences;

import org.slf4j.LoggerFactory;

/**
 * This class contains Linux specific implementations for file directories and file/application open handling methods <br>
 * We cannot use a static logger instance here in this class as the Logger first needs to be configured in the {@link Launcher#addLogToDisk}
 * The configuration of tinylog will become immutable as soon as the first log entry is issued.
 * https://tinylog.org/v2/configuration/
 **/
@AllowedToUseAwt("Requires AWT to open a file with the native method")
public class Linux extends NativeDesktop {

    private static final String ETC_ALTERNATIVES_X_TERMINAL_EMULATOR = "/etc/alternatives/x-terminal-emulator";

    private void nativeOpenFile(String filePath) {
        JabRefExecutorService.INSTANCE.execute(() -> {
            try {
                File file = new File(filePath);
                Desktop.getDesktop().open(file);
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
    public void openFile(String filePath, String fileType, FilePreferences filePreferences) throws IOException {
        Optional<ExternalFileType> type = ExternalFileTypes.getExternalFileTypeByExt(fileType, filePreferences);
        String viewer;

        if (type.isPresent() && !type.get().getOpenWithApplication().isEmpty()) {
            viewer = type.get().getOpenWithApplication();
            ProcessBuilder processBuilder = new ProcessBuilder(viewer, filePath);
            Process process = processBuilder.start();
            StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LoggerFactory.getLogger(Linux.class)::debug);
            StreamGobbler streamGobblerError = new StreamGobbler(process.getErrorStream(), LoggerFactory.getLogger(Linux.class)::debug);

            JabRefExecutorService.INSTANCE.execute(streamGobblerInput);
            JabRefExecutorService.INSTANCE.execute(streamGobblerError);
        } else {
            nativeOpenFile(filePath);
        }
    }

    @Override
    public void openFileWithApplication(String filePath, String application) throws IOException {
        // Use the given app if specified, and the universal "xdg-open" otherwise:
        String[] openWith;
        if ((application != null) && !application.isEmpty()) {
            openWith = application.split(" ");
            String[] cmdArray = new String[openWith.length + 1];
            System.arraycopy(openWith, 0, cmdArray, 0, openWith.length);
            cmdArray[cmdArray.length - 1] = filePath;

            ProcessBuilder processBuilder = new ProcessBuilder(cmdArray);
            Process process = processBuilder.start();

            StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LoggerFactory.getLogger(Linux.class)::debug);
            StreamGobbler streamGobblerError = new StreamGobbler(process.getErrorStream(), LoggerFactory.getLogger(Linux.class)::debug);

            JabRefExecutorService.INSTANCE.execute(streamGobblerInput);
            JabRefExecutorService.INSTANCE.execute(streamGobblerError);
        } else {
            nativeOpenFile(filePath);
        }
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

        JabRefExecutorService.INSTANCE.execute(streamGobblerInput);
        JabRefExecutorService.INSTANCE.execute(streamGobblerError);
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
                builder.directory(new File(absolutePath));
                Process processTerminal = builder.start();

                StreamGobbler streamGobblerInput = new StreamGobbler(processTerminal.getInputStream(), LoggerFactory.getLogger(Linux.class)::debug);
                StreamGobbler streamGobblerError = new StreamGobbler(processTerminal.getErrorStream(), LoggerFactory.getLogger(Linux.class)::debug);

                JabRefExecutorService.INSTANCE.execute(streamGobblerInput);
                JabRefExecutorService.INSTANCE.execute(streamGobblerError);
            }
        }
    }

    @Override
    public String detectProgramPath(String programName, String directoryName) {
        return programName;
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
                return getUserDirectory();
            }
            String documentsDirectory = strings.getFirst();
            Path documentsPath = Path.of(documentsDirectory);
            if (!Files.exists(documentsPath)) {
                LoggerFactory.getLogger(Linux.class).error("xdg-user-dir returned non-existant directory {}", documentsDirectory);
                return getUserDirectory();
            }
            LoggerFactory.getLogger(Linux.class).debug("Got documents path {}", documentsPath);
            return documentsPath;
        } catch (IOException e) {
            LoggerFactory.getLogger(Linux.class).error("Error while executing xdg-user-dir", e);
        }

        // Fallback
        return getUserDirectory();
    }
}
