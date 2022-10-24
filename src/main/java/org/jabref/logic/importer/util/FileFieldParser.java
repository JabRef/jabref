package org.jabref.logic.importer.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jabref.model.entry.LinkedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileFieldParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileFieldParser.class);

    private final String value;

    private StringBuilder charactersOfCurrentElement;
    private boolean windowsPath;

    public FileFieldParser(String value) {
        this.value = value;
    }

    /**
     * Converts the string representation of LinkedFileData to a List of LinkedFile
     *
     * The syntax of one element is description:path:type
     * Multiple elements are concatenated with ;
     *
     * The main challenges of the implementation are:
     *
     * <ul>
     *     <li>that XML characters might be included (thus one cannot simply split on ";")</li>
     *     <li>some characters might be escaped</li>
     *     <li>Windows absolute paths might be included without escaping</li>
     * </ul>
     */
    public static List<LinkedFile> parse(String value) {
        // We need state to have a more clean code. Thus, we instantiate the class and then return the result
        FileFieldParser fileFieldParser = new FileFieldParser(value);
        return fileFieldParser.parse();
    }

    public List<LinkedFile> parse() {
        List<LinkedFile> files = new ArrayList<>();

        if ((value == null) || value.trim().isEmpty()) {
            return files;
        }

        if (LinkedFile.isOnlineLink(value.trim())) {
            // needs to be modifiable
            try {
                return List.of(new LinkedFile(new URL(value), ""));
            } catch (MalformedURLException e) {
                LOGGER.error("invalid url", e);
                return files;
            }
        }

        // data of each LinkedFile as split string
        List<String> linkedFileData = new ArrayList<>();

        resetDataStructuresForNextElement();
        boolean inXmlChar = false;
        boolean escaped = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!escaped && (c == '\\')) {
                if (windowsPath) {
                    charactersOfCurrentElement.append(c);
                    continue;
                } else {
                    escaped = true;
                    continue;
                }
            } else if (!escaped && (c == '&') && !inXmlChar) {
                // Check if we are entering an XML special character construct such
                // as "&#44;", because we need to know in order to ignore the semicolon.
                charactersOfCurrentElement.append(c);
                if ((value.length() > (i + 1)) && (value.charAt(i + 1) == '#')) {
                    inXmlChar = true;
                }
            } else if (!escaped && inXmlChar && (c == ';')) {
                // Check if we are exiting an XML special character construct:
                charactersOfCurrentElement.append(c);
                inXmlChar = false;
            } else if (!escaped && (c == ':')) {
                if ((linkedFileData.size() == 1) && // we already parsed the description
                        (charactersOfCurrentElement.length() == 1)) { // we parsed one character
                    // special case of Windows paths
                    // Example: ":c:\test.pdf:PDF"
                    // We are at the second : (position 3 in the example) and "just" add it to the current element
                    charactersOfCurrentElement.append(c);
                    windowsPath = true;
                } else {
                    // We are in the next LinkedFile data element
                    linkedFileData.add(charactersOfCurrentElement.toString());
                    resetDataStructuresForNextElement();
                }
            } else if (!escaped && (c == ';') && !inXmlChar) {
                linkedFileData.add(charactersOfCurrentElement.toString());
                files.add(convert(linkedFileData));

                // next iteration
                resetDataStructuresForNextElement();
            } else {
                charactersOfCurrentElement.append(c);
            }
            escaped = false;
        }
        if (charactersOfCurrentElement.length() > 0) {
            linkedFileData.add(charactersOfCurrentElement.toString());
        }
        if (!linkedFileData.isEmpty()) {
            files.add(convert(linkedFileData));
        }
        return files;
    }

    private void resetDataStructuresForNextElement() {
        charactersOfCurrentElement = new StringBuilder();
        windowsPath = false;
    }

    /**
     * Converts the given textual representation of a LinkedFile object
     *
     * SIDE EFFECT: The given entry list is cleared upon completion
     *
     * @param entry the list of elements in the linked file textual representation
     * @return a LinkedFile object
     */
    static LinkedFile convert(List<String> entry) {
        // ensure list has at least 3 fields
        while (entry.size() < 3) {
            entry.add("");
        }

        LinkedFile field = null;
        if (LinkedFile.isOnlineLink(entry.get(1))) {
            try {
                field = new LinkedFile(entry.get(0), new URL(entry.get(1)), entry.get(2));
            } catch (MalformedURLException e) {
                // in case the URL is malformed, store it nevertheless
                field = new LinkedFile(entry.get(0), entry.get(1), entry.get(2));
            }
        }

        if (field == null) {
            String pathStr = entry.get(1);
            if (pathStr.contains("//")) {
                // In case the path contains //, we assume it is a malformed URL, not a malformed path.
                // On linux, the double slash would be converted to a single slash.
                field = new LinkedFile(entry.get(0), pathStr, entry.get(2));
            } else {
                try {
                    // there is no Path.isValidPath(String) method
                    Path path = Path.of(pathStr);
                    field = new LinkedFile(entry.get(0), path, entry.get(2));
                } catch (InvalidPathException e) {
                    // Ignored
                    field = new LinkedFile(entry.get(0), pathStr, entry.get(2));
                }
            }
        }

        // link is the only mandatory field
        if (field.getDescription().isEmpty() && field.getLink().isEmpty() && !field.getFileType().isEmpty()) {
            field = new LinkedFile("", Path.of(field.getFileType()), "");
        } else if (!field.getDescription().isEmpty() && field.getLink().isEmpty() && field.getFileType().isEmpty()) {
            field = new LinkedFile("", Path.of(field.getDescription()), "");
        }
        entry.clear();
        return field;
    }
}
