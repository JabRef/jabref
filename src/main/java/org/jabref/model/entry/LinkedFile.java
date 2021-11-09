package org.jabref.model.entry;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileHelper;
import org.jabref.preferences.FilePreferences;

/**
 * Represents the link to an external file (e.g. associated PDF file).
 * This class is {@link Serializable} which is needed for drag and drop in gui
 */
public class LinkedFile implements Serializable {

    private static final LinkedFile NULL_OBJECT = new LinkedFile("", Path.of(""), "");

    // We have to mark these properties as transient because they can't be serialized directly
    private transient StringProperty description = new SimpleStringProperty();
    private transient StringProperty link = new SimpleStringProperty();
    private transient StringProperty fileType = new SimpleStringProperty();

    public LinkedFile(String description, Path link, String fileType) {
        this(Objects.requireNonNull(description), Objects.requireNonNull(link).toString(), Objects.requireNonNull(fileType));
    }

    /**
     * Constructor for non-valid paths. We need to parse them, because the GUI needs to render it.
     */
    public LinkedFile(String description, String link, String fileType) {
        this.description.setValue(Objects.requireNonNull(description));
        setLink(link);
        this.fileType.setValue(Objects.requireNonNull(fileType));
    }

    public LinkedFile(URL link, String fileType) {
        this("", Objects.requireNonNull(link).toString(), Objects.requireNonNull(fileType));
    }

    public LinkedFile(String description, URL link, String fileType) {
        this(description, Objects.requireNonNull(link).toString(), Objects.requireNonNull(fileType));
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
     *
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
     *
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
     *
     * @param toCheck The String to check
     * @return <code>true</code>, if it starts with "http://", "https://" or contains "www."; <code>false</code> otherwise
     */
    public static boolean isOnlineLink(String toCheck) {
        String normalizedFilePath = toCheck.trim().toLowerCase();
        return normalizedFilePath.startsWith("http://") || normalizedFilePath.startsWith("https://") || normalizedFilePath.contains("www.");
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

    public Optional<Path> findIn(BibDatabaseContext databaseContext, FilePreferences filePreferences) {
        List<Path> dirs = databaseContext.getFileDirectories(filePreferences);
        return findIn(dirs);
    }

    /**
     * Tries to find the file in the given directories and returns the path to the file (if found). Returns an empty
     * optional if the file cannot be found.
     */
    public Optional<Path> findIn(List<Path> directories) {
        try {
            if (link.get().isEmpty()) {
                // We do not want to match empty paths (which could be any file or none ?!)
                return Optional.empty();
            }

            Path file = Path.of(link.get());
            if (file.isAbsolute() || directories.isEmpty()) {
                if (Files.exists(file)) {
                    return Optional.of(file);
                } else {
                    return Optional.empty();
                }
            } else {
                return FileHelper.find(link.get(), directories);
            }
        } catch (InvalidPathException ex) {
            return Optional.empty();
        }
    }
}
