package org.jabref.gui.desktop.os;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;

public class Windows implements NativeDesktop {
    private static final String DEFAULT_EXECUTABLE_EXTENSION = ".exe";

    @Override
    public void openFile(String filePath, String fileType) throws IOException {
        Optional<ExternalFileType> type = ExternalFileTypes.getInstance().getExternalFileTypeByExt(fileType);

        if (type.isPresent() && !type.get().getOpenWithApplication().isEmpty()) {
            openFileWithApplication(filePath, type.get().getOpenWithApplication());
        } else {
            // quote String so explorer handles URL query strings correctly
            String quotePath = "\"" + filePath + "\"";
            new ProcessBuilder("explorer.exe", quotePath).start();
        }
    }

    @Override
    public String detectProgramPath(String programName, String directoryName) {
        String progFiles = System.getenv("ProgramFiles(x86)");
        if (progFiles == null) {
            progFiles = System.getenv("ProgramFiles");
        }
        if ((directoryName != null) && !directoryName.isEmpty()) {
            return Path.of(progFiles, directoryName, programName + DEFAULT_EXECUTABLE_EXTENSION).toString();
        }
        return Path.of(progFiles, programName + DEFAULT_EXECUTABLE_EXTENSION).toString();
    }

    @Override
    public Path getApplicationDirectory() {
        String programDir = System.getenv("ProgramFiles");

        if (programDir != null) {
            return Path.of(programDir);
        }
        return getUserDirectory();
    }

    @Override
    public void openFileWithApplication(String filePath, String application) throws IOException {
        new ProcessBuilder(Path.of(application).toString(), Path.of(filePath).toString()).start();
    }

    @Override
    public void openFolderAndSelectFile(Path filePath) throws IOException {
        new ProcessBuilder("explorer.exe", "/select,", filePath.toString()).start();
    }

    @Override
    public void openConsole(String absolutePath) throws IOException {
        ProcessBuilder process = new ProcessBuilder("cmd.exe", "/c", "start");
        process.directory(new File(absolutePath));
        process.start();
    }
}
