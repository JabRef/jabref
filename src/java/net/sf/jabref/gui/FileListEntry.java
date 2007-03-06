package net.sf.jabref.gui;

import net.sf.jabref.external.ExternalFileType;

/**
 * This class represents a file link for a Bibtex entry.
*/
public class FileListEntry {
    private String link;
    private String description;
    private ExternalFileType type;

    public FileListEntry(String description, String link, ExternalFileType type) {
        this.link = link;
        this.description = description;
        this.type = type;
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

    public ExternalFileType getType() {
        return type;
    }

    public void setType(ExternalFileType type) {
        this.type = type;
    }

    public String toString() {
        return description+" : "+link+" : "+type;
    }

}
