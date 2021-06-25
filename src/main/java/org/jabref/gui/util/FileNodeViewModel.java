package org.jabref.gui.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.l10n.Localization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileNodeViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileNodeViewModel.class);
    private final Path path;
    private final ObservableList<FileNodeViewModel> children;
    private int fileCount;

    public FileNodeViewModel(Path path) {
        this.path = path;
        this.children = FXCollections.observableArrayList();
        this.fileCount = 0;
    }

    public Path getPath() {
        return path;
    }

    public ObservableList<FileNodeViewModel> getChildren() {
        return new ReadOnlyListWrapper<>(children);
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    /**
     * Return a string of a FileTime in a yyyy-MM-dd HH:mm format.
     */
    public static String formatDateTime(FileTime fileTime) {

        LocalDateTime localDateTime = fileTime
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    /**
     * Return a string for displaying a node name (and its number of children if it is a directory).
     */
    public String getDisplayText() {
        if (path.toFile().isDirectory()) {
            return String.format("%s (%s %s)", path.getFileName(), fileCount,
                    fileCount == 1 ? Localization.lang("file") : Localization.lang("files"));
        }
        return path.getFileName().toString();
    }

    /**
     * Return a string for displaying a node name (and its number of children if it is a directory).
     * along with the last edited time
     */
    public String getDisplayTextWithEditDate() {
        if (path.toFile().isDirectory()) {
            return String.format("%s (%s %s)", path.getFileName(), fileCount,
                    fileCount == 1 ? Localization.lang("file") : Localization.lang("files"));
        }
        FileTime lastEditedTime = null;
        try {
            lastEditedTime = Files.getLastModifiedTime(path);
        } catch (IOException e) {
            LOGGER.error("Exception Caught", e);
        }
        return String.format("%s (%s: %s)", path.getFileName().toString(), Localization.lang("last edited"), formatDateTime(lastEditedTime));
    }

    @Override
    public String toString() {
        return String.format("FileNodeViewModel{path=%s, children=%s, fileCount=%s}",
                this.path,
                this.children,
                this.fileCount);
    }
}
