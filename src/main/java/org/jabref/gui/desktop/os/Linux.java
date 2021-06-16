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
                System.out.println("Open file in default application with Desktop integration");
            } catch (IllegalArgumentException e) {
                System.out.println("Fail back to xdg-open");
                try {
                    String[] cmd = {"xdg-open", filePath};
                    Runtime.getRuntime().exec(cmd);
                } catch (Exception e2) {
                    System.out.println("Open operation not successful: " + e2);
                }
            } catch (IOException e) {
                System.out.println("Native open operation not successful: " + e);
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

        String cmd = "xdg-open " + filePath.toAbsolutePath().getParent().toString(); // default command

        if (desktopSession != null) {
            desktopSession = desktopSession.toLowerCase(Locale.ROOT);
            if (desktopSession.contains("gnome")) {
                cmd = "nautilus" + filePath.toString().replace(" ", "\\ ");
            } else if (desktopSession.contains("kde")) {
                cmd = "dolphin --select " + filePath.toString().replace(" ", "\\ ");
            }
        }
        Runtime.getRuntime().exec(cmd);
    }

    @Override
    public void openConsole(String absolutePath) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        Process p = runtime.exec("readlink /etc/alternatives/x-terminal-emulator");
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String emulatorName = reader.readLine();
        if (emulatorName != null) {
            emulatorName = emulatorName.substring(emulatorName.lastIndexOf(File.separator) + 1);

            if (emulatorName.contains("gnome")) {
                runtime.exec("gnome-terminal --working-directory=" + absolutePath);
            } else if (emulatorName.contains("xfce4")) {
                runtime.exec("xfce4-terminal --working-directory=" + absolutePath);
            } else if (emulatorName.contains("konsole")) {
                runtime.exec("konsole --workdir=" + absolutePath);
            } else {
                runtime.exec(emulatorName, null, new File(absolutePath));
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
