package org.jabref.gui.externalfiles;

import java.io.FileFilter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TreeItem;
import javafx.stage.FileChooser;

import org.jabref.gui.util.FileFilterConverter;
import org.jabref.logic.util.StandardFileType;

import com.sun.star.util.DateTime;

public class FindUnlinkedFilesViewModel {
    private StringProperty directory = new SimpleStringProperty();
    private BooleanProperty isDirectorySpecified = new SimpleBooleanProperty();
    private ObjectProperty<FileFilter> fileFilter;
    private ObjectProperty<List<UnlinkedFile>>  unlinkedFiles;

    private List<FileChooser.ExtensionFilter> fileFilterList = new LinkedList<FileChooser.ExtensionFilter>();
    private List<UnlinkedFileRow> unlinkedFilesList = new LinkedList<UnlinkedFileRow>();
    private TreeItem<UnlinkedFileRow> treeRoot = new TreeItem<>(new UnlinkedFileRow("Unlinked Files"));


    private void initialize() {
        // fetch the file filter from the model
    }

    public BooleanProperty isDirectorySpecifiedProperty() {
        return isDirectorySpecified;
    }

    // fileFilter
    // https://stackoverflow.com/a/34512438/3830240
    public ObjectProperty<FileFilter> fileFilterProperty() {
        return fileFilter;
    }

    public FileFilter getFileFilter() {
        return fileFilterProperty().get();
    }

    public void setFileFilter(FileFilter fileFilter) {
        fileFilterProperty().set(fileFilter);
    }

    // UnlinkedFileTable
    public ObjectProperty<List<UnlinkedFile>> unlinkedFileObjectProperty() {
        return unlinkedFiles;
    }

}

class UnlinkedFile {
    private Path fullPath;
    private DateTime dateTimeCreated;
    private boolean directory;

    public Path getFullPath() {
        return fullPath;
    }

    public void setFullPath(Path fullPath) {
        this.fullPath = fullPath;
    }

    public boolean isDirectory() {
        return directory;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
    }

    public DateTime getDateTimeCreated() {
        return dateTimeCreated;
    }

    public void setDateTimeCreated(DateTime dateTimeCreated) {
        this.dateTimeCreated = dateTimeCreated;
    }
}

class UnlinkedFileRow {
    private UnlinkedFile unlinkedFile;
    private int fileCount;
    private String filename;

    UnlinkedFileRow(String filename) {
        this.filename = filename;
        this.unlinkedFile = null;
        fileCount = 0;
    }

    public boolean isDirectory() {
        return unlinkedFile.isDirectory();
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}

