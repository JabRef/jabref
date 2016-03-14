package net.sf.jabref.gui.desktop.os;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.ExternalFileTypes;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Linux {
    public static void openFolderAndSelectFile(String fileLink) throws IOException {
        String desktopSession = System.getenv("DESKTOP_SESSION").toLowerCase();

        String cmd;

        if (desktopSession.contains("gnome")) {
            cmd = "nautilus " + fileLink;
        } else if (desktopSession.contains("kde")) {
            cmd = "dolphin --select " + fileLink;
        } else {
            cmd = "xdg-open " + fileLink.substring(0, fileLink.lastIndexOf(File.separator));
        }

        Runtime.getRuntime().exec(cmd);
    }

    public static void openConsole(String absolutePath) throws IOException {
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

    public static void openFile(String link, String fileType) throws IOException {
        ExternalFileType type = ExternalFileTypes.getInstance().getExternalFileTypeByExt(fileType);
        String viewer = type == null ? Globals.prefs.get(JabRefPreferences.PSVIEWER) : type.getOpenWith();
        String[] cmdArray = new String[2];
        cmdArray[0] = viewer;
        cmdArray[1] = link;
        Runtime.getRuntime().exec(cmdArray);
    }
}
