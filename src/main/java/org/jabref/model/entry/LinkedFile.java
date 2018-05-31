package org.jabref.model.entry;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.Observable;
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
    //We have to mark these properties as transient because they can't be serialized directly
    private transient StringProperty description = new SimpleStringProperty();
    private transient StringProperty link = new SimpleStringProperty();
    private transient StringProperty fileType = new SimpleStringProperty();

    public LinkedFile(String description, String link, String fileType) {
        this.description.setValue(Objects.requireNonNull(description));
        this.fileType.setValue(Objects.requireNonNull(fileType));
        setLink(Objects.requireNonNull(link));
    }

    public LinkedFile(URL link, String fileType) {
        this("", Objects.requireNonNull(link).toString(), fileType);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public StringProperty linkProperty() {
        return link;
    }

    public StringProperty fileTypeProperty() {
        return fileType;
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
        if (!isOnlineLink(link)) {
            this.link.setValue(link.replace("\\", "/"));
        } else {
            this.link.setValue(link);
        }
    }

    public Observable[] getObservables() {
        return new Observable[] {this.link, this.description, this.fileType};
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof LinkedFile) {
            LinkedFile that = (LinkedFile) o;
            return Objects.equals(description.get(), that.description.get())
                    && Objects.equals(link.get(), that.link.get())
                    && Objects.equals(fileType.get(), that.fileType.get());
        }
        return false;
    }

    /**
     * Writes serialized object to ObjectOutputStream, automatically called
     * @param out {@link ObjectOutputStream}
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(getFileType());
        out.writeUTF(getLink());
        out.writeUTF(getDescription());
        out.flush();
    }

    /**
     * Reads serialized object from ObjectInputStreamm, automatically called
     * @param in {@link ObjectInputStream}
     * @throws IOException
     */
    private void readObject(ObjectInputStream in) throws IOException {
        fileType = new SimpleStringProperty(in.readUTF());
        link = new SimpleStringProperty(in.readUTF());
        description = new SimpleStringProperty(in.readUTF());
    }

    /**
     * Checks if the given String is an online link
     * @param toCheck The String to check
     * @return True if it starts with http://, https:// or contains www; false otherwise
     */
    private boolean isOnlineLink(String toCheck) {
        return toCheck.startsWith("http://") || toCheck.startsWith("https://") || toCheck.contains("www.");
    }

    @Override
    public int hashCode() {
        return Objects.hash(description.get(), link.get(), fileType.get());
    }

    @Override
    public String toString() {
        return "ParsedFileField{" +
                "description='" + description.get() + '\'' +
                ", link='" + link.get() + '\'' +
                ", fileType='" + fileType.get() + '\'' +
                '}';
    }

    public boolean isEmpty() {
        return NULL_OBJECT.equals(this);
    }

    public boolean isOnlineLink() {
        return isOnlineLink(link.get());
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
