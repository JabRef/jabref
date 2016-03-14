package net.sf.jabref.gui.desktop.os;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.ExternalFileTypes;

import java.io.File;
import java.io.IOException;

public class OSX {
    public static void openFile(String link, String fileType) throws IOException {
        ExternalFileType type = ExternalFileTypes.getInstance().getExternalFileTypeByExt(fileType);
        String viewer = type == null ? Globals.prefs.get(JabRefPreferences.PSVIEWER) : type.getOpenWith();
        String[] cmd = {"/usr/bin/open", "-a", viewer, link};
        Runtime.getRuntime().exec(cmd);
    }

    public static void openFile(String link) throws IOException {
        ExternalFileType type = ExternalFileTypes.getInstance().getExternalFileTypeByExt("ps");
        String viewer = type == null ? Globals.prefs.get(JabRefPreferences.PSVIEWER) : type.getOpenWith();
        String[] cmd = {"/usr/bin/open", "-a", viewer, link};
        Runtime.getRuntime().exec(cmd);
    }

    public static void openConsole(String absolutePath) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        runtime.exec("open -a Terminal " + absolutePath, null, new File(absolutePath));
    }
}
