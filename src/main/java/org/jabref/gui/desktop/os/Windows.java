package org.jabref.gui.desktop.os;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.preferences.JabRefPreferences;

import static org.jabref.preferences.JabRefPreferences.ADOBE_ACROBAT_COMMAND;
import static org.jabref.preferences.JabRefPreferences.SUMATRA_PDF_COMMAND;
import static org.jabref.preferences.JabRefPreferences.USE_PDF_READER;

public class Windows implements NativeDesktop {
    private static String DEFAULT_EXECUTABLE_EXTENSION = ".exe";

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
            return Paths.get(progFiles, directoryName, programName + DEFAULT_EXECUTABLE_EXTENSION).toString();
        }
        return Paths.get(progFiles, programName + DEFAULT_EXECUTABLE_EXTENSION).toString();
    }

    @Override
    public Path getApplicationDirectory() {
        String programDir = System.getenv("ProgramFiles");

        if (programDir != null) {
            return Paths.get(programDir);
        }
        return getUserDirectory();
    }

    @Override
    public void openFileWithApplication(String filePath, String application) throws IOException {
        new ProcessBuilder(Paths.get(application).toString(), Paths.get(filePath).toString()).start();
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

    @Override
    public void openPdfWithParameters(String filePath, List<String> parameters) throws IOException {
        String pdfReaderPath = JabRefPreferences.getInstance().get(USE_PDF_READER);
        if (pdfReaderPath.equals(SUMATRA_PDF_COMMAND) || pdfReaderPath.equals(ADOBE_ACROBAT_COMMAND)) {
            String[] command = new String[parameters.size() + 2];
            command[0] = "\"" + Paths.get(pdfReaderPath).toString() + "\"";
            for (int i = 1; i < command.length - 1; i++) {
                command[i] = "\"" + parameters.get(i - 1) + "\"";
            }
            command[command.length - 1] = "\"" + filePath + "\"";
            new ProcessBuilder(command).start();
        } else {
            openFile(filePath, "PDF");
        }
    }
}
