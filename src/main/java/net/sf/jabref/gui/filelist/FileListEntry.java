package net.sf.jabref.gui.filelist;

import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.gui.externalfiletype.ExternalFileType;

/**
 * This class represents a file link for a Bibtex entry.
 */
public class FileListEntry {

    public String description;
    public String link;
    public Optional<ExternalFileType> type;

    public FileListEntry(String description, String link) {
        this(description, link, Optional.empty());
    }

    public FileListEntry(String description, String link, ExternalFileType type) {
        this.description = Objects.requireNonNull(description);
        this.link = Objects.requireNonNull(link);
        this.type = Optional.of(Objects.requireNonNull(type));
    }

    public FileListEntry(String description, String link, Optional<ExternalFileType> type) {
        this.description = Objects.requireNonNull(description);
        this.link = Objects.requireNonNull(link);
        this.type = Objects.requireNonNull(type);
    }

    public String[] getStringArrayRepresentation() {
        return new String[] {description, link, getTypeName()};
    }

    private String getTypeName() {
        return this.type.isPresent() ? this.type.get().getName() : "";
    }

    @Override
    public String toString() {
        return description + " : " + link + " : " + type.orElse(null);
    }
}
