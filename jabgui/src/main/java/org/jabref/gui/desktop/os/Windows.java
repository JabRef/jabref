package org.jabref.gui.desktop.os;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.util.Directories;

import com.sun.jna.platform.win32.KnownFolders;
import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShlObj;
import com.sun.jna.platform.win32.Win32Exception;
import org.slf4j.LoggerFactory;

/// This class contains Windows specific implementations for file directories and file/application open handling methods.
///
/// We cannot use a static logger instance here in this class as the Logger first needs to be configured in the {@link JabKit#initLogging}.
/// The configuration of tinylog will become immutable as soon as the first log entry is issued.
/// https://tinylog.org/v2/configuration/
public class Windows extends NativeDesktop {

    @Override
    public void openFile(String filePath, String fileType, ExternalApplicationsPreferences externalApplicationsPreferences, int pageNumber) throws IOException {
        Optional<ExternalFileType> type = ExternalFileTypes.getExternalFileTypeByExt(fileType, externalApplicationsPreferences);

        if (type.isPresent() && !type.get().getOpenWithApplication().isEmpty()) {
            openFileWithApplication(filePath, type.get().getOpenWithApplication(), pageNumber);
        } else {
            if ("pdf".equalsIgnoreCase(fileType) && pageNumber > 1) {
                Optional<Path> sumatraPdfExecutable = findSumatraPdfExecutable();
                if (sumatraPdfExecutable.isPresent()) {
                    new ProcessBuilder(
                            sumatraPdfExecutable.get().toString(),
                            "-page",
                            String.valueOf(pageNumber),
                            Path.of(filePath).toString())
                            .start();
                    return;
                }
            }

            // quote String so explorer handles URL query strings correctly
            String quotePath = "\"" + filePath + "\"";
            new ProcessBuilder("explorer.exe", quotePath).start();
        }
    }

    @Override
    public Path getApplicationDirectory() {
        String programDir = System.getenv("ProgramFiles");

        if (programDir != null) {
            return Path.of(programDir);
        }
        return Directories.getUserDirectory();
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
            // needs to be non-static because of org.jabref.Launcher.addLogToDisk
            LoggerFactory.getLogger(Windows.class).error("Error accessing folder", e);
            return Path.of(System.getProperty("user.home"));
        }
    }

    @Override
    public void openFileWithApplication(String filePath, String application, int pageNumber) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(Path.of(application).toString());

        String executable = Path.of(application).getFileName().toString().toLowerCase(Locale.ROOT);
        if (pageNumber > 1) {
            if (executable.contains("sumatrapdf")) {
                command.add("-page");
                command.add(String.valueOf(pageNumber));
            } else if (executable.contains("acrord32") || executable.contains("acrobat")) {
                command.add("/A");
                command.add("page=" + pageNumber);
            }
        }

        command.add(Path.of(filePath).toString());
        new ProcessBuilder(command).start();
    }

    @Override
    public void openFolderAndSelectFile(Path filePath) throws IOException {
        new ProcessBuilder("explorer.exe", "/select,", filePath.toString()).start();
    }

    @Override
    public void openConsole(String absolutePath, DialogService dialogService) throws IOException {
        ProcessBuilder process = new ProcessBuilder("cmd.exe", "/c", "start");
        process.directory(Path.of(absolutePath).toFile());
        process.start();
    }

    private static Optional<Path> findSumatraPdfExecutable() {
        String programFiles = System.getenv("ProgramFiles");
        String programFilesX86 = System.getenv("ProgramFiles(x86)");

        if (programFiles != null) {
            Path candidate = Path.of(programFiles, "SumatraPDF", "SumatraPDF.exe");
            if (Files.isExecutable(candidate)) {
                return Optional.of(candidate);
            }
        }

        if (programFilesX86 != null) {
            Path candidate = Path.of(programFilesX86, "SumatraPDF", "SumatraPDF.exe");
            if (Files.isExecutable(candidate)) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }
}
