package org.jabref.gui.desktop.os;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.util.StandardFileType;

@AllowedToUseAwt("Requires AWT to open a file")
public class OSX extends NativeDesktop {

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
        List<String> command = new ArrayList<>();
        command.add("/usr/bin/open");


        String appNameLower = application.toLowerCase(Locale.ROOT);

        if (pageNumber > 1 && (appNameLower.contains("chrome") || appNameLower.contains("edge") ||
                appNameLower.contains("safari") || appNameLower.contains("firefox") || appNameLower.contains("brave"))) {
            String fileUrlWithPage = Path.of(filePath).toUri().toString() + "#page=" + pageNumber;
            String executable = application;
            if (application.endsWith(".app")) {
                if (appNameLower.contains("chrome")) {
                    executable += "/Contents/MacOS/Google Chrome";
                } else if (appNameLower.contains("edge")) {
                    executable += "/Contents/MacOS/Microsoft Edge";
                }
            }

            new ProcessBuilder(executable, fileUrlWithPage).start();
            return;
        }

        if (application != null && !application.isEmpty()) {
            command.add("-a");
            command.add(application);
        }
        command.add(filePath);

        if (pageNumber > 1 && appNameLower.contains("skim")) {
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
            command.add("-a");
            command.add(application);
        }
        command.add(filePath);
        new ProcessBuilder(command).start();
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
