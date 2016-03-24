package net.sf.jabref.gui.desktop.os;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class Windows implements NativeDesktop {
    @Override
    public String detectProgramPath(String programName, String directoryName) {
        String progFiles = System.getenv("ProgramFiles(x86)");
        if (progFiles == null) {
            progFiles = System.getenv("ProgramFiles");
        }
        if ((directoryName != null) && !directoryName.isEmpty()) {
            return progFiles + "\\" + directoryName + "\\" + programName + ".exe";
        }
        return progFiles + "\\" + programName + ".exe";
    }

    @Override
    public void openFile(String filePath, String fileType) throws IOException {
        // escape & and spaces
        Runtime.getRuntime().exec("cmd.exe /c start " + filePath.replace("&", "\"&\"").replace(" ", "\" \""));
    }

    @Override
    public void openFileWithApplication(String filePath, String application) throws IOException {
        Runtime.getRuntime().exec(Paths.get(application) + " " + Paths.get(filePath));
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
