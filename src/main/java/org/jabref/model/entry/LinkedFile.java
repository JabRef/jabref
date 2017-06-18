package org.jabref.model.entry;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.FileDirectoryPreferences;
import org.jabref.model.util.FileHelper;

/**
 * Represents the link to an external file (e.g. associated PDF file).
 */
public class LinkedFile {

    private static final LinkedFile NULL_OBJECT = new LinkedFile("", "", "");
    private String description;
    private String link;
    private String fileType;

    public LinkedFile(String description, String link, String fileType) {
        this.description = Objects.requireNonNull(description);
        this.link = Objects.requireNonNull(link);
        this.fileType = Objects.requireNonNull(fileType);
    }
    public LinkedFile(String description, URL link, String fileType) {
        this(description, Objects.requireNonNull(link).toString(), fileType);
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
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
        return link.startsWith("http://") || link.startsWith("https://") || link.contains("www.");
    }

    public Optional<Path> findIn(BibDatabaseContext databaseContext, FileDirectoryPreferences fileDirectoryPreferences) {
        List<Path> dirs = databaseContext.getFileDirectoriesAsPaths(fileDirectoryPreferences);
        return findIn(dirs);
    }

    public Optional<Path> findIn(List<Path> directories) {
        Path file = Paths.get(link);
        if (file.isAbsolute() || directories.isEmpty()) {
            return Optional.of(file);
        } else {
            return FileHelper.expandFilenameAsPath(link, directories);
        }
    }
}
