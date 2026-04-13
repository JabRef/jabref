package org.jabref.gui.desktop.os;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.BrowserUtils;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.util.StandardFileType;

/// This class contains macOS specific implementations for file directories and file/application open handling methods.
///
/// We cannot use a static logger instance here in this class as the Logger first needs to be configured in the {@link JabKit#initLogging}.
/// The configuration of tinylog will become immutable as soon as the first log entry is issued.
/// https://tinylog.org/v2/configuration
@AllowedToUseAwt("Requires AWT to open a file")
public class OSX extends NativeDesktop {

    private static final String CHROME_EXECUTABLE_PATH = "/Contents/MacOS/Google Chrome";
    private static final String EDGE_EXECUTABLE_PATH = "/Contents/MacOS/Microsoft Edge";

    @Override
    public void openFile(String filePath, String fileType, ExternalApplicationsPreferences externalApplicationsPreferences, int pageNumber) throws IOException {
        Optional<ExternalFileType> type = ExternalFileTypes.getExternalFileTypeByExt(fileType, externalApplicationsPreferences);
        if (type.isPresent() && !type.get().getOpenWithApplication().isEmpty()) {
            openFileWithApplication(filePath, type.get().getOpenWithApplication(), pageNumber);
        } else if (pageNumber > 1 && StandardFileType.PDF.getExtensions().stream().anyMatch(extension -> extension.equalsIgnoreCase(fileType))) {
            String fileUrlWithPage = Path.of(filePath).toUri().toString() + "#page=" + pageNumber;
            NativeDesktop.openBrowser(fileUrlWithPage, externalApplicationsPreferences);
        } else {
            String[] cmd = {"/usr/bin/open", filePath};
            Runtime.getRuntime().exec(cmd);
        }
    }

    @Override
    public void openFileWithApplication(String filePath, String application, int pageNumber) throws IOException {
        List<String> commands = new ArrayList<>();
        commands.add("/usr/bin/open");

        String appNameLower = application == null ? "" : application.toLowerCase(Locale.ROOT);

        if (pageNumber > 1 && BrowserUtils.isBrowserSupportingPageJump(appNameLower)) {
            String fileUrlWithPage = Path.of(filePath).toUri().toString() + "#page=" + pageNumber;
            String browserExecutable = getBrowserExecutableWithOptions(application, appNameLower);
            new ProcessBuilder(browserExecutable, fileUrlWithPage).start();
            return;
        }

        if (pageNumber > 1 && BrowserUtils.isSkim(appNameLower)) {
            String appleScript = String.format(
                    "tell application \"Skim\"\n" +
                            "  activate\n" +
                            "  set myDoc to open POSIX file \"%s\"\n" +
                            "  tell myDoc to go to page %d\n" +
                            "end tell", filePath, pageNumber);

            new ProcessBuilder("/usr/bin/osascript", "-e", appleScript).start();
            return;
        }

        if (application != null && !application.isEmpty()) {
            commands.add("-a");
            commands.add(application);
        }
        commands.add(filePath);
        new ProcessBuilder(commands).start();
    }

    private static String getBrowserExecutableWithOptions(String application, String appNameLower) {
        if ((application != null) && application.endsWith(".app")) {
            if (BrowserUtils.isChrome(appNameLower)) {
                return application + CHROME_EXECUTABLE_PATH;
            }

            if (BrowserUtils.isEdge(appNameLower)) {
                return application + EDGE_EXECUTABLE_PATH;
            }
        }

        return application;
    }

    @Override
    public void openFolderAndSelectFile(Path file) throws IOException {
        String[] cmd = {"/usr/bin/open", "-R", file.toString()};
        Runtime.getRuntime().exec(cmd);
    }

    @Override
    public void openConsole(String absolutePath, DialogService dialogService) throws IOException {
        new ProcessBuilder("open", "-a", "Terminal", absolutePath).start();
    }

    @Override
    public Path getApplicationDirectory() {
        return Path.of("/Applications");
    }
}
