package org.jabref.logic.openoffice;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Matcher;
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

public class OOBibStyleParser {

    // Emit warning instead of error if possible.
    private static boolean patient = true;

    // Section names
    private static final String NAME_MARK = "NAME";
    private static final String JOURNALS_MARK = "JOURNALS";
    private static final String PROPERTIES_MARK = "PROPERTIES";
    private static final String CITATION_MARK = "CITATION";
    private static final String LAYOUT_MARK = "LAYOUT";

    // name of default layout
    private static final String DEFAULT_MARK = "default";

    private static final Pattern QUOTED = Pattern.compile("\".*\"");
    private static final Pattern NUM_PATTERN = Pattern.compile("-?\\d+");
    private static final Pattern LAYOUT_MULTILINE_STARTER =
        Pattern.compile("^\\s*([^\\s=]+)\\s*[=]\\s*[|](.*)[|]\\s*$");
    private static final Pattern LAYOUT_MULTILINE_CONTINUATION = Pattern.compile("^\\s*[|](.*)[|]\\s*$");

    private static final Set<String> SECTION_NAMES =
        Set.of(NAME_MARK, JOURNALS_MARK, PROPERTIES_MARK, CITATION_MARK, LAYOUT_MARK);

    /*
     * Keys in the PROPERTIES section that we warn about.
     */
    private static final Map<String,String> PROPERTY_WARNINGS = Map.of(
        // Appeared in setDefaultProperties, otherwise unknown.
        "SortAlgorithm", "SortAlgorithm is not used"
        );

    /*
     * Keys in the CITATION section we warn about.
     */
    private static final Map<String, String> CITATION_PROPERTY_WARNINGS = makeCitationPropertyWarnings();
    private static final Map<String,PropertyType> KNOWN_PROPERTIES = makeKnownProperties();
    private static final Map<String,PropertyType> KNOWN_CITATION_PROPERTIES =
        makeKnownCitationProperties();

    enum PropertyType {
        BOOL,
        INT,
        STRING,
        IGNORE // ignore silently, unless also in WARNINGS
    }

    private static boolean hasQuotes(String s) {
        return (s.length() >= 2) && QUOTED.matcher(s).matches();
    }

    private static String dropQuotes(String s) {
        if (hasQuotes(s)){
            return s.substring(1, s.length()-1);
        }
        return s;
    }

    /**
     * Add property with name {@code propertyName} to {@code destProperties}
     *
     * @param type The expected type of the value. Directs decoding
     * from the string {@code value}. The {@code IGNORE} type directs to silently skip
     * decoding and assignment.
     *
     * @param whatIsIt "property" or "citation property"..
     */
    private static ParseLogLevel addProperty(String propertyName,
                                             String value, // already trimmed
                                             PropertyType type,
                                             Map<String, Object> destProperties,
                                             String fileName,
                                             int lineNumber,
                                             ParseLog logger,
                                             String whatIsIt) {
        final String quotedTrue  = "\"true\"";
        final String quotedFalse = "\"false\"";

        switch (type) {
        case IGNORE:
            return ParseLogLevel.OK;
        case BOOL:
            switch (value) {
            case "true":
                destProperties.put(propertyName, Boolean.TRUE );
                return ParseLogLevel.OK;
            case "false":
                destProperties.put(propertyName, Boolean.FALSE );
                return ParseLogLevel.OK;
            default:
                String msg = String.format("Boolean %s '%s'"
                                           + " expects true or false as value, got '%s'",
                                           whatIsIt,
                                           propertyName, value);
                if (patient) {
                    if (value.equals(quotedTrue)){
                        destProperties.put(propertyName, Boolean.TRUE );
                        logger.warn(fileName, lineNumber, msg);
                        return ParseLogLevel.WARN;
                    }
                    if (value.equals(quotedFalse)){
                        destProperties.put(propertyName, Boolean.FALSE );
                        logger.warn(fileName, lineNumber, msg);
                        return ParseLogLevel.WARN;
                    }
                }
                logger.error(fileName, lineNumber, msg);
                return ParseLogLevel.ERROR;
            }

        case INT:
            if (NUM_PATTERN.matcher(value).matches()) {
                destProperties.put(propertyName, Integer.parseInt(value));
                return ParseLogLevel.OK;
            } else {
                String msg = String.format("Integer %s '%s'"
                                           + " expects number matching '-?[0-9]+' as value, got '%s'",
                                           whatIsIt,
                                           propertyName, value);
                if (patient) {
                    if (NUM_PATTERN.matcher(dropQuotes(value)).matches()) {
                        destProperties.put(propertyName, Integer.parseInt(dropQuotes(value)));
                        logger.warn(fileName, lineNumber, msg);
                        return ParseLogLevel.WARN;
                    }
                }
                logger.error(fileName, lineNumber, msg);
                return ParseLogLevel.ERROR;
            }

        case STRING:
            boolean isQuoted = hasQuotes(value);
            if (isQuoted) {
                destProperties.put(propertyName, dropQuotes(value));
                return ParseLogLevel.OK;
            } else {
                String msg = String.format("String %s '%s'"
                                           + " expects double quotes around value, got '%s'",
                                           whatIsIt,
                                           propertyName, value);
                if (patient) {
                    destProperties.put(propertyName, dropQuotes(value));
                    logger.warn(fileName, lineNumber, msg);
                    return ParseLogLevel.WARN;
                } else {
                    logger.error(fileName, lineNumber, msg);
                    return ParseLogLevel.ERROR;
                }
            }
        }
        throw new RuntimeException("");
    }

    private static Map<String, PropertyType> makeKnownProperties() {
        Map<String, PropertyType> res = new HashMap<String, PropertyType>();
        res.put("Title", PropertyType.STRING);
        res.put("IsNumberEntries", PropertyType.BOOL);
        res.put("IsSortByPosition", PropertyType.BOOL);
        res.put("ReferenceHeaderParagraphFormat", PropertyType.STRING);
        res.put("ReferenceParagraphFormat", PropertyType.STRING);
        return Collections.unmodifiableMap(res);
    }

    private static Map<String, String> makeCitationPropertyWarnings() {

        Map<String, String> res = new HashMap<String, String>();
        /* ItalicCitations was only recognized, but not used in JabRef5.2. */
        res.put("ItalicCitations", "ItalicCitations is not implemented");
        res.put("BoldCitations", "BoldCitations is not implemented");
        res.put("SuperscriptCitations", "SuperscriptCitations is not implemented");
        res.put("SubscriptCitations", "SubscriptCitations is not implemented");
        res.put("BibtexKeyCitations", "Found 'BibtexKeyCitations' instead of 'BibTeXKeyCitations'");
        return Collections.unmodifiableMap(res);
    }

    private static Map<String, PropertyType> makeKnownCitationProperties() {

        Map<String, PropertyType> res = new HashMap<String, PropertyType>();
        res.put("AuthorField", PropertyType.STRING);
        res.put("YearField", PropertyType.STRING);
        res.put("MaxAuthors", PropertyType.INT);
        res.put("MaxAuthorsFirst", PropertyType.INT);
        res.put("AuthorSeparator", PropertyType.STRING);
        res.put("AuthorLastSeparator", PropertyType.STRING);
        res.put("AuthorLastSeparatorInText", PropertyType.STRING);
        res.put("EtAlString", PropertyType.STRING);
        res.put("YearSeparator", PropertyType.STRING);
        res.put("InTextYearSeparator", PropertyType.STRING);
        res.put("BracketBefore", PropertyType.STRING);
        res.put("BracketAfter", PropertyType.STRING);
        res.put("BracketBeforeInList", PropertyType.STRING);
        res.put("BracketAfterInList", PropertyType.STRING);
        res.put("CitationSeparator", PropertyType.STRING);
        res.put("PageInfoSeparator", PropertyType.STRING);
        res.put("GroupedNumbersSeparator", PropertyType.STRING);
        res.put("MinimumGroupingCount", PropertyType.INT);
        res.put("FormatCitations", PropertyType.BOOL);
        res.put("CitationCharacterFormat", PropertyType.STRING);
        res.put("ItalicCitations", PropertyType.BOOL);
        res.put("BoldCitations", PropertyType.BOOL);
        res.put("SuperscriptCitations", PropertyType.BOOL);
        res.put("SubscriptCitations", PropertyType.BOOL);
        res.put("MultiCiteChronological", PropertyType.BOOL);
        res.put("BibTeXKeyCitations", PropertyType.BOOL); // BIBTEX_KEY_CITATIONS
        res.put("ItalicEtAl", PropertyType.BOOL);
        res.put("OxfordComma", PropertyType.STRING);
        res.put("UniquefierSeparator", PropertyType.STRING);
        return Collections.unmodifiableMap(res);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(OOBibStyleParser.class);

    private static boolean endsWithCharacter(String s, char c){
        return !s.isEmpty() && (s.charAt(s.length() - 1) == c);
    }

    private static String dropLastCharacter(String s) {
        return s.substring(0, s.length() - 1);
    }

    private static enum BibStyleMode {
        BEFORE_NAME_SECTION,
        IN_NAME_SECTION,
        BEFORE_JOURNALS_SECTION,
        IN_JOURNALS_SECTION,
        IN_PROPERTIES_SECTION,
        IN_CITATION_SECTION,
        IN_LAYOUT_SECTION,
        IN_LAYOUT_SECTION_MULTILINE
    }


    /** Also used as return code */
    static enum ParseLogLevel {
        ERROR,
        WARN,
        INFO,
        OK // no message
    }

    static class ParseLogEntry {
        public final ParseLogLevel level;
        public final String fileName;
        public final int lineNumber;
        public final String message;
        ParseLogEntry(ParseLogLevel level, String fileName, int lineNumber, String message) {
            this.level = level;
            this.fileName = fileName;
            this.lineNumber = lineNumber;
            this.message = message;
        }
        public String format() {
            StringBuilder sb = new StringBuilder();
            ParseLogEntry e = this;
            sb.append(e.fileName);
            sb.append(":");
            sb.append(e.lineNumber);
            sb.append(":");
            switch (e.level) {
            case ERROR:
                sb.append("error:");
                break;
            case WARN:
                sb.append("warning:");
                break;
            case INFO:
                sb.append("info:");
                break;
            case OK:
                sb.append("unexpected 'OK'");
                break;
            }
            sb.append(e.message);
            sb.append("\n");
            return sb.toString();
        }

        public String formatShort() {
            StringBuilder sb = new StringBuilder();
            ParseLogEntry e = this;
            sb.append("line");
            sb.append(":");
            sb.append(e.lineNumber);
            sb.append(":");
            switch (e.level) {
            case ERROR:
                sb.append("error:");
                break;
            case WARN:
                sb.append("warning:");
                break;
            case INFO:
                sb.append("info:");
                break;
            case OK:
                sb.append("unexpected 'OK'");
                break;
            }
            sb.append(e.message);
            sb.append("\n");
            return sb.toString();
        }
    }

    public static class ParseLog {
        List<ParseLogEntry> entries;
        ParseLog() {
            this.entries = new ArrayList<>();
        }
        void log( ParseLogLevel level, String fileName, int lineNumber, String message) {
            this.entries.add(new ParseLogEntry(level, fileName, lineNumber, message));
        }
        void error(String fileName, int lineNumber, String message) {
            this.entries.add(new ParseLogEntry(ParseLogLevel.ERROR, fileName, lineNumber, message));
        }
        void warn(String fileName, int lineNumber, String message) {
            this.entries.add(new ParseLogEntry(ParseLogLevel.WARN, fileName, lineNumber, message));
        }
        void info(String fileName, int lineNumber, String message) {
            this.entries.add(new ParseLogEntry(ParseLogLevel.INFO, fileName, lineNumber, message));
        }

        public int size() {
            return entries.size();
        }

        public boolean isEmpty() {
            return entries.isEmpty();
        }

        public String format(){
            StringBuilder sb = new StringBuilder();
            for (ParseLogEntry e : entries) {
                sb.append(e.format());
            }
            return sb.toString();
        }

        // omits file path
        public String formatShort(){
            StringBuilder sb = new StringBuilder();
            for (ParseLogEntry e : entries) {
                sb.append(e.formatShort());
            }
            return sb.toString();
        }

        public boolean hasError() {
            for (ParseLogEntry e : entries) {
                if (e.level == ParseLogLevel.ERROR) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     *  Parse a *.jstyle file from {@code in}.
     *
     *  - Does not reset style, only adds things.
     *
     *  - Expects a fixed order of sections (NAME,JOURNALS,PROPERTIES,CITATION,LAYOUT)
     *
     *  - Only known properties and citation properties are accepted, and only with the
     *    type defined in KNOWN_PROPERTIES and KNOWN_CITATION_PROPERTIES.
     *  - boolean and integer values must not be quoted, strings must.
     *  - Unparsable lines are not ignored.
     *
     * - To reduce friction, (OOBibStyleParser.patient == true) turns
     *   "unparsable lines", "extra quotes", "missing quotes", "unknown property"
     *   into a warning (instead of error).
     *
     * - To avoid the necessity of long lines in the LAYOUT section,
     *   if the RHS matches "|.*|", than we switch to multiline LAYOUT mode
     *   for the given entry, and collect lines matching to "|.*|" into the value.
     *   We expect an empty line to terminate the multiline rule.
     *   The "|" characters are stripped and the rest are concatenated before
     *   using the value.
     *
     */
    public static ParseLog readFormatFile(Reader in, OOBibStyle style, String fileName)
        throws
        IOException {

        ParseLog logger = new ParseLog();

        // First read all the contents of the file:
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            sb.append((char) c);
        }

        // Store a local copy for viewing
        style.localCopy = sb.toString();

        // Add EOL and a fake an empty line at the end. In case
        // IN_LAYOUT_SECTION_MULTILINE is terminated by EOF,
        // this will trigger closing.
        // Break into separate lines:
        sb.append("\n \n ");
        String[] lines = sb.toString().split("\n");

        BibStyleMode mode = BibStyleMode.BEFORE_NAME_SECTION;
        int lineNumber = 0;

        // For multiline LAYOUT rules
        int layoutLineCollectorStartLine = -1;
        String layoutLineCollectorName = "***";
        List<String> layoutLineCollectorValue = new ArrayList<>();

        for (String line1 : lines) {
            String line = line1;
            lineNumber++;

            // Drop "\r" from end of line
            if (endsWithCharacter(line, '\r')) {
                line = dropLastCharacter(line);
            }

            final String trimmedLine = line.trim();

            // Skip empty lines, unless we are in IN_LAYOUT_SECTION_MULTILINE
            if (trimmedLine.isEmpty() && (mode != BibStyleMode.IN_LAYOUT_SECTION_MULTILINE) ) {
                continue;
            }

            // Skip comment:
            if (line.length() > 0 && line.charAt(0) == '#') {
                continue;
            }

            /* We only get here if we do have something (not empty line or comment) */
            switch (mode) {

            case BEFORE_NAME_SECTION:
                switch (trimmedLine) {
                case NAME_MARK:
                    mode = BibStyleMode.IN_NAME_SECTION;
                    continue;
                default:
                    logger.error(fileName, lineNumber,
                                 String.format("Expected \"%s\", got \"%s\"", NAME_MARK, line));
                    return logger;
                }

            case IN_NAME_SECTION:
                if (SECTION_NAMES.contains(trimmedLine)) {
                    logger.error( fileName, lineNumber,
                                  "Expected name of style, found section name"
                                  + String.format("'%s'", trimmedLine ));
                    return logger;
                } else {
                    // ok
                    style.obsName = trimmedLine;
                    mode = BibStyleMode.BEFORE_JOURNALS_SECTION;
                    continue;
                }

            case BEFORE_JOURNALS_SECTION:
                switch (trimmedLine) {
                case JOURNALS_MARK:
                    mode = BibStyleMode.IN_JOURNALS_SECTION;
                    continue;
                default:
                    String msg = String.format("Expected \"%s\", got \"%s\"", JOURNALS_MARK, line);
                    if (patient) {
                        logger.warn(fileName, lineNumber, msg);
                        style.obsName = trimmedLine; // mimic old behaviour
                        continue;
                    } else {
                        logger.error(fileName, lineNumber,
                                     String.format("Expected \"%s\", got \"%s\"", JOURNALS_MARK, line));
                        return logger;
                    }
                }

            case IN_JOURNALS_SECTION:
                if (SECTION_NAMES.contains(trimmedLine)) {
                    if (trimmedLine.equals(PROPERTIES_MARK)) {
                        mode = BibStyleMode.IN_PROPERTIES_SECTION;
                        continue;
                    } else {
                        logger.error(fileName, lineNumber,
                                     String.format("Expected journal name or '%s',"
                                                   + " found section name '%s'",
                                                   PROPERTIES_MARK,
                                                   trimmedLine));
                        return logger;
                    }
                } else {
                    style.journals.add( trimmedLine );
                    continue;
                }

            case IN_PROPERTIES_SECTION:
                if (SECTION_NAMES.contains(trimmedLine)) {
                    if (trimmedLine.equals(CITATION_MARK)) {
                        mode = BibStyleMode.IN_CITATION_SECTION;
                        continue;
                    } else {
                        logger.error(fileName, lineNumber,
                                     String.format("Expected property setting or '%s',"
                                                   + " found section name '%s'",
                                                    CITATION_MARK,
                                                   trimmedLine));
                        return logger;
                    }
                } else {
                    ParseLogLevel res = handlePropertiesLine(trimmedLine,
                                                             fileName,
                                                             lineNumber,
                                                             style.obsProperties,
                                                             PROPERTY_WARNINGS,
                                                             KNOWN_PROPERTIES,
                                                             logger,
                                                             "property");
                    if (res == ParseLogLevel.ERROR) {
                        return logger;
                    } else {
                        continue;
                    }
                }

            case IN_CITATION_SECTION:
                if (SECTION_NAMES.contains(trimmedLine)) {
                    if (trimmedLine.equals(LAYOUT_MARK)) {
                        mode = BibStyleMode.IN_LAYOUT_SECTION;
                        continue;
                    } else {
                        logger.error(fileName, lineNumber,
                                     String.format("Expected citation property setting or '%s',"
                                                   + " found section name '%s'",
                                                   LAYOUT_MARK,
                                                   trimmedLine));
                        return logger;
                    }
                } else {
                    ParseLogLevel res = handlePropertiesLine(trimmedLine,
                                                             fileName,
                                                             lineNumber,
                                                             style.obsCitProperties,
                                                             CITATION_PROPERTY_WARNINGS,
                                                             KNOWN_CITATION_PROPERTIES,
                                                             logger,
                                                             "citation property");
                    if (res == ParseLogLevel.ERROR) {
                        return logger;
                    } else {
                        continue;
                    }
                }

            case IN_LAYOUT_SECTION:
                Matcher ms = LAYOUT_MULTILINE_STARTER.matcher(line);
                if (ms.find()) {
                    layoutLineCollectorStartLine = lineNumber;
                    layoutLineCollectorName = ms.group(1);
                    layoutLineCollectorValue = new ArrayList<>();
                    layoutLineCollectorValue.add(ms.group(2));
                    mode = BibStyleMode.IN_LAYOUT_SECTION_MULTILINE;
                    continue;
                } else {
                    ParseLogLevel res = handleLayoutLine(line, style, fileName, lineNumber, logger);
                    if (res == ParseLogLevel.ERROR) {
                        return logger;
                    } else {
                        continue;
                    }
                }
            case IN_LAYOUT_SECTION_MULTILINE:
                Matcher mc = LAYOUT_MULTILINE_CONTINUATION.matcher(line);
                if (mc.find()) {
                    layoutLineCollectorValue.add(mc.group(1));
                    continue;
                } else if (trimmedLine.equals("")) {
                    ParseLogLevel res = handleLayoutLineParts(layoutLineCollectorName,
                                                              String.join("", layoutLineCollectorValue),
                                                              style,
                                                              fileName,
                                                              layoutLineCollectorStartLine,
                                                              logger );

                    layoutLineCollectorName = "***";
                    layoutLineCollectorValue = new ArrayList<>();
                    layoutLineCollectorStartLine = -1;

                    mode = BibStyleMode.IN_LAYOUT_SECTION;

                    if (res == ParseLogLevel.ERROR) {
                        return logger;
                    } else {
                        continue;
                    }
                } else {
                    logger.error(fileName,
                                 lineNumber,
                                 "line is neither empty, nor |.*|"
                                 + " while expecting multiline LAYOUT rule continuation.");
                    return logger;
                }

            default:
                throw new RuntimeException("Unexpected mode in OOBibStyleParser.readFormatFile");
            }
        }

        // Set validity boolean based on whether we found every section
        // in the file.
        if (mode == BibStyleMode.IN_LAYOUT_SECTION_MULTILINE) {
            logger.error(fileName, lineNumber, "Reached end of file inside a multiline LAYOUT rule.");
            return logger;
        }
        if (mode != BibStyleMode.IN_LAYOUT_SECTION && mode != BibStyleMode.IN_LAYOUT_SECTION_MULTILINE) {
            logger.error(fileName, lineNumber, "Did not reach LAYOUT section at EOF");
            return logger;
        }
        if (!style.isDefaultLayoutPresent) {
            logger.error(fileName, lineNumber, "File did not provide a \"default\" layout.");
            return logger;
        }
        style.valid = true;
        return logger;
    }

    /**
     * Parse a line providing a property name and value.
     *
     * @param line The line containing the formatter names.
     *
     * Format: "{propertyName}={value}"
     */
    private static ParseLogLevel handlePropertiesLine(String trimmedLine,
                                                      String fileName,
                                                      int lineNumber,
                                                      Map<String, Object> properties,
                                                      Map<String, String> WARNINGS,
                                                      Map<String, PropertyType> KNOWN,
                                                      ParseLog logger,
                                                      String whatIsIt) {
        int index = trimmedLine.indexOf('=');
        ParseLogLevel softError = (patient ? ParseLogLevel.WARN : ParseLogLevel.ERROR);
        if (index < 0) {
            logger.log(softError,
                       fileName, lineNumber,
                       String.format("Expected %s setting,"
                                     + " but the line does not contain '='",
                                     whatIsIt));
            return softError;
        }

        String propertyName = trimmedLine.substring(0, index).trim();
        String value = trimmedLine.substring(index + 1).trim();

        if ("".equals(propertyName)){
            logger.log(softError, fileName, lineNumber,
                       String.format("Empty %s name", whatIsIt));
            return softError;
        }

        if (WARNINGS.containsKey(propertyName)) {
            String msg = WARNINGS.get(propertyName);
            // LOGGER.warn(msg);
            logger.warn(fileName, lineNumber, msg);
            // Do not return yet. Warning does not preclude using the value.
            // return ParseLogLevel.WARN;
        }

        PropertyType type = KNOWN.get(propertyName);
        if (type == null) {
            String msg = String.format("Unknown %s: '%s'", whatIsIt, propertyName);
            logger.log(softError, fileName, lineNumber, msg);
            return softError;
        } else {
            ParseLogLevel res = addProperty(propertyName,
                                            value,
                                            type,
                                            properties,
                                            fileName, lineNumber, logger, whatIsIt);
            return res;
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
     * See
     * https://docs.jabref.org/collaborative-work/export/customexports
     * for a description of what can go into the RHS.
     */
    private static ParseLogLevel handleLayoutLine(String line,
                                                  OOBibStyle style,
                                                  String fileName,
                                                  int lineNumber,
                                                  ParseLog logger) {
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
        if (index < 0) {
            logger.error(fileName, lineNumber,
                         "Expected format definition,"
                         + " but the line does not contain '='");
            return ParseLogLevel.ERROR;
        }

        if (index == 0) {
            logger.error(fileName, lineNumber,
                         "Expected entry type name or default,"
                         + " but the line is empty before '='");
            return ParseLogLevel.ERROR;
        }

        if (index >= (line.length() - 1)) {
            logger.error(fileName, lineNumber,
                         "Expected entry layout definition,"
                         + " but the line is empty after '='");
            return ParseLogLevel.ERROR;
        }

        String name = line.substring(0, index);
        String formatString = line.substring(index + 1);
        return handleLayoutLineParts(name,
                                     formatString,
                                     style,
                                     fileName,
                                     lineNumber,
                                     logger );
    }

    private static ParseLogLevel handleLayoutLineParts(String name,
                                                       String formatString,
                                                       OOBibStyle style,
                                                       String fileName,
                                                       int lineNumber,
                                                       ParseLog logger) {

        // Parse name: actually look up in a closed list, or
        // return {@code new UnknownEntryType(typeName)}
        EntryType type = EntryTypeFactory.parse(name);

        // Parse the formatString. Apparently does not depend on EntryType.
        StringReader reader = new StringReader(formatString);
        Layout layout;
        try {
            layout = new LayoutHelper(reader, style.prefs).getLayoutFromText();
        } catch (IOException ex) {
            // LOGGER.warn("Cannot parse bibliography structure", ex);
            String msg = String.format("Cannot parse bibliography structure. %s", ex.getMessage());
            logger.error(fileName, lineNumber, msg);
            return ParseLogLevel.ERROR;
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
        return ParseLogLevel.OK;
    }
}
