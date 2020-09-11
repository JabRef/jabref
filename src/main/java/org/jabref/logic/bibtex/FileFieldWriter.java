package org.jabref.logic.bibtex;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.model.entry.LinkedFile;

public class FileFieldWriter {

    private FileFieldWriter() {
    }

    public static String getStringRepresentation(List<LinkedFile> fields) {
        String[][] array = new String[fields.size()][];
        int i = 0;
        for (LinkedFile entry : fields) {
            array[i] = new String[] {entry.getDescription(), entry.getLink(), entry.getFileType()};
            i++;
        }
        return encodeStringArray(array);
    }

    public static String getStringRepresentation(LinkedFile field) {
        return getStringRepresentation(Collections.singletonList(field));
    }

    /**
     * Encodes a two-dimensional String array into a single string, using ':' and
     * ';' as separators. The characters ':' and ';' are escaped with '\'.
     * @param values The String array.
     * @return The encoded String.
     */
    public static String encodeStringArray(String[][] values) {
        return Arrays.stream(values)
                     .map(FileFieldWriter::encodeStringArray)
                     .collect(Collectors.joining(";"));
    }

    /**
     * Encodes a String array into a single string, using ':' as separator.
     * The characters ':' and ';' are escaped with '\'.
     * @param entry The String array.
     * @return The encoded String.
     */
    private static String encodeStringArray(String[] entry) {
        return Arrays.stream(entry)
                     .map(FileFieldWriter::quote)
                     .collect(Collectors.joining(":"));
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
