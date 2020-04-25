package org.jabref.gui.desktop.os;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;

import org.jabref.JabRefExecutorService;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.util.StreamGobbler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Linux implements NativeDesktop {

    private static final Logger LOGGER = LoggerFactory.getLogger(Linux.class);

    @Override
    public void openFile(String filePath, String fileType) throws IOException {
        Optional<ExternalFileType> type = ExternalFileTypes.getInstance().getExternalFileTypeByExt(fileType);
        String viewer;

        if (type.isPresent() && !type.get().getOpenWithApplication().isEmpty()) {
            viewer = type.get().getOpenWithApplication();
        } else {
            viewer = "xdg-open";
        }
        ProcessBuilder processBuilder = new ProcessBuilder(viewer, filePath);
        Process process = processBuilder.start();
        StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LOGGER::debug);
        StreamGobbler streamGobblerError = new StreamGobbler(process.getErrorStream(), LOGGER::debug);

        JabRefExecutorService.INSTANCE.execute(streamGobblerInput);
        JabRefExecutorService.INSTANCE.execute(streamGobblerError);
    }

    @Override
    public void openFileWithApplication(String filePath, String application) throws IOException {
        // Use the given app if specified, and the universal "xdg-open" otherwise:
        String[] openWith;
        if ((application != null) && !application.isEmpty()) {
            openWith = application.split(" ");
        } else {
            openWith = new String[] {"xdg-open"};
        }
        String[] cmdArray = new String[openWith.length + 1];
        System.arraycopy(openWith, 0, cmdArray, 0, openWith.length);
        cmdArray[cmdArray.length - 1] = filePath;

        ProcessBuilder processBuilder = new ProcessBuilder(cmdArray);
        Process process = processBuilder.start();

        StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LOGGER::debug);
        StreamGobbler streamGobblerError = new StreamGobbler(process.getErrorStream(), LOGGER::debug);

        JabRefExecutorService.INSTANCE.execute(streamGobblerInput);
        JabRefExecutorService.INSTANCE.execute(streamGobblerError);
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
        return Paths.get("/usr/lib/");
    }
}
