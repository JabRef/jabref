package org.jabref.model.entry;

import java.io.Serializable;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.FileDirectoryPreferences;
import org.jabref.model.util.FileHelper;

/**
 * Represents the link to an external file (e.g. associated PDF file).
 * This class is {@link Serializable} which is needed for drag and drop in gui
 */
public class LinkedFile implements Serializable {

    private static final LinkedFile NULL_OBJECT = new LinkedFile("", "", "");
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty link = new SimpleStringProperty();
    private final StringProperty fileType = new SimpleStringProperty();
    private final DoubleProperty downloadProgress = new SimpleDoubleProperty(-1);
    private final BooleanProperty isAutomaticallyFound = new SimpleBooleanProperty(false);

    public LinkedFile(String description, String link, String fileType) {
        this.description.setValue(Objects.requireNonNull(description));
        this.link.setValue(Objects.requireNonNull(link));
        this.fileType.setValue(Objects.requireNonNull(fileType));
    }

    public LinkedFile(String description, URL link, String fileType) {
        this(description, Objects.requireNonNull(link).toString(), fileType);
    }

    public String getFileType() {
        return fileType.get();
    }

    public void setFileType(String fileType) {
        this.fileType.setValue(fileType);
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(String description) {
        this.description.setValue(description);

    }

    public String getLink() {
        return link.get();
    }

    public void setLink(String link) {
        this.link.setValue(link);
    }

    public Observable[] getObservables() {
        return new Observable[] {this.downloadProgress, this.isAutomaticallyFound};
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof LinkedFile) {

            LinkedFile that = (LinkedFile) o;

            if (!this.description.equals(that.description)) {
                return false;
            }
            if (!this.link.equals(that.link)) {
                return false;
            }
            return this.fileType.equals(that.fileType);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, link, fileType);
    }

    @Override
    public String toString() {
        return "ParsedFileField{" +
                "description='" + description + '\'' +
                ", link='" + link + '\'' +
                ", fileType='" + fileType + '\'' +
                '}';
    }

    public boolean isEmpty() {
        return NULL_OBJECT.equals(this);
    }

    public boolean isOnlineLink() {
        return link.get().startsWith("http://") || link.get().startsWith("https://") || link.get().contains("www.");
    }

    public Optional<Path> findIn(BibDatabaseContext databaseContext, FileDirectoryPreferences fileDirectoryPreferences) {
        List<Path> dirs = databaseContext.getFileDirectoriesAsPaths(fileDirectoryPreferences);
        return findIn(dirs);
    }

    public Optional<Path> findIn(List<Path> directories) {
        Path file = Paths.get(link.get());
        if (file.isAbsolute() || directories.isEmpty()) {
            return Optional.of(file);
        } else {
            return FileHelper.expandFilenameAsPath(link.get(), directories);
        }
    }
}
