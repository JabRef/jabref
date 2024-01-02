package org.jabref.gui.desktop.os;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.cli.Launcher;
import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.preferences.FilePreferences;

import com.sun.jna.platform.win32.KnownFolders;
import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShlObj;
import com.sun.jna.platform.win32.Win32Exception;
import org.slf4j.LoggerFactory;

/**
 * This class contains Windows specific implementations for file directories and file/application open handling methods <br>
 * We cannot use a static logger instance here in this class as the Logger first needs to be configured in the {@link Launcher#addLogToDisk}
 * The configuration of tinylog will become immutable as soon as the first log entry is issued.
 * https://tinylog.org/v2/configuration/
 **/
public class Windows extends NativeDesktop {

    private static final String DEFAULT_EXECUTABLE_EXTENSION = ".exe";

    @Override
    public void openFile(String filePath, String fileType, FilePreferences filePreferences) throws IOException {
        Optional<ExternalFileType> type = ExternalFileTypes.getExternalFileTypeByExt(fileType, filePreferences);

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
        String programPath;
        if (progFiles != null) {
            programPath = getProgramPath(programName, directoryName, progFiles);
            if (programPath != null) {
                return programPath;
            }
        }

        progFiles = System.getenv("ProgramFiles");
        programPath = getProgramPath(programName, directoryName, progFiles);
        if (programPath != null) {
            return programPath;
        }

        return "";
    }

    private String getProgramPath(String programName, String directoryName, String progFiles) {
        Path programPath;
        if ((directoryName != null) && !directoryName.isEmpty()) {
            programPath = Path.of(progFiles, directoryName, programName + DEFAULT_EXECUTABLE_EXTENSION);
        } else {
            programPath = Path.of(progFiles, programName + DEFAULT_EXECUTABLE_EXTENSION);
        }
        if (Files.exists(programPath)) {
            return programPath.toString();
        }
        return null;
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
    public Path getDefaultFileChooserDirectory() {
        try {
            try {
                return Path.of(Shell32Util.getKnownFolderPath(KnownFolders.FOLDERID_Documents));
            } catch (UnsatisfiedLinkError e) {
                // Windows Vista or earlier
                return Path.of(Shell32Util.getFolderPath(ShlObj.CSIDL_MYDOCUMENTS));
            }
        } catch (Win32Exception e) {
            // needs to be non-static because of org.jabref.cli.Launcher.addLogToDisk
            LoggerFactory.getLogger(Windows.class).error("Error accessing folder", e);
            return Path.of(System.getProperty("user.home"));
        }
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
    public void openConsole(String absolutePath, DialogService dialogService) throws IOException {
        ProcessBuilder process = new ProcessBuilder("cmd.exe", "/c", "start");
        process.directory(new File(absolutePath));
        process.start();
    }
}
