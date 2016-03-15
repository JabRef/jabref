package net.sf.jabref.gui.desktop.os;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class DefaultDesktop implements NativeDesktop {
    private static final Log LOGGER = LogFactory.getLog(NativeDesktop.class);

    @Override
    public void openFile(String filePath, String fileType) throws IOException {
        Desktop.getDesktop().open(new File(filePath));
    }

    @Override
    public void openFileWithApplication(String filePath, String application) throws IOException {
        Desktop.getDesktop().open(new File(filePath));
    }

    @Override
    public void openFolderAndSelectFile(String filePath) throws IOException {
        File file = new File(filePath);
        Desktop.getDesktop().open(file.getParentFile());
    }

    @Override
    public void openConsole(String absolutePath) throws IOException {
        LOGGER.error("This feature is not supported by your Operating System.");
    }
}
