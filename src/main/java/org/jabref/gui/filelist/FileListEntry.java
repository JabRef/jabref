package org.jabref.gui.filelist;

import java.util.Objects;
import java.util.Optional;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.model.entry.LinkedFile;
/**
 * This class represents a file link for a Bibtex entry.
 */

public class FileListEntry {

    private String description;

    private String link;

    private Optional<ExternalFileType> type;

    public FileListEntry(String description, String link) {
        this(description, link, Optional.empty());
    }

    public FileListEntry(String description, String link, ExternalFileType type) {
        this.setDescription(Objects.requireNonNull(description));
        this.setLink(Objects.requireNonNull(link));
        this.setType(Optional.of(Objects.requireNonNull(type)));
    }

    public FileListEntry(String description, String link, Optional<ExternalFileType> type) {
        this.setDescription(Objects.requireNonNull(description));
        this.setLink(Objects.requireNonNull(link));
        this.setType(Objects.requireNonNull(type));
    }

    public String[] getStringArrayRepresentation() {
        return new String[] {getDescription(), getLink(), getTypeName()};
    }

    private String getTypeName() {
        return this.getType().isPresent() ? this.getType().get().getName() : "";
    }

    @Override
    public String toString() {
        return getDescription() + " : " + getLink() + " : " + getType().orElse(null);
    }

    public LinkedFile toParsedFileField() {
        return new LinkedFile(getDescription(), getLink(), getType().isPresent() ? getType().get().getName() : "");
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

    public Optional<ExternalFileType> getType() {
        return type;
    }

    public void setType(Optional<ExternalFileType> type) {
        this.type = type;
    }
}
