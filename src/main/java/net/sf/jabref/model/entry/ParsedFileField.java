package net.sf.jabref.model.entry;

import java.util.Objects;

public class ParsedFileField {

    private static final ParsedFileField NULL_OBJECT = new ParsedFileField("", "", "");

    private final String description;
    private final String link;
    private final String fileType;

    public ParsedFileField(String description, String link, String fileType) {
        this.description = Objects.requireNonNull(description);
        this.link = Objects.requireNonNull(link);
        this.fileType = Objects.requireNonNull(fileType);
    }

    public String getFileType() {
        return fileType;
    }

    public String getDescription() {
        return description;
    }

    public String getLink() {
        return link;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof ParsedFileField) {

            ParsedFileField that = (ParsedFileField) o;

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
}
