package org.jabref.gui.externalfiles;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javafx.concurrent.Task;

import org.jabref.logic.net.ProgressInputStream;
import org.jabref.logic.net.URLDownload;

import org.fxmisc.easybind.EasyBind;

public class FileDownloadTask extends Task<Void> {

    private final URL source;
    private final Path destination;

    public FileDownloadTask(URL source, Path destination) {
        this.source = source;
        this.destination = destination;
    }

    @Override
    protected Void call() throws Exception {
        URLDownload download = new URLDownload(source);
        try (ProgressInputStream inputStream = download.asInputStream()) {
            EasyBind.subscribe(
                    inputStream.totalNumBytesReadProperty(),
                    bytesRead -> updateProgress(bytesRead.longValue(), inputStream.getMaxNumBytes()));
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }

        return null;
    }
}
