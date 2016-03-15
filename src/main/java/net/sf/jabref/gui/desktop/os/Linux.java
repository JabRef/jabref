package net.sf.jabref.gui.desktop.os;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.ExternalFileTypes;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Linux implements NativeDesktop {
    @Override
    public void openFile(String filePath, String fileType) throws IOException {
        ExternalFileType type = ExternalFileTypes.getInstance().getExternalFileTypeByExt(fileType);
        // TODO: use "xdg-open" instead?
        String viewer = type == null ? Globals.prefs.get(JabRefPreferences.PSVIEWER) : type.getOpenWith();
        String[] cmdArray = new String[2];
        cmdArray[0] = viewer;
        cmdArray[1] = filePath;
        Runtime.getRuntime().exec(cmdArray);
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
        Runtime.getRuntime().exec(cmdArray);
    }

    @Override
    public void openFolderAndSelectFile(String filePath) throws IOException {
        String desktopSession = System.getenv("DESKTOP_SESSION").toLowerCase();

        String cmd;

        if (desktopSession.contains("gnome")) {
            cmd = "nautilus " + filePath;
        } else if (desktopSession.contains("kde")) {
            cmd = "dolphin --select " + filePath;
        } else {
            cmd = "xdg-open " + filePath.substring(0, filePath.lastIndexOf(File.separator));
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
}
