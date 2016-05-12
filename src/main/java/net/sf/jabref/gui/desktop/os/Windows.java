package net.sf.jabref.gui.desktop.os;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;

import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.ExternalFileTypes;

public class Windows implements NativeDesktop {
    private static String DEFAULT_EXECUTABLE_EXTENSION = ".exe";

    @Override
    public void openFile(String filePath, String fileType) throws IOException {
        Optional<ExternalFileType> type = ExternalFileTypes.getInstance().getExternalFileTypeByExt(fileType);
        if (type.isPresent() && !type.get().getOpenWithApplication().isEmpty()) {
            openFileWithApplication(filePath, type.get().getOpenWithApplication());
        } else {
            // quote String so explorer handles URL query strings correctly
            String quotePath = "\"" + filePath +"\"";
            Runtime.getRuntime().exec(new String[] {"explorer.exe", quotePath});
        }
    }

    @Override
    public String detectProgramPath(String programName, String directoryName) {
        String progFiles = System.getenv("ProgramFiles(x86)");
        if (progFiles == null) {
            progFiles = System.getenv("ProgramFiles");
        }
        if ((directoryName != null) && !directoryName.isEmpty()) {
            return Paths.get(progFiles, directoryName, programName, DEFAULT_EXECUTABLE_EXTENSION).toString();
        }
        return Paths.get(progFiles, programName, DEFAULT_EXECUTABLE_EXTENSION).toString();
    }

    @Override
    public void openFileWithApplication(String filePath, String application) throws IOException {
        new ProcessBuilder(Paths.get(application).toString(), Paths.get(filePath).toString()).start();
    }

    @Override
    public void openFolderAndSelectFile(String filePath) throws IOException {
        String cmd = "explorer.exe";
        String arg = "/select,";
        String[] commandWithArgs = {cmd, arg, filePath};
        // Array variant, because otherwise the Tokenizer, which is internally run, kills the whitespaces in the path
        Runtime.getRuntime().exec(commandWithArgs);
    }

    @Override
    public void openConsole(String absolutePath) throws IOException {
        Runtime.getRuntime().exec("cmd.exe /c start", null, new File(absolutePath));
    }
}
