package org.jabref.gui.desktop.os;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;

public class OSX implements NativeDesktop {

    @Override
    public void openFile(String filePath, String fileType) throws IOException {
        Optional<ExternalFileType> type = ExternalFileTypes.getInstance().getExternalFileTypeByExt(fileType);
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
        Runtime.getRuntime().exec(cmd);
    }

    @Override
    public void openFolderAndSelectFile(Path file) throws IOException {
        Desktop.getDesktop().open(file.getParent().toFile());
    }

    @Override
    public void openConsole(String absolutePath) throws IOException {
        Runtime.getRuntime().exec("open -a Terminal " + absolutePath, null, new File(absolutePath));
    }

    @Override
    public void openPdfWithParameters(String filePath, List<String> parameters) throws IOException {
        //TODO implement
    }

    @Override
    public String detectProgramPath(String programName, String directoryName) {
        return programName;
    }

    @Override
    public Path getApplicationDirectory() {
        return Paths.get("/Applications");
    }
}
