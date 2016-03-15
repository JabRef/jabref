package net.sf.jabref.gui.desktop.os;

import java.io.File;
import java.io.IOException;

public class Windows implements NativeDesktop {
    @Override
    public void openFile(String filePath, String fileType) throws IOException {
        // escape & and spaces
        Runtime.getRuntime().exec("cmd.exe /c start " + filePath.replaceAll("&", "\"&\"").replaceAll(" ", "\" \""));
    }

    @Override
    public void openFileWithApplication(String filePath, String application) throws IOException {
        String escapedLink = filePath.replaceAll("&", "\"&\"").replaceAll(" ", "\" \"");

        Runtime.getRuntime().exec(application + " " + escapedLink);
    }

    @Override
    public void openFolderAndSelectFile(String filePath) throws IOException {
        String escapedLink = filePath.replace("&", "\"&\"");

        String cmd = "explorer.exe /select,\"" + escapedLink + "\"";

        Runtime.getRuntime().exec(cmd);
    }

    @Override
    public void openConsole(String absolutePath) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        runtime.exec("cmd.exe /c start", null, new File(absolutePath));
    }
}
