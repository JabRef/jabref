package org.jabref.gui.desktop.os;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.gui.JabRefExecutorService;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.util.StreamGobbler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllowedToUseAwt("Requires AWT to open a file with the native method")
public class Linux implements NativeDesktop {

    private static final Logger LOGGER = LoggerFactory.getLogger(Linux.class);

    private void nativeOpenFile(String filePath) {
        JabRefExecutorService.INSTANCE.execute(() -> {
            try {
                File file = new File(filePath);
                Desktop.getDesktop().open(file);
                LOGGER.debug("Open file in default application with Desktop integration");
            } catch (IllegalArgumentException e) {
                LOGGER.debug("Fail back to xdg-open");
                try {
                    String[] cmd = {"xdg-open", filePath};
                    Runtime.getRuntime().exec(cmd);
                } catch (Exception e2) {
                    LOGGER.warn("Open operation not successful: " + e2);
                }
            } catch (IOException e) {
                LOGGER.warn("Native open operation not successful: " + e);
            }
        });
    }

    @Override
    public void openFile(String filePath, String fileType) throws IOException {
        Optional<ExternalFileType> type = ExternalFileTypes.getInstance().getExternalFileTypeByExt(fileType);
        String viewer;

        if (type.isPresent() && !type.get().getOpenWithApplication().isEmpty()) {
            viewer = type.get().getOpenWithApplication();
            ProcessBuilder processBuilder = new ProcessBuilder(viewer, filePath);
            Process process = processBuilder.start();
            StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LOGGER::debug);
            StreamGobbler streamGobblerError = new StreamGobbler(process.getErrorStream(), LOGGER::debug);

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

            StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LOGGER::debug);
            StreamGobbler streamGobblerError = new StreamGobbler(process.getErrorStream(), LOGGER::debug);

            JabRefExecutorService.INSTANCE.execute(streamGobblerInput);
            JabRefExecutorService.INSTANCE.execute(streamGobblerError);
        } else {
            nativeOpenFile(filePath);
        }
    }

    @Override
    public void openFolderAndSelectFile(Path filePath) throws IOException {
        String desktopSession = System.getenv("DESKTOP_SESSION");

        String cmd = "xdg-open";// default command

        if (desktopSession != null) {
            desktopSession = desktopSession.toLowerCase(Locale.ROOT);
            if (desktopSession.contains("gnome")) {
                cmd = "nautilus --select";
            } else if (desktopSession.contains("kde") || desktopSession.contains("plasma")) {
                cmd = "dolphin --select";
            } else if (desktopSession.contains("mate")) {
                cmd = "caja --select";
            } else if (desktopSession.contains("cinnamon")) {
                cmd = "nemo";
           }
        }
        ProcessBuilder processBuilder =  new ProcessBuilder(cmd, filePath.toAbsolutePath().toString());
        Process process = processBuilder.start();

        StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LOGGER::debug);
        StreamGobbler streamGobblerError = new StreamGobbler(process.getErrorStream(), LOGGER::debug);

        JabRefExecutorService.INSTANCE.execute(streamGobblerInput);
        JabRefExecutorService.INSTANCE.execute(streamGobblerError);
    }

    @Override
    public void openConsole(String absolutePath) throws IOException {

        ProcessBuilder processBuilder = new ProcessBuilder("readlink /etc/alternatives/x-terminal-emulator");
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String emulatorName = reader.readLine();
            if (emulatorName != null) {
                emulatorName = emulatorName.substring(emulatorName.lastIndexOf(File.separator) + 1);

                String cmd = "";
                if (emulatorName.contains("gnome")) {
                    cmd = "gnome-terminal --working-directory=";
                } else if (emulatorName.contains("xfce4")) {
                    cmd = ("xfce4-terminal --working-directory=");
                } else if (emulatorName.contains("konsole")) {
                    cmd = ("konsole --workdir=");
                } else {
                    cmd = emulatorName;
                }
                process = new ProcessBuilder(cmd, absolutePath).start();

                StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LOGGER::debug);
                StreamGobbler streamGobblerError = new StreamGobbler(process.getErrorStream(), LOGGER::debug);

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
}
