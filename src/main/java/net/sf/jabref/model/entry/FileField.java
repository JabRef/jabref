package net.sf.jabref.model.entry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FileField {
    private static final FileField.ParsedFileField NULL_OBJECT = new FileField.ParsedFileField("", "", "");

    public static class ParsedFileField {
        public final String description;
        public final String link;
        public final String fileType;

        public ParsedFileField(String description, String link, String fileType) {
            this.description = Objects.requireNonNull(description);
            this.link = Objects.requireNonNull(link);
            this.fileType = Objects.requireNonNull(fileType);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || this.getClass() != o.getClass()) return false;

            FileField.ParsedFileField that = (FileField.ParsedFileField) o;

            if (this.description != null ? !this.description.equals(that.description) : that.description != null) return false;
            if (this.link != null ? !this.link.equals(that.link) : that.link != null) return false;
            return this.fileType != null ? this.fileType.equals(that.fileType) : that.fileType == null;

        }

        @Override
        public int hashCode() {
            int result = this.description != null ? this.description.hashCode() : 0;
            result = 31 * result + (this.link != null ? this.link.hashCode() : 0);
            result = 31 * result + (this.fileType != null ? this.fileType.hashCode() : 0);
            return result;
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

    public static List<FileField.ParsedFileField> parse(String value) {
        if(value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<FileField.ParsedFileField> files = new ArrayList<>();
        List<String> entry = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inXmlChar = false;
        boolean escaped = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!escaped && (c == '\\')) {
                escaped = true;
                continue;
            }
            // Check if we are entering an XML special character construct such
            // as "&#44;", because we need to know in order to ignore the semicolon.
            else if (!escaped && (c == '&') && !inXmlChar) {
                sb.append(c);
                if ((value.length() > (i + 1)) && (value.charAt(i + 1) == '#')) {
                    inXmlChar = true;
                }
            } else if (!escaped && inXmlChar && (c == ';')) {
                // Check if we are exiting an XML special character construct:
                sb.append(c);
                inXmlChar = false;
            } else if (!escaped && (c == ':')) {
                entry.add(sb.toString());
                sb = new StringBuilder();
            } else if (!escaped && (c == ';') && !inXmlChar) {
                entry.add(sb.toString());
                sb = new StringBuilder();

                files.add(convert(entry));
            } else {
                sb.append(c);
            }
            escaped = false;
        }
        if (sb.length() > 0) {
            entry.add(sb.toString());
        }

        if (!entry.isEmpty()) {
            files.add(convert(entry));
        }

        return files;
    }

    private static FileField.ParsedFileField convert(List<String> entry) {
        // ensure list has at least 3 fields
        while(entry.size() < 3) {
            entry.add("");
        }
        FileField.ParsedFileField field = new FileField.ParsedFileField(entry.get(0), entry.get(1), entry.get(2));
        entry.clear();
        return field;
    }
}
