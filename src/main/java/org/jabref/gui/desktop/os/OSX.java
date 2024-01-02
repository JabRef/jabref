package org.jabref.gui.desktop.os;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.cli.Launcher;
import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.preferences.FilePreferences;

/**
 * This class contains macOS (OSX) specific implementations for file directories and file/application open handling methods <br>
 * We cannot use a static logger instance here in this class as the Logger first needs to be configured in the {@link Launcher#addLogToDisk}
 * The configuration of tinylog will become immutable as soon as the first log entry is issued.
 * https://tinylog.org/v2/configuration/
 **/
@AllowedToUseAwt("Requires AWT to open a file")
public class OSX extends NativeDesktop {

    @Override
    public void openFile(String filePath, String fileType, FilePreferences filePreferences) throws IOException {
        Optional<ExternalFileType> type = ExternalFileTypes.getExternalFileTypeByExt(fileType, filePreferences);
        if (type.isPresent() && !type.get().getOpenWithApplication().isEmpty()) {
            openFileWithApplication(filePath, type.get().getOpenWithApplication());
        } else {
            String[] cmd = {"/usr/bin/open", filePath};
            Runtime.getRuntime().exec(cmd);
        }
    }

    @Override
    public void openFileWithApplication(String filePath, String application) throws IOException {
        // Use "-a <application>" if the app is specified, and just "open <filename>" otherwise:
        String[] cmd = (application != null) && !application.isEmpty() ? new String[] {"/usr/bin/open", "-a",
                application, filePath} : new String[] {"/usr/bin/open", filePath};
        new ProcessBuilder(cmd).start();
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
    public String detectProgramPath(String programName, String directoryName) {
        return programName;
    }

    @Override
    public Path getApplicationDirectory() {
        return Path.of("/Applications");
    }
}
