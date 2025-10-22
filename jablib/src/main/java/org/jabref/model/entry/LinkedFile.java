package org.jabref.model.entry;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.strings.StringUtil;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents the link to an external file (e.g. associated PDF file).
 * This class is {@link Serializable} which is needed for drag and drop in gui.
 * <p>
 * Use static factory methods to create instances for better clarity:
 * <ul>
 *   <li>{@link #of(String, Path, String)} - for local file paths</li>
 *   <li>{@link #of(String, String, String)} - for string-based links</li>
 *   <li>{@link #of(URL, String)} - for online resources</li>
 *   <li>{@link #of(Path)} - for path without description</li>
 * </ul>
 * </p>
 */
@AllowedToUseLogic("Uses FileUtil from logic")
@NullMarked
public class LinkedFile implements Serializable {

    private static final String REGEX_URL = "^((?:https?\\:\\/\\/|www\\.)(?:[-a-z0-9]+\\.)*[-a-z0-9]+.*)";
    private static final Pattern URL_PATTERN = Pattern.compile(REGEX_URL);

    private static final LinkedFile NULL_OBJECT = of("", Path.of(""), "");

    // We have to mark these properties as transient because they can't be serialized directly
    private transient StringProperty description = new SimpleStringProperty();
    private transient StringProperty link = new SimpleStringProperty();
    // This field is a {@link StringProperty}, and not an {@link ObjectProperty<FileType>}, as {@link LinkedFile} might
    // be a URI, where a file type might not be present.
    private transient StringProperty fileType = new SimpleStringProperty();
    private transient StringProperty sourceURL = new SimpleStringProperty();

    /**
     * Private constructor - use static factory methods instead.
     * Constructor can also be used for non-valid paths. We need to parse them, because the GUI needs to render it.
     *
     * @param description the file description
     * @param link        the file link as string
     * @param fileType    the file type
     * @param sourceUrl   the source URL where the file was obtained
     */
    private LinkedFile(String description, String link, String fileType, String sourceUrl) {
        this.description.setValue(description);
        setLink(link);
        this.fileType.setValue(fileType);
        this.sourceURL.setValue(sourceUrl);
    }

    // Static Factory Methods

    /**
     * Creates a LinkedFile from a local file path.
     * This is the most common way to create a LinkedFile for local files.
     *
     * @param description the file description
     * @param link        the file path
     * @param fileType    the file type, e.g., "PDF", "TXT"
     * @return a new LinkedFile instance
     */
    public static LinkedFile of(String description, Path link, String fileType) {
        return new LinkedFile(description, link.toString(), fileType, "");
    }

    /**
     * Creates a LinkedFile from a local file path with a source URL.
     *
     * @param description the file description
     * @param link        the file path
     * @param fileType    the file type, e.g., "PDF", "TXT"
     * @param sourceUrl   the source URL where the file was obtained
     * @return a new LinkedFile instance
     */
    public static LinkedFile of(String description, Path link, String fileType, String sourceUrl) {
        return new LinkedFile(description, link.toString(), fileType, sourceUrl);
    }

    /**
     * Creates a LinkedFile from string representations.
     * This is useful when parsing file information from external sources.
     *
     * @param description the file description
     * @param link        the file link as string
     * @param fileType    the file type (as FileType object)
     * @return a new LinkedFile instance
     */
    public static LinkedFile of(String description, String link, FileType fileType) {
        return new LinkedFile(description, link, fileType.getName(), "");
    }

    /**
     * Creates a LinkedFile from string representations with source URL.
     * This is the most flexible factory method, accepting all parameters as strings.
     *
     * @param description the file description
     * @param link        the file link as string
     * @param fileType    the file type
     * @param sourceUrl   the source URL where the file was obtained
     * @return a new LinkedFile instance
     */
    public static LinkedFile of(String description, String link, String fileType, String sourceUrl) {
        return new LinkedFile(description, link, fileType, sourceUrl);
    }

    /**
     * Creates a LinkedFile from string representations without source URL.
     *
     * @param description the file description
     * @param link        the file link as string
     * @param fileType    the file type
     * @return a new LinkedFile instance
     */
    public static LinkedFile of(String description, String link, String fileType) {
        return new LinkedFile(description, link, fileType, "");
    }

    /**
     * Creates a LinkedFile from a URL without description.
     * Useful for online resources where no description is needed.
     *
     * @param link     the URL
     * @param fileType the file type, e.g., "URL", "HTML"
     * @return a new LinkedFile instance
     */
    public static LinkedFile of(URL link, String fileType) {
        return new LinkedFile("", link.toString(), fileType, "");
    }

    /**
     * Creates a LinkedFile from a URL with description.
     * Recommended for online resources that need a descriptive label.
     *
     * @param description the file description
     * @param link        the URL
     * @param fileType    the file type, e.g., "URL", "HTML"
     * @return a new LinkedFile instance
     */
    public static LinkedFile of(String description, URL link, String fileType) {
        return new LinkedFile(description, link.toString(), fileType, "");
    }

    /**
     * Creates a LinkedFile from a URL with description and source URL.
     *
     * @param description the file description
     * @param link        the URL
     * @param fileType    the file type, e.g., "URL", "HTML"
     * @param sourceUrl   the source URL where the file was obtained
     * @return a new LinkedFile instance
     */
    public static LinkedFile of(String description, URL link, String fileType, String sourceUrl) {
        return new LinkedFile(description, link.toString(), fileType, sourceUrl);
    }

    /**
     * Creates a LinkedFile with an empty file type and an empty description.
     * Useful for quick creation when only the path is known.
     *
     * @param link the file path
     * @return a new LinkedFile instance
     */
    public static LinkedFile of(Path link) {
        return new LinkedFile("", link.toString(), "", "");
    }

    // Properties

    public StringProperty descriptionProperty() {
        return description;
    }

    public StringProperty linkProperty() {
        return link;
    }

    public StringProperty fileTypeProperty() {
        return fileType;
    }

    public StringProperty sourceUrlProperty() {
        return sourceURL;
    }

    // Getters and Setters

    public String getFileType() {
        return fileType.get();
    }

    public void setFileType(String fileType) {
        this.fileType.setValue(fileType);
    }

    public void setFileType(FileType fileType) {
        this.setFileType(fileType.getName());
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

    public String getSourceUrl() {
        return sourceURL.get();
    }

    public void setSourceURL(String url) {
        this.sourceURL.setValue(url);
    }

    public Observable[] getObservables() {
        return new Observable[] {this.link, this.description, this.fileType, this.sourceURL};
    }

    // Serialization

    /**
     * Writes serialized object to ObjectOutputStream, automatically called
     */
    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(getFileType());
        out.writeUTF(getLink());
        out.writeUTF(getDescription());
        out.writeUTF(getSourceUrl());
        out.flush();
    }

    /**
     * Reads serialized object from {@link ObjectInputStream}, automatically called
     */
    @Serial
    private void readObject(ObjectInputStream in) throws IOException {
        fileType = new SimpleStringProperty(in.readUTF());
        link = new SimpleStringProperty(in.readUTF());
        description = new SimpleStringProperty(in.readUTF());
        sourceURL = new SimpleStringProperty(in.readUTF());
    }

    // Utility Methods

    /**
     * Checks if the given String is an online link
     *
     * @param toCheck The String to check
     * @return <code>true</code>, if it starts with "http://", "https://" or contains "www."; <code>false</code> otherwise
     */
    public static boolean isOnlineLink(String toCheck) {
        String normalizedFilePath = toCheck.trim().toLowerCase();
        return URL_PATTERN.matcher(normalizedFilePath).matches();
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

    /// Tries to locate the file.
    /// In case the path is absolute, the path is checked.
    /// In case the path is relative, the given directories are used as base directories.
    ///
    /// @return absolute path if found.
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
                return FileUtil.find(link.get(), directories);
            }
        } catch (InvalidPathException ex) {
            return Optional.empty();
        }
    }

    // Object Methods

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof LinkedFile that) {
            return Objects.equals(description.get(), that.description.get())
                    && Objects.equals(link.get(), that.link.get())
                    && Objects.equals(fileType.get(), that.fileType.get())
                    && Objects.equals(sourceURL.get(), that.sourceURL.get());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(description.get(), link.get(), fileType.get(), sourceURL.get());
    }

    @Override
    public String toString() {
        return "ParsedFileField{" +
                "description='" + description.get() + '\'' +
                ", link='" + link.get() + '\'' +
                ", fileType='" + fileType.get() + '\'' +
                (StringUtil.isNullOrEmpty(sourceURL.get()) ? "" : (", sourceUrl='" + sourceURL.get() + '\'')) +
                '}';
    }
}
