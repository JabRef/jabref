package org.jabref.logic.openoffice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class embodies a bibliography formatting for OpenOffice, which is composed
 * of the following elements:
 * <p>
 * 1) Each OO BIB entry type must have a formatting. A formatting is an array of elements, each
 * of which is either a piece of constant text, an entry field value, or a tab. Each element has
 * a character format associated with it.
 * <p>
 * 2) Many field values (e.g. author) need to be formatted before input to OpenOffice. The style
 * has the responsibility of formatting all field values. Formatting is handled by 0-n
 * JabRef LayoutFormatter classes.
 * <p>
 * 3) If the entries are not numbered, a citation marker must be produced for each entry. This
 * operation is performed for each JabRef BibEntry.
 */
public class OOBibStyle implements Comparable<OOBibStyle> {

    public static final String ITALIC_ET_AL = "ItalicEtAl";
    public static final String MULTI_CITE_CHRONOLOGICAL = "MultiCiteChronological";
    public static final String MINIMUM_GROUPING_COUNT = "MinimumGroupingCount";
    public static final String ET_AL_STRING = "EtAlString";
    public static final String MAX_AUTHORS_FIRST = "MaxAuthorsFirst";
    public static final String REFERENCE_HEADER_PARAGRAPH_FORMAT = "ReferenceHeaderParagraphFormat";
    public static final String REFERENCE_PARAGRAPH_FORMAT = "ReferenceParagraphFormat";

    public static final String TITLE = "Title";
    public static final String UNDEFINED_CITATION_MARKER = "??";
    private static final Pattern NUM_PATTERN = Pattern.compile("-?\\d+");
    private static final String LAYOUT_MRK = "LAYOUT";
    private static final String PROPERTIES_MARK = "PROPERTIES";
    private static final String CITATION_MARK = "CITATION";
    private static final String NAME_MARK = "NAME";
    private static final String JOURNALS_MARK = "JOURNALS";
    private static final String DEFAULT_MARK = "default";
    private static final String BRACKET_AFTER_IN_LIST = "BracketAfterInList";
    private static final String BRACKET_BEFORE_IN_LIST = "BracketBeforeInList";
    private static final String UNIQUEFIER_SEPARATOR = "UniquefierSeparator";
    private static final String BIBTEX_KEY_CITATIONS = "BibTeXKeyCitations";
    private static final String SUBSCRIPT_CITATIONS = "SubscriptCitations";
    private static final String SUPERSCRIPT_CITATIONS = "SuperscriptCitations";
    private static final String BOLD_CITATIONS = "BoldCitations";
    private static final String ITALIC_CITATIONS = "ItalicCitations";
    private static final String CITATION_CHARACTER_FORMAT = "CitationCharacterFormat";
    private static final String FORMAT_CITATIONS = "FormatCitations";
    private static final String GROUPED_NUMBERS_SEPARATOR = "GroupedNumbersSeparator";
    private static final String PAGE_INFO_SEPARATOR = "PageInfoSeparator";
    private static final String CITATION_SEPARATOR = "CitationSeparator";
    private static final String IN_TEXT_YEAR_SEPARATOR = "InTextYearSeparator";
    private static final String MAX_AUTHORS = "MaxAuthors";
    private static final String YEAR_FIELD = "YearField";
    private static final String AUTHOR_FIELD = "AuthorField";
    private static final String BRACKET_AFTER = "BracketAfter";
    private static final String BRACKET_BEFORE = "BracketBefore";
    private static final String IS_NUMBER_ENTRIES = "IsNumberEntries";
    private static final String IS_SORT_BY_POSITION = "IsSortByPosition";
    private static final String SORT_ALGORITHM = "SortAlgorithm";
    private static final String OXFORD_COMMA = "OxfordComma";
    private static final String YEAR_SEPARATOR = "YearSeparator";
    private static final String AUTHOR_LAST_SEPARATOR_IN_TEXT = "AuthorLastSeparatorInText";
    private static final String AUTHOR_LAST_SEPARATOR = "AuthorLastSeparator";

    private static final String AUTHOR_SEPARATOR = "AuthorSeparator";

    private static final Pattern QUOTED = Pattern.compile("\".*\"");

    private static final Logger LOGGER = LoggerFactory.getLogger(OOBibStyle.class);

    private String name = "";

    private final SortedSet<String> journals = new TreeSet<>();
    // Formatter to be run on fields before they are used as part of citation marker:
    private final LayoutFormatter fieldFormatter = new OOPreFormatter();

    private Layout defaultBibLayout;

    // reference layout mapped from entry type number:
    private final Map<String, Layout> bibLayout = new HashMap<>();

    private final Map<String, Object> properties = new HashMap<>();

    private final Map<String, Object> citProperties = new HashMap<>();

    private boolean valid;
    private final boolean fromResource;
    private final String path;

    enum BibStyleMode {
        NONE,
        LAYOUT,
        PROPERTIES,
        CITATION,
        NAME,
        JOURNALS
        }

    private File styleFile;
    private final Charset encoding;
    private long styleFileModificationTime = Long.MIN_VALUE;
    private String localCopy;
    private final LayoutFormatterPreferences prefs;


    public OOBibStyle(File styleFile, LayoutFormatterPreferences prefs,
            Charset encoding) throws IOException {
        this.prefs = Objects.requireNonNull(prefs);
        this.styleFile = Objects.requireNonNull(styleFile);
        this.encoding = Objects.requireNonNull(encoding);
        setDefaultProperties();
        reload();
        fromResource = false;
        path = styleFile.getPath();
    }

    public OOBibStyle(String resourcePath, LayoutFormatterPreferences prefs) throws IOException {
        this.prefs = Objects.requireNonNull(prefs);
        Objects.requireNonNull(resourcePath);
        this.encoding = StandardCharsets.UTF_8;
        setDefaultProperties();
        initialize(OOBibStyle.class.getResourceAsStream(resourcePath));
        fromResource = true;
        path = resourcePath;
    }

    private void setDefaultProperties() {
        // Set default property values:
        properties.put(TITLE, "Bibliography");
        properties.put(SORT_ALGORITHM, "alphanumeric");
        properties.put(IS_SORT_BY_POSITION, Boolean.FALSE);
        properties.put(IS_NUMBER_ENTRIES, Boolean.FALSE);
        properties.put(BRACKET_BEFORE, "[");
        properties.put(BRACKET_AFTER, "]");
        properties.put(REFERENCE_PARAGRAPH_FORMAT, "Default");
        properties.put(REFERENCE_HEADER_PARAGRAPH_FORMAT, "Heading 1");

        // Set default properties for the citation marker:
        citProperties.put(AUTHOR_FIELD, FieldName.orFields(FieldName.AUTHOR, FieldName.EDITOR));
        citProperties.put(YEAR_FIELD, FieldName.YEAR);
        citProperties.put(MAX_AUTHORS, 3);
        citProperties.put(MAX_AUTHORS_FIRST, -1);
        citProperties.put(AUTHOR_SEPARATOR, ", ");
        citProperties.put(AUTHOR_LAST_SEPARATOR, " & ");
        citProperties.put(AUTHOR_LAST_SEPARATOR_IN_TEXT, null);
        citProperties.put(ET_AL_STRING, " et al.");
        citProperties.put(YEAR_SEPARATOR, ", ");
        citProperties.put(IN_TEXT_YEAR_SEPARATOR, " ");
        citProperties.put(BRACKET_BEFORE, "(");
        citProperties.put(BRACKET_AFTER, ")");
        citProperties.put(CITATION_SEPARATOR, "; ");
        citProperties.put(PAGE_INFO_SEPARATOR, "; ");
        citProperties.put(GROUPED_NUMBERS_SEPARATOR, "-");
        citProperties.put(MINIMUM_GROUPING_COUNT, 3);
        citProperties.put(FORMAT_CITATIONS, Boolean.FALSE);
        citProperties.put(CITATION_CHARACTER_FORMAT, "Default");
        citProperties.put(ITALIC_CITATIONS, Boolean.FALSE);
        citProperties.put(BOLD_CITATIONS, Boolean.FALSE);
        citProperties.put(SUPERSCRIPT_CITATIONS, Boolean.FALSE);
        citProperties.put(SUBSCRIPT_CITATIONS, Boolean.FALSE);
        citProperties.put(MULTI_CITE_CHRONOLOGICAL, Boolean.TRUE);
        citProperties.put(BIBTEX_KEY_CITATIONS, Boolean.FALSE);
        citProperties.put(ITALIC_ET_AL, Boolean.FALSE);
        citProperties.put(OXFORD_COMMA, "");
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public File getFile() {
        return styleFile;
    }

    public Set<String> getJournals() {
        return Collections.unmodifiableSet(journals);
    }

    private void initialize(InputStream stream) throws IOException {
        Objects.requireNonNull(stream);

        try (Reader reader = new InputStreamReader(stream, encoding)) {
            readFormatFile(reader);
        }
    }

    /**
     * If this style was initialized from a file on disk, reload the style
     * if the file has been modified since it was read.
     *
     * @throws IOException
     */
    public void ensureUpToDate() throws IOException {
        if (!isUpToDate()) {
            reload();
        }
    }

    /**
     * If this style was initialized from a file on disk, reload the style
     * information.
     *
     * @throws IOException
     */
    private void reload() throws IOException {
        if (styleFile != null) {
            this.styleFileModificationTime = styleFile.lastModified();
            try (InputStream stream = new FileInputStream(styleFile)) {
                initialize(stream);
            }
        }
    }

    /**
     * If this style was initialized from a file on disk, check whether the file
     * is unmodified since initialization.
     *
     * @return true if the file has not been modified, false otherwise.
     */
    private boolean isUpToDate() {
        if (styleFile == null) {
            return true;
        } else {
            return styleFile.lastModified() == this.styleFileModificationTime;
        }
    }

    private void readFormatFile(Reader in) throws IOException {

        // First read all the contents of the file:
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            sb.append((char) c);
        }

        // Store a local copy for viewing
        localCopy = sb.toString();

        // Break into separate lines:
        String[] lines = sb.toString().split("\n");
        BibStyleMode mode = BibStyleMode.NONE;

        for (String line1 : lines) {
            String line = line1;
            if (!line.isEmpty() && (line.charAt(line.length() - 1) == '\r')) {
                line = line.substring(0, line.length() - 1);
            }
            // Check for empty line or comment:
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
                    name = line.trim();
                }
                break;
            case LAYOUT:
                handleStructureLine(line);
                break;
            case PROPERTIES:
                handlePropertiesLine(line, properties);
                break;
            case CITATION:
                handlePropertiesLine(line, citProperties);
                break;
            case JOURNALS:
                handleJournalsLine(line);
                break;
            default:
                break;
            }

        }

        // Set validity boolean based on whether we found anything interesting
        // in the file:
        if (mode != BibStyleMode.NONE) {
            valid = true;
        }

    }

    /**
     * After initializing this style from a file, this method can be used to check
     * whether the file appeared to be a proper style file.
     *
     * @return true if the file could be parsed as a style file, false otherwise.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Parse a line providing bibliography structure information for an entry type.
     *
     * @param line The string containing the structure description.
     * @throws IOException
     */
    private void handleStructureLine(String line) {
        int index = line.indexOf('=');
        if ((index > 0) && (index < (line.length() - 1))) {
            String formatString = line.substring(index + 1);
            boolean setDefault = line.substring(0, index).equals(OOBibStyle.DEFAULT_MARK);
            String type = line.substring(0, index);
            try {
                Layout layout = new LayoutHelper(new StringReader(formatString), this.prefs).getLayoutFromText();
                if (setDefault) {
                    defaultBibLayout = layout;
                } else {
                    bibLayout.put(type.toLowerCase(Locale.ENGLISH), layout);
                }

            } catch (IOException ex) {
                LOGGER.warn("Cannot parse bibliography structure", ex);
            }
        }
    }

    /**
     * Parse a line providing a property name and value.
     *
     * @param line The line containing the formatter names.
     * @throws IOException
     */
    private void handlePropertiesLine(String line, Map<String, Object> map) {
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
     *
     * @param line
     * @throws IOException
     */
    private void handleJournalsLine(String line) {
        if (!line.trim().isEmpty()) {
            journals.add(line.trim());
        }
    }

    public Layout getReferenceFormat(String type) {
        Layout l = bibLayout.get(type.toLowerCase(Locale.ENGLISH));
        if (l == null) {
            return defaultBibLayout;
        } else {
            return l;
        }
    }

    /**
     * Format a number-based citation marker for the given number.
     *
     * @param number The citation numbers.
     * @return The text for the citation.
     */
    public String getNumCitationMarker(List<Integer> number, int minGroupingCount, boolean inList) {
        String bracketBefore = getStringCitProperty(BRACKET_BEFORE);
        if (inList && (citProperties.containsKey(BRACKET_BEFORE_IN_LIST))) {
            bracketBefore = getStringCitProperty(BRACKET_BEFORE_IN_LIST);
        }
        String bracketAfter = getStringCitProperty(BRACKET_AFTER);
        if (inList && (citProperties.containsKey(BRACKET_AFTER_IN_LIST))) {
            bracketAfter = getStringCitProperty(BRACKET_AFTER_IN_LIST);
        }
        // Sort the numbers:
        List<Integer> lNum = new ArrayList<>(number);
        Collections.sort(lNum);
        StringBuilder sb = new StringBuilder(bracketBefore);
        int combineFrom = -1;
        int written = 0;
        for (int i = 0; i < lNum.size(); i++) {
            int i1 = lNum.get(i);
            if (combineFrom < 0) {
                // Check if next entry is the next in the ref list:
                if ((i < (lNum.size() - 1)) && (lNum.get(i + 1) == (i1 + 1)) && (i1 > 0)) {
                    combineFrom = i1;
                } else {
                    // Add single entry:
                    if (i > 0) {
                        sb.append(getStringCitProperty(CITATION_SEPARATOR));
                    }
                    sb.append(lNum.get(i) > 0 ? String.valueOf(lNum.get(i)) : OOBibStyle.UNDEFINED_CITATION_MARKER);
                    written++;
                }
            } else {
                // We are building a list of combined entries.
                // Check if it ends here:
                if ((i == (lNum.size() - 1)) || (lNum.get(i + 1) != (i1 + 1))) {
                    if (written > 0) {
                        sb.append(getStringCitProperty(CITATION_SEPARATOR));
                    }
                    if ((minGroupingCount > 0) && (((i1 + 1) - combineFrom) >= minGroupingCount)) {
                        sb.append(combineFrom);
                        sb.append(getStringCitProperty(GROUPED_NUMBERS_SEPARATOR));
                        sb.append(i1);
                        written++;
                    } else {
                        // Either we should never group, or there aren't enough
                        // entries in this case to group. Output all:
                        for (int jj = combineFrom; jj <= i1; jj++) {
                            sb.append(jj);
                            if (jj < i1) {
                                sb.append(getStringCitProperty(CITATION_SEPARATOR));
                            }
                            written++;
                        }
                    }
                    combineFrom = -1;

                }
                // If it doesn't end here, just keep iterating.
            }

        }
        sb.append(bracketAfter);
        return sb.toString();
    }

    /**
     * Format the marker for the in-text citation according to this BIB style. Uniquefier letters are added as
     * provided by the uniquefiers argument. If successive entries within the citation are uniquefied from each other,
     * this method will perform a grouping of these entries.
     *
     * @param entries       The list of JabRef BibEntry providing the data.
     * @param database      A map of BibEntry-BibDatabase pairs.
     * @param inParenthesis Signals whether a parenthesized citation or an in-text citation is wanted.
     * @param uniquefiers   Strings to add behind the year for each entry in case it's needed to separate similar
     *                      entries.
     * @param unlimAuthors  Boolean for each entry. If true, we should not use "et al" formatting regardless
     *                      of the number of authors. Can be null to indicate that no entries should have unlimited names.
     * @return The formatted citation.
     */
    public String getCitationMarker(List<BibEntry> entries, Map<BibEntry, BibDatabase> database, boolean inParenthesis,
            String[] uniquefiers, int[] unlimAuthors) {
        // Look for groups of uniquefied entries that should be combined in the output.
        // E.g. (Olsen, 2005a, b) should be output instead of (Olsen, 2005a; Olsen, 2005b).
        int piv = -1;
        String tmpMarker = null;
        if (uniquefiers != null) {
            for (int i = 0; i < uniquefiers.length; i++) {

                if ((uniquefiers[i] == null) || uniquefiers[i].isEmpty()) {
                    // This entry has no uniquefier.
                    // Check if we just passed a group of more than one entry with uniquefier:
                    if ((piv > -1) && (i > (piv + 1))) {
                        // Do the grouping:
                        group(entries, uniquefiers, piv, i - 1);
                    }

                    piv = -1;
                } else {
                    BibEntry currentEntry = entries.get(i);
                    if (piv == -1) {
                        piv = i;
                        tmpMarker = getAuthorYearParenthesisMarker(Collections.singletonList(currentEntry), database,
                                null, unlimAuthors);
                    } else {
                        // See if this entry can go into a group with the previous one:
                        String thisMarker = getAuthorYearParenthesisMarker(Collections.singletonList(currentEntry),
                                database, null, unlimAuthors);

                        String authorField = getStringCitProperty(AUTHOR_FIELD);
                        int maxAuthors = getIntCitProperty(MAX_AUTHORS);
                        String author = getCitationMarkerField(currentEntry, database.get(currentEntry),
                                authorField);
                        AuthorList al = AuthorList.parse(author);
                        int prevALim = unlimAuthors[i - 1]; // i always at least 1 here
                        if (!thisMarker.equals(tmpMarker)
                                || ((al.getNumberOfAuthors() > maxAuthors) && (unlimAuthors[i] != prevALim))) {
                            // No match. Update piv to exclude the previous entry. But first check if the
                            // previous entry was part of a group:
                            if ((piv > -1) && (i > (piv + 1))) {
                                // Do the grouping:
                                group(entries, uniquefiers, piv, i - 1);
                            }
                            tmpMarker = thisMarker;
                            piv = i;
                        }
                    }
                }

            }
            // Finished with the loop. See if the last entries form a group:
            if (piv >= 0) {
                // Do the grouping:
                group(entries, uniquefiers, piv, uniquefiers.length - 1);
            }
        }

        if (inParenthesis) {
            return getAuthorYearParenthesisMarker(entries, database, uniquefiers, unlimAuthors);
        } else {
            return getAuthorYearInTextMarker(entries, database, uniquefiers, unlimAuthors);
        }
    }

    /**
     * Modify entry and uniquefier arrays to facilitate a grouped presentation of uniquefied entries.
     *
     * @param entries     The entry array.
     * @param uniquefiers The uniquefier array.
     * @param from        The first index to group (inclusive)
     * @param to          The last index to group (inclusive)
     */
    private void group(List<BibEntry> entries, String[] uniquefiers, int from, int to) {
        String separator = getStringCitProperty(UNIQUEFIER_SEPARATOR);
        StringBuilder sb = new StringBuilder(uniquefiers[from]);
        for (int i = from + 1; i <= to; i++) {
            sb.append(separator);
            sb.append(uniquefiers[i]);
            entries.set(i, null);
        }
        uniquefiers[from] = sb.toString();
    }

    /**
     * This method produces (Author, year) style citation strings in many different forms.
     *
     * @param entries           The list of BibEntry to get fields from.
     * @param database          A map of BibEntry-BibDatabase pairs.
     * @param uniquifiers       Optional parameter to separate similar citations. Elements can be null if not needed.
     * @return The formatted citation.
     */
    private String getAuthorYearParenthesisMarker(List<BibEntry> entries, Map<BibEntry, BibDatabase> database,
            String[] uniquifiers, int[] unlimAuthors) {

        String authorField = getStringCitProperty(AUTHOR_FIELD); // The bibtex field providing author names, e.g. "author" or "editor".
        int maxA = getIntCitProperty(MAX_AUTHORS); // The maximum number of authors to write out in full without using etal. Set to
                                                             // -1 to always write out all authors.
        String yearSep = getStringCitProperty(YEAR_SEPARATOR); // The String to separate authors from year, e.g. "; ".
        String startBrace = getStringCitProperty(BRACKET_BEFORE); // The opening parenthesis.
        String endBrace = getStringCitProperty(BRACKET_AFTER); // The closing parenthesis.
        String citationSeparator = getStringCitProperty(CITATION_SEPARATOR); // The String to separate citations from each other.
        String yearField = getStringCitProperty(YEAR_FIELD); // The bibtex field providing the year, e.g. "year".
        String andString = getStringCitProperty(AUTHOR_LAST_SEPARATOR); // The String to add between the two last author names, e.g. " & ".
        StringBuilder sb = new StringBuilder(startBrace);
        for (int j = 0; j < entries.size(); j++) {
            BibEntry currentEntry = entries.get(j);

            // Check if this entry has been nulled due to grouping with the previous entry(ies):
            if (currentEntry == null) {
                continue;
            }

            if (j > 0) {
                sb.append(citationSeparator);
            }

            BibDatabase currentDatabase = database.get(currentEntry);
            int unlimA = unlimAuthors == null ? -1 : unlimAuthors[j];
            int maxAuthors = unlimA > 0 ? unlimA : maxA;

            String author = getCitationMarkerField(currentEntry, currentDatabase, authorField);
            String authorString = createAuthorList(author, maxAuthors, andString, yearSep);
            sb.append(authorString);
            String year = getCitationMarkerField(currentEntry, currentDatabase, yearField);
            if (year != null) {
                sb.append(year);
            }
            if ((uniquifiers != null) && (uniquifiers[j] != null)) {
                sb.append(uniquifiers[j]);
            }
        }
        sb.append(endBrace);
        return sb.toString();

    }

    /**
     * This method produces "Author (year)" style citation strings in many different forms.
     *
     * @param entries     The list of BibEntry to get fields from.
     * @param database    A map of BibEntry-BibDatabase pairs.
     * @param uniquefiers Optional parameters to separate similar citations. Can be null if not needed.
     * @return The formatted citation.
     */
    private String getAuthorYearInTextMarker(List<BibEntry> entries, Map<BibEntry, BibDatabase> database,
            String[] uniquefiers,
            int[] unlimAuthors) {
        String authorField = getStringCitProperty(AUTHOR_FIELD); // The bibtex field providing author names, e.g. "author" or "editor".
        int maxA = getIntCitProperty(MAX_AUTHORS); // The maximum number of authors to write out in full without using etal. Set to
                                                             // -1 to always write out all authors.
        String yearSep = getStringCitProperty(IN_TEXT_YEAR_SEPARATOR); // The String to separate authors from year, e.g. "; ".
        String startBrace = getStringCitProperty(BRACKET_BEFORE); // The opening parenthesis.
        String endBrace = getStringCitProperty(BRACKET_AFTER); // The closing parenthesis.
        String citationSeparator = getStringCitProperty(CITATION_SEPARATOR); // The String to separate citations from each other.
        String yearField = getStringCitProperty(YEAR_FIELD); // The bibtex field providing the year, e.g. "year".
        String andString = getStringCitProperty(AUTHOR_LAST_SEPARATOR_IN_TEXT); // The String to add between the two last author names, e.g. " & ".

        if (andString == null) {
            // Use the default one if no explicit separator for text is defined
            andString = getStringCitProperty(AUTHOR_LAST_SEPARATOR);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            BibEntry currentEntry = entries.get(i);

            // Check if this entry has been nulled due to grouping with the previous entry(ies):
            if (currentEntry == null) {
                continue;
            }

            BibDatabase currentDatabase = database.get(currentEntry);
            int unlimA = unlimAuthors == null ? -1 : unlimAuthors[i];
            int maxAuthors = unlimA > 0 ? unlimA : maxA;

            if (i > 0) {
                sb.append(citationSeparator);
            }
            String author = getCitationMarkerField(currentEntry, currentDatabase, authorField);
            String authorString = createAuthorList(author, maxAuthors, andString, yearSep);
            sb.append(authorString);
            sb.append(startBrace);
            String year = getCitationMarkerField(currentEntry, currentDatabase, yearField);
            if (year != null) {
                sb.append(year);
            }
            if ((uniquefiers != null) && (uniquefiers[i] != null)) {
                sb.append(uniquefiers[i]);
            }
            sb.append(endBrace);
        }
        return sb.toString();

    }

    /**
     * This method looks up a field for an entry in a database. Any number of backup fields can be used
     * if the primary field is empty.
     *
     * @param entry    The entry.
     * @param database The database the entry belongs to.
     * @param field    The field, or succession of fields, to look up. If backup fields are needed, separate
     *                 field names by /. E.g. to use "author" with "editor" as backup, specify FieldName.orFields(FieldName.AUTHOR, FieldName.EDITOR).
     * @return The resolved field content, or an empty string if the field(s) were empty.
     */
    private String getCitationMarkerField(BibEntry entry, BibDatabase database, String field) {
        String authorField = getStringCitProperty(AUTHOR_FIELD);
        String[] fields = field.split(FieldName.FIELD_SEPARATOR);
        for (String s : fields) {
            Optional<String> content = entry.getResolvedFieldOrAlias(s, database);

            if ((content.isPresent()) && !content.get().trim().isEmpty()) {
                if (field.equals(authorField) && StringUtil.isInCurlyBrackets(content.get())) {
                    return "{" + fieldFormatter.format(content.get()) + "}";
                }
                return fieldFormatter.format(content.get());
            }
        }
        // No luck? Return an empty string:
        return "";
    }


    /**
     * Look up the nth author and return the proper last name for citation markers.
     *
     * @param al     The author list.
     * @param number The number of the author to return.
     * @return The author name, or an empty String if inapplicable.
     */
    private String getAuthorLastName(AuthorList al, int number) {
        StringBuilder sb = new StringBuilder();

        if (al.getNumberOfAuthors() > number) {
            Author a = al.getAuthor(number);
            a.getVon().filter(von -> !von.isEmpty()).ifPresent(von -> sb.append(von).append(' '));
            sb.append(a.getLast().orElse(""));
        }

        return sb.toString();
    }

    /**
     * Take a finished citation and insert a string at the end (but inside the end bracket)
     * separated by "PageInfoSeparator"
     *
     * @param citation
     * @param pageInfo
     * @return
     */
    public String insertPageInfo(String citation, String pageInfo) {
        String bracketAfter = getStringCitProperty(BRACKET_AFTER);
        if (citation.endsWith(bracketAfter)) {
            String first = citation.substring(0, citation.length() - bracketAfter.length());
            return first + getStringCitProperty(PAGE_INFO_SEPARATOR) + pageInfo + bracketAfter;
        } else {
            return citation + getStringCitProperty(PAGE_INFO_SEPARATOR) + pageInfo;
        }
    }

    /**
     * Convenience method for checking the property for whether we use number citations or
     * author-year citations.
     *
     * @return true if we use numbered citations, false otherwise.
     */
    public boolean isNumberEntries() {
        return (Boolean) getProperty(IS_NUMBER_ENTRIES);
    }

    /**
     * Convenience method for checking the property for whether we sort the bibliography
     * according to their order of appearance in the text.
     *
     * @return true to sort by appearance, false to sort alphabetically.
     */
    public boolean isSortByPosition() {
        return (Boolean) getProperty(IS_SORT_BY_POSITION);
    }

    /**
     * Convenience method for checking whether citation markers should be italicized.
     * Will only be relevant if isFormatCitations() returns true.
     *
     * @return true to indicate that citations should be in italics.
     */
    public boolean isItalicCitations() {
        return (Boolean) citProperties.get(ITALIC_CITATIONS);
    }

    /**
     * Convenience method for checking whether citation markers should be bold.
     * Will only be relevant if isFormatCitations() returns true.
     *
     * @return true to indicate that citations should be in bold.
     */
    public boolean isBoldCitations() {
        return (Boolean) citProperties.get(BOLD_CITATIONS);
    }

    /**
     * Convenience method for checking whether citation markers formatted
     * according to the results of the isItalicCitations() and
     * isBoldCitations() methods.
     *
     * @return true to indicate that citations should be in italics.
     */
    public boolean isFormatCitations() {
        return (Boolean) citProperties.get(FORMAT_CITATIONS);
    }

    public boolean isBibtexKeyCiteMarkers() {
        return (Boolean) citProperties.get(BIBTEX_KEY_CITATIONS);
    }

    /**
     * Get boolean property.
     *
     * @param key The property key
     * @return the value
     */
    public boolean getBooleanCitProperty(String key) {
        return (Boolean) citProperties.get(key);
    }

    public int getIntCitProperty(String key) {
        return (Integer) citProperties.get(key);
    }

    public String getStringCitProperty(String key) {
        return (String) citProperties.get(key);
    }

    public String getCitationCharacterFormat() {
        return getStringCitProperty(CITATION_CHARACTER_FORMAT);
    }

    /**
     * Get a style property.
     *
     * @param propName The property name.
     * @return The property value, or null if it doesn't exist.
     */
    public Object getProperty(String propName) {
        return properties.get(propName);
    }

    public boolean isFromResource() {
        return fromResource;
    }

    public String getLocalCopy() {
        return localCopy;
    }

    @Override
    public int compareTo(OOBibStyle other) {
        return getName().compareTo(other.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof OOBibStyle) {
            OOBibStyle otherStyle = (OOBibStyle) o;
            return Objects.equals(path, otherStyle.path) && Objects.equals(name, otherStyle.name)
                    && Objects.equals(citProperties, otherStyle.citProperties)
                    && Objects.equals(properties, otherStyle.properties);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, name, citProperties, properties);
    }

    private String createAuthorList(String author, int maxAuthors, String andString,
            String yearSep) {
        Objects.requireNonNull(author);
        String etAlString = getStringCitProperty(ET_AL_STRING); //  The String to represent authors that are not mentioned, e.g. " et al."
        String authorSep = getStringCitProperty(AUTHOR_SEPARATOR); // The String to add between author names except the last two, e.g. ", ".
        String oxfordComma = getStringCitProperty(OXFORD_COMMA); // The String to put after the second to last author in case of three or more authors
        StringBuilder sb = new StringBuilder();
        AuthorList al = AuthorList.parse(author);
        if (!al.isEmpty()) {
            sb.append(getAuthorLastName(al, 0));
        }
        if ((al.getNumberOfAuthors() > 1) && ((al.getNumberOfAuthors() <= maxAuthors) || (maxAuthors < 0))) {
            int j = 1;
            while (j < (al.getNumberOfAuthors() - 1)) {
                sb.append(authorSep);
                sb.append(getAuthorLastName(al, j));
                j++;
            }
            if (al.getNumberOfAuthors() > 2) {
                sb.append(oxfordComma);
            }
            sb.append(andString);
            sb.append(getAuthorLastName(al, al.getNumberOfAuthors() - 1));
        } else if (al.getNumberOfAuthors() > maxAuthors) {
            sb.append(etAlString);
        }
        sb.append(yearSep);
        return sb.toString();
    }
}
