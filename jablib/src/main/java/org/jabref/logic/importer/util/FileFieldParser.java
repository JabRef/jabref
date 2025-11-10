package org.jabref.logic.importer.util;

import java.net.MalformedURLException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.Range;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileFieldParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileFieldParser.class);

    private final String value;

    private StringBuilder charactersOfCurrentElement;

    private boolean windowsPath;

    private FileFieldParser(String value) {
        if (value == null) {
            this.value = null;
        } else {
            this.value = value.replace("$\\backslash$", "\\");
        }
    }

    /**
     * Converts the string representation of LinkedFileData to a List of LinkedFile
     * <p>
     * The syntax of one element is description:path:type
     * Multiple elements are concatenated with ;
     * <p>
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
        return fileFieldParser.parse().stream().map(LinkedFilePosition::linkedFile).toList();
    }

    public static Map<LinkedFile, Range> parseToPosition(String value) {
        FileFieldParser fileFieldParser = new FileFieldParser(value);
        return fileFieldParser.parse().stream().collect(HashMap::new, (map, position) -> map.put(position.linkedFile(), position.range()), HashMap::putAll);
    }

    private List<LinkedFilePosition> parse() {
        List<LinkedFilePosition> files = new ArrayList<>();

        if ((value == null) || value.trim().isEmpty()) {
            return files;
        }

        if (LinkedFile.isOnlineLink(value.trim())) {
            // needs to be modifiable
            try {
                return List.of(new LinkedFilePosition(new LinkedFile(URLUtil.create(value), ""), new Range(0, value.length() - 1)));
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
        int startColumn = 0;

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
                    // special case for zotero absolute path on windows that do not have a colon in front
                    // e.g. A:\zotero\paper.pdf
                } else if (charactersOfCurrentElement.length() == 1 && value.charAt(i + 1) == '\\') {
                    charactersOfCurrentElement.append(c);
                    windowsPath = true;
                } else {
                    // We are in the next LinkedFile data element
                    linkedFileData.add(charactersOfCurrentElement.toString());
                    resetDataStructuresForNextElement();
                }
            } else if (!escaped && (c == ';') && !inXmlChar) {
                linkedFileData.add(charactersOfCurrentElement.toString());
                files.add(new LinkedFilePosition(convert(linkedFileData), new Range(startColumn, i)));
                startColumn = i + 1;

                // next iteration
                resetDataStructuresForNextElement();
            } else {
                charactersOfCurrentElement.append(c);
            }
            escaped = false;
        }
        if (!charactersOfCurrentElement.isEmpty()) {
            linkedFileData.add(charactersOfCurrentElement.toString());
        }
        if (!linkedFileData.isEmpty()) {
            files.add(new LinkedFilePosition(convert(linkedFileData), new Range(startColumn, value.length() - 1)));
        }
        return files;
    }

    private void resetDataStructuresForNextElement() {
        charactersOfCurrentElement = new StringBuilder();
        windowsPath = false;
    }

    /**
     * Converts the given textual representation of a LinkedFile object
     * <p>
     * SIDE EFFECT: The given entry list is cleared upon completion
     * <p>
     * Expected format is: description:link:fileType:sourceURL
     * fileType is an {@link org.jabref.gui.externalfiletype.ExternalFileType}, which contains a name and a mime type
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
                field = new LinkedFile(entry.getFirst(), URLUtil.create(entry.get(1)), entry.get(2));
            } catch (MalformedURLException e) {
                // in case the URL is malformed, store it nevertheless
                field = new LinkedFile(entry.getFirst(), entry.get(1), entry.get(2));
            }
        } else {
            String pathStr = entry.get(1);
            if (pathStr.contains("//")) {
                // In case the path contains //, we assume it is a malformed URL, not a malformed path.
                // On linux, the double slash would be converted to a single slash.
                field = new LinkedFile(entry.getFirst(), pathStr, entry.get(2));
            } else {
                try {
                    // there is no Path.isValidPath(String) method
                    field = new LinkedFile(entry.getFirst(), Path.of(pathStr), entry.get(2));
                } catch (InvalidPathException e) {
                    // Ignored
                    LOGGER.debug("Invalid path object, continuing with string", e);
                    field = new LinkedFile(entry.getFirst(), pathStr, entry.get(2));
                }
            }
        }

        if (entry.size() > 3) {
            field.setSourceURL(entry.get(3));
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

    private record LinkedFilePosition(LinkedFile linkedFile, Range range) {
    }
}
