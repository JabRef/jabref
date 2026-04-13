package org.jabref.gui.desktop.os;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.BrowserUtils;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.StandardFileType;

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
        String application = type.map(ExternalFileType::getOpenWithApplication)
                                 .filter(app -> !app.isEmpty())
                                 .orElse("");

        if (!application.isEmpty()) {
            openFileWithApplication(filePath, application, pageNumber);
        } else if (pageNumber > 1 && StandardFileType.PDF.getExtensions().stream().anyMatch(extension -> extension.equalsIgnoreCase(fileType))) {
            String fileUrlWithPage = Path.of(filePath).toUri().toString() + "#page=" + pageNumber;
            NativeDesktop.openBrowser(fileUrlWithPage, externalApplicationsPreferences);
        } else {
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
        List<String> commands = new ArrayList<>();
        commands.add(application);

        String appNameLower = Path.of(application).getFileName().toString().toLowerCase(Locale.ROOT);

        if (pageNumber > 1) {
            if (BrowserUtils.isBrowserSupportingPageJump(appNameLower)) {
                String fileUrlWithPage = Path.of(filePath).toUri().toString() + "#page=" + pageNumber;
                commands.add(fileUrlWithPage);
            } else if (appNameLower.contains("sumatrapdf")) {
                commands.add("-page");
                commands.add(String.valueOf(pageNumber));
                commands.add(filePath);
            } else if (appNameLower.contains("acrord32") || appNameLower.contains("acrobat")) {
                commands.add("/A");
                commands.add("page=" + pageNumber);
                commands.add(filePath);
            } else {
                commands.add(filePath);
            }
        } else {
            commands.add(filePath);
        }
        new ProcessBuilder(commands).start();
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
}
