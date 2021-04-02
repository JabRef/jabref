package org.jabref.logic.openoffice;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.SortedSet;
import java.util.regex.Pattern;

import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parse a "*.jstyle" file
 */

class OOBibStyleParser {

    private static final String LAYOUT_MRK = "LAYOUT";
    private static final String PROPERTIES_MARK = "PROPERTIES";
    private static final String CITATION_MARK = "CITATION";
    private static final String NAME_MARK = "NAME";
    private static final String JOURNALS_MARK = "JOURNALS";
    private static final String DEFAULT_MARK = "default";

    private static final Pattern QUOTED = Pattern.compile("\".*\"");
    private static final Pattern NUM_PATTERN = Pattern.compile("-?\\d+");

    private static final Logger LOGGER = LoggerFactory.getLogger(OOBibStyleParser.class);

    /**
     *  Parse a *.jstyle file from {@code in}.
     *
     *  - Does not reset style, only adds things.
     */
    public static void readFormatFile(Reader in, OOBibStyle style) throws IOException {

        // First read all the contents of the file:
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            sb.append((char) c);
        }

        // Store a local copy for viewing
        style.localCopy = sb.toString();

        // Break into separate lines:
        String[] lines = sb.toString().split("\n");
        BibStyleMode mode = BibStyleMode.NONE;

        for (String line1 : lines) {
            String line = line1;

            // Drop "\r" from end of line
            if (!line.isEmpty()
                && (line.charAt(line.length() - 1) == '\r')) {
                line = line.substring(0, line.length() - 1);
            }

            // Skip empty line or comment:
            if (line.trim().isEmpty() || (line.charAt(0) == '#')) {
                continue;
            }

            // Check if we should change mode:
            switch (line) {
                case NAME_MARK:
                    mode = BibStyleMode.NAME;
                    continue;
                case LAYOUT_MRK:
                    mode = BibStyleMode.LAYOUT;
                    continue;
                case PROPERTIES_MARK:
                    mode = BibStyleMode.PROPERTIES;
                    continue;
                case CITATION_MARK:
                    mode = BibStyleMode.CITATION;
                    continue;
                case JOURNALS_MARK:
                    mode = BibStyleMode.JOURNALS;
                    continue;
                default:
                    break;
            }

            switch (mode) {
                case NAME:
                    if (!line.trim().isEmpty()) {
                        style.name = line.trim();
                    }
                    break;
                case LAYOUT:
                    handleLayoutLine(line, style);
                    break;
                case PROPERTIES:
                    handlePropertiesLine(line, style.properties);
                    break;
                case CITATION:
                    handlePropertiesLine(line, style.citProperties);
                    break;
                case JOURNALS:
                    handleJournalsLine(line, style.journals);
                    break;
                default:
                    break;
            }
        }

        // Set validity boolean based on whether we found anything interesting
        // in the file:
        if ((mode != BibStyleMode.NONE) && style.isDefaultLayoutPresent) {
            style.valid = true;
        }
    }

    /**
     * Parse a line providing bibliography structure information for an entry type.
     *
     * @param line The string containing the structure description.
     *
     * Expecting a line from below the "LAYOUT" tag in *.jstyle,
     * in "name=RHS" form. Silently ignores lines not matching this pattern.
     *
     * The "name" part is passed to {@code EntryTypeFactory.parse(name);}
     * The "RHS"  part is passed to {@code new LayoutHelper( ..., style.prefs).getLayoutFromText();}
     *
     */
    private static void handleLayoutLine(String line, OOBibStyle style) {
        /*
         * uses:
         *     style.prefs
         *     style.isDefaultLayoutPresent = x
         *     style.defaultBibLayout = x
         *     style.bibLayout.put()
         *
         */
        // "^([^=]+)[=](.+)$" With formatString = $2; name = $1;

        /*
         * Split the line at the first '='.
         * Do nothing if either half is empty, or "=" is not present.
         *
         * Note: maye should signal somehow that this line was not processed.
         *
         */
        int index = line.indexOf('=');
        if (index <= 0) {
            return; /* No "=" or line[0] == "=" */
        }
        if (index >= (line.length() - 1)) {
            return; /* First "=" is at the last character. */
        }

        String name = line.substring(0, index);
        String formatString = line.substring(index + 1);

        // Parse name: actually look up in a closed list, or
        // return {@code new UnknownEntryType(typeName)}
        EntryType type = EntryTypeFactory.parse(name);

        // Parse the formatString. Apparently does not depend on EntryType.
        StringReader reader = new StringReader(formatString);
        Layout layout;
        try {
            layout = new LayoutHelper(reader, style.prefs).getLayoutFromText();
        } catch (IOException ex) {
            LOGGER.warn("Cannot parse bibliography structure", ex);
            return;
        }

        /* At the first DEFAULT_MARK, put into defaultBibLayout, otherwise
         * add to bibLayout.
         *
         * Note: Adding the second DEFAULT_MARK to bibLayout may be unintended.
         *
         */
        if (!style.isDefaultLayoutPresent && name.equals(DEFAULT_MARK)) {
            style.isDefaultLayoutPresent = true;
            style.defaultBibLayout = layout;
        } else {
            style.bibLayout.put(type, layout);
        }
    }

    /**
     * Parse a line providing a property name and value.
     *
     * @param line The line containing the formatter names.
     */
    private static void handlePropertiesLine(String line, Map<String, Object> map) {
        int index = line.indexOf('=');
        if ((index > 0) && (index <= (line.length() - 1))) {
            String propertyName = line.substring(0, index).trim();
            String value = line.substring(index + 1);
            if ((value.trim().length() > 1) && QUOTED.matcher(value.trim()).matches()) {
                value = value.trim().substring(1, value.trim().length() - 1);
            }
            Object toSet = value;
            if (NUM_PATTERN.matcher(value).matches()) {
                toSet = Integer.parseInt(value);
            } else if ("true".equalsIgnoreCase(value.trim())) {
                toSet = Boolean.TRUE;
            } else if ("false".equalsIgnoreCase(value.trim())) {
                toSet = Boolean.FALSE;
            }
            map.put(propertyName, toSet);
        }
    }

    /**
     * Parse a line providing a journal name for which this style is valid.
     */
    private static void handleJournalsLine(String line, SortedSet<String> journals) {
        if (!line.trim().isEmpty()) {
            journals.add(line.trim());
        }
    }

    enum BibStyleMode {
        NONE,
        LAYOUT,
        PROPERTIES,
        CITATION,
        NAME,
        JOURNALS
    }

}
