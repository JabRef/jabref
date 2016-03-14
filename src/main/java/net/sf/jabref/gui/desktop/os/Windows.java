package net.sf.jabref.gui.desktop.os;

import java.io.File;
import java.io.IOException;

public class Windows {
    /**
     * Opens a file on a Windows system, using its default viewer.
     *
     * @param link
     *            The filename.
     * @throws IOException
     */
    public static void openFile(String link) throws IOException {
        // escape & and spaces
        Runtime.getRuntime().exec("cmd.exe /c start " + link.replaceAll("&", "\"&\"").replaceAll(" ", "\" \""));
    }

    /**
     * Opens a file on a Windows system, using the given application.
     *
     * @param link The filename.
     * @param application Link to the app that opens the file.
     * @throws IOException
     */
    public static void openFileWithApplication(String link, String application) throws IOException {
        String escapedLink = link.replaceAll("&", "\"&\"").replaceAll(" ", "\" \"");

        Runtime.getRuntime().exec(application + " " + escapedLink);
    }

    public static void openFolderAndSelectFile(String link) throws IOException {
        String escapedLink = link.replace("&", "\"&\"");

        String cmd = "explorer.exe /select,\"" + escapedLink + "\"";

        Runtime.getRuntime().exec(cmd);
    }

    public static void openConsole(String absolutePath) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        runtime.exec("cmd.exe /c start", null, new File(absolutePath));
    }
}
