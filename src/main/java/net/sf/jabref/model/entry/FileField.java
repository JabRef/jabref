package net.sf.jabref.model.entry;

import java.util.*;
import java.util.stream.Collectors;

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
            if (this == o) {
                return true;
            }
            if ((o == null) || (this.getClass() != o.getClass())) {
                return false;
            }

            FileField.ParsedFileField that = (FileField.ParsedFileField) o;

            if (!this.description.equals(that.description)) {
                return false;
            }
            if (!this.link.equals(that.link)) {
                return false;
            }
            return this.fileType.equals(that.fileType);
        }

        @Override
        public int hashCode() {
            int result = this.description.hashCode();
            result = (31 * result) + this.link.hashCode();
            result = (31 * result) + this.fileType.hashCode();
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
        if ((value == null) || value.trim().isEmpty()) {
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
        while (entry.size() < 3) {
            entry.add("");
        }
        FileField.ParsedFileField field = new FileField.ParsedFileField(entry.get(0), entry.get(1), entry.get(2));
        // link is only mandatory field
        if(field.description.isEmpty() && field.link.isEmpty() && !field.fileType.isEmpty()) {
            field = new ParsedFileField("", field.fileType, "");
        } else if(!field.description.isEmpty() && field.link.isEmpty() && field.fileType.isEmpty()) {
            field = new ParsedFileField("", field.description, "");
        }
        entry.clear();
        return field;
    }

    public static String getStringRepresentation(List<ParsedFileField> fields) {
        String[][] array = new String[fields.size()][];
        int i = 0;
        for (ParsedFileField entry : fields) {
            array[i] = new String[] {entry.description, entry.link, entry.fileType};
            i++;
        }
        return encodeStringArray(array);
    }

    public static String getStringRepresentation(ParsedFileField field) {
        return getStringRepresentation(Collections.singletonList(field));
    }

    /**
     * Encodes a two-dimensional String array into a single string, using ':' and
     * ';' as separators. The characters ':' and ';' are escaped with '\'.
     * @param values The String array.
     * @return The encoded String.
     */
    public static String encodeStringArray(String[][] values) {
        return Arrays.asList(values).stream().map(entry -> encodeStringArray(entry)).collect(Collectors.joining(";"));
    }

    /**
     * Encodes a String array into a single string, using ':' as separator.
     * The characters ':' and ';' are escaped with '\'.
     * @param entry The String array.
     * @return The encoded String.
     */
    private static String encodeStringArray(String[] entry) {
        return Arrays.asList(entry).stream().map(string -> quote(string)).collect(Collectors.joining(":"));
    }

    public static String quote(String s) {
        if (s == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == ';') || (c == ':') || (c == '\\')) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

}
