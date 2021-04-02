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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
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
    private static final String CITATION_KEY_CITATIONS = "BibTeXKeyCitations";
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
    private final SortedSet<String> journals = new TreeSet<>();
    // Formatter to be run on fields before they are used as part of citation marker:
    private final LayoutFormatter fieldFormatter = new OOPreFormatter();
    // reference layout mapped from entry type:
    private final Map<EntryType, Layout> bibLayout = new HashMap<>();
    private final Map<String, Object> properties = new HashMap<>();
    private final Map<String, Object> citProperties = new HashMap<>();
    private final boolean fromResource;
    private final String path;
    private final Charset encoding;
    private final LayoutFormatterPreferences prefs;
    private String name = "";
    private Layout defaultBibLayout;
    private boolean valid;
    private File styleFile;
    private long styleFileModificationTime = Long.MIN_VALUE;
    private String localCopy;
    private boolean isDefaultLayoutPresent;

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

    public Layout getDefaultBibLayout() {
        return defaultBibLayout;
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
        citProperties.put(AUTHOR_FIELD,
                          FieldFactory.serializeOrFields(StandardField.AUTHOR, StandardField.EDITOR));
        citProperties.put(YEAR_FIELD, StandardField.YEAR.getName());
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
        citProperties.put(CITATION_KEY_CITATIONS, Boolean.FALSE);
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
        if ((mode != BibStyleMode.NONE) && isDefaultLayoutPresent) {
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
     */
    private void handleStructureLine(String line) {
        int index = line.indexOf('=');
        if ((index > 0) && (index < (line.length() - 1))) {

            try {
                String formatString = line.substring(index + 1);
                StringReader reader = new StringReader(formatString);
                Layout layout = new LayoutHelper(reader, this.prefs).getLayoutFromText();
                EntryType type = EntryTypeFactory.parse(line.substring(0, index));

                if (!isDefaultLayoutPresent && line.substring(0, index).equals(OOBibStyle.DEFAULT_MARK)) {
                    isDefaultLayoutPresent = true;
                    defaultBibLayout = layout;
                } else {
                    bibLayout.put(type, layout);
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
     */
    private void handleJournalsLine(String line) {
        if (!line.trim().isEmpty()) {
            journals.add(line.trim());
        }
    }

    public Layout getReferenceFormat(EntryType type) {
        Layout l = bibLayout.get(type);
        if (l == null) {
            return defaultBibLayout;
        } else {
            return l;
        }
    }

    /**
     *  Make sure that (1) we have exactly one entry for each
     *  citation, (2) each entry is either null or is not empty when trimmed.
     */
    public List<String> regularizePageInfosForCitations(List<String> pageInfosForCitations,
                                                        int nCitations) {
        if (pageInfosForCitations == null) {
            List<String> res = new ArrayList<>(nCitations);
            for (int i = 0; i < nCitations; i++) {
                res.add(null);
            }
            return res;
        } else {
            if (pageInfosForCitations.size() != nCitations) {
                throw new RuntimeException("regularizePageInfosForCitations:"
                                           + " pageInfosForCitations.size() != nCitations");
            }
            List<String> res = new ArrayList<>(nCitations);
            for (int i = 0; i < nCitations; i++) {
                String p = pageInfosForCitations.get(i);
                if (p != null) {
                    String pt = p.trim();
                    if (pt.equals("")) {
                        p = null;
                    } else {
                        p = pt;
                    }
                }
                res.add(p);
            }
            return res;
        }
    }

    /*
     * Helper class for sorting citation numbers while
     * maintaining their correspondance to pageInfos.
     */
    private static class NumberWithPageInfo {
        int num;
        String pageInfo;
        NumberWithPageInfo(int num, String pageInfo) {
            this.num = num;
            this.pageInfo = pageInfo;
        }
    }

    /**
     * Defines sort order for NumberWithPageInfo entries.
     *
     * null comes before non-null
     */
    private static int compareNumberWithPageInfo(NumberWithPageInfo a, NumberWithPageInfo b) {
        int res = Integer.compare(a.num, b.num);
        if (res == 0) {
            if (a.pageInfo == null && b.pageInfo == null) {
                res = 0;
            } else if (a.pageInfo == null) {
                res = -1;
            } else if (b.pageInfo == null) {
                res = +1;
            } else {
                res = a.pageInfo.compareTo(b.pageInfo);
            }
        }
        return res;
    }

    /**
     * See {@see getNumCitationMarkerCommon} for details.
     */
    public String getNumCitationMarkerForInText(List<Integer> numbers,
                                                int minGroupingCount,
                                                List<String> pageInfosForCitations) {
        return getNumCitationMarkerCommon(numbers,
                                          minGroupingCount,
                                          CitationMarkerPurpose.CITATION,
                                          pageInfosForCitations);
    }

    /**
     *  Create a numeric marker for use in the bibliography as label for the entry.
     *
     *  To support for example numbers in superscript without brackets for the text,
     *  but "[1]" form for the bibliogaphy, the style can provide
     *  the optional "BracketBeforeInList" and "BracketAfterInList" strings
     *  to be used in the bibliography instead of "BracketBefore" and "BracketAfter"
     *
     *  @return "[${number}]" where
     *       "[" stands for BRACKET_BEFORE_IN_LIST (with fallback BRACKET_BEFORE)
     *       "]" stands for BRACKET_AFTER_IN_LIST (with fallback BRACKET_AFTER)
     *       "${number}" stands for the formatted number.
     */
    public String getNumCitationMarkerForBibliography(int number) {
        return getNumCitationMarkerCommon(Collections.singletonList(number),
                                          0,
                                          CitationMarkerPurpose.BIBLIOGRAPHY,
                                          null);
    }

    private enum CitationMarkerPurpose {
        /** Creating citation marker for in-text citation. */
        CITATION,
        /** Creating citation marker for the bibliography. */
        BIBLIOGRAPHY
    }

    /**
     * Format a number-based citation marker for the given number or numbers.
     *
     * This is the common implementation behind
     * getNumCitationMarkerForInText and
     * getNumCitationMarkerForBibliography. The latter could be easily
     * separated unless there is (or going to be) a need for handling
     * multiple numbers or page info by getNumCitationMarkerForBibliography.
     *
     * @param numbers The citation numbers.
     *
     *               A zero in the list means: could not look this up
     *               in the databases. Positive integers are the valid numbers.
     *
     *               Duplicate citation numbers are allowed:
     *
     *                 - If their pageInfos are identical, only a
     *                   single instance is emitted.
     *
     *                 - If their pageInfos differ, the number is emitted with each
     *                    distinct pageInfo.
     *
     *                    For pageInfo null and "" (after
     *                    pageInfo.trim()) are considered equal (and missing).
     *
     * @param minGroupingCount Zero and negative means never group
     *
     * @param purpose BIBLIOGRAPHY (was: inList==True) when creating for a bibliography entry,
     *                CITATION (was: inList=false) when creating in-text citation.
     *
     *               If BIBLIOGRAPHY: Prefer BRACKET_BEFORE_IN_LIST over BRACKET_BEFORE,
     *                                   and BRACKET_AFTER_IN_LIST over BRACKET_AFTER.
     *                                Ignore pageInfosForCitations.
     *
     * @param pageInfosForCitations  Null for "none", or a list with a
     *        pageInfo for each citation. Any or all of these can be null as well.
     *
     * @return The text for the citation.
     *
     */
    private String getNumCitationMarkerCommon(List<Integer> numbers,
                                              int minGroupingCount,
                                              CitationMarkerPurpose purpose,
                                              List<String> pageInfosForCitations) {

        final boolean joinIsDisabled = (minGroupingCount <= 0);
        final int notFoundInDatabases = 0;
        final int nCitations = numbers.size();

        /*
         * strictPurpose: if true, require (nCitations == 1) when (purpose == BIBLIOGRAPHY),
         *                otherwise allow multiple citation numbers and process the BIBLIOGRAPHY case
         *                as CITATION with no pageInfo.
         */
        final boolean strictPurpose = true;

        String bracketBefore = getStringCitProperty(BRACKET_BEFORE);
        String bracketAfter = getStringCitProperty(BRACKET_AFTER);

        /*
         * purpose == BIBLIOGRAPHY means: we are formatting for the
         *                       bibliography, (not for in-text citation).
         */
        if (purpose == CitationMarkerPurpose.BIBLIOGRAPHY) {
            // prefer BRACKET_BEFORE_IN_LIST and BRACKET_AFTER_IN_LIST
            if (citProperties.containsKey(BRACKET_BEFORE_IN_LIST)) {
                bracketBefore = getStringCitProperty(BRACKET_BEFORE_IN_LIST);
            }
            if (citProperties.containsKey(BRACKET_AFTER_IN_LIST)) {
                bracketAfter = getStringCitProperty(BRACKET_AFTER_IN_LIST);
            }

            if (strictPurpose) {
                // If (purpose==BIBLIOGRAPHY), then
                // we expect exactly one number here, and can handle quickly
                if (nCitations != 1) {
                    throw new RuntimeException(
                        "getNumCitationMarker:"
                        + "nCitations != 1 for purpose==BIBLIOGRAPHY."
                        + String.format(" nCitations = %d", nCitations));
                }
                //
                StringBuilder sb = new StringBuilder(bracketBefore);
                final int current = numbers.get(0);
                if (current < 0) {
                    throw new RuntimeException("getNumCitationMarker: found negative value");
                }
                sb.append(current != notFoundInDatabases
                          ? String.valueOf(current)
                          : OOBibStyle.UNDEFINED_CITATION_MARKER);
                sb.append(bracketAfter);
                return sb.toString();
            }
        }

        /*
         * From here:
         *  - formatting for in-text (not for bibliography)
         *  - need to care about pageInfosForCitations
         *
         *  - In case {@code strictPurpose} above is set to false and allows us to
         *    get here, and {@code purpose==BIBLIOGRAPHY}, then we just fill
         *    pageInfos with null values.
         */
        List<String> pageInfos =
            regularizePageInfosForCitations((purpose == CitationMarkerPurpose.BIBLIOGRAPHY
                                             ? null
                                             : pageInfosForCitations),
                                            numbers.size());

        // Sort the numbers, together with the corresponding pageInfo values
        List<NumberWithPageInfo> nps = new ArrayList<>();
        for (int i = 0; i < nCitations; i++) {
            nps.add(new NumberWithPageInfo(numbers.get(i), pageInfos.get(i)));
        }
        Collections.sort(nps, OOBibStyle::compareNumberWithPageInfo);

        // "["
        StringBuilder sb = new StringBuilder(bracketBefore);

        /*
         * int emitBlock(List<NumberWithPageInfo> block)
         *
         * Given a block containing 1 or (two or more)
         * NumberWithPageInfo entries collected as singletons or
         * joinable into an "i-j" form, append to {@code sb} the
         * formatted text.
         *
         * Assumes:
         *
         * - block is not empty
         *
         * - For a block with a single element the element may have
         *    pageInfo and its num part may be zero
         *    (notFoundInDatabases).
         *
         * - For a block with two or more elements
         *
         *   - The elements do not have pageInfo and their num part is
         *     not zero.
         *
         *   - The elements num parts are consecutive positive integers,
         *     without repetition.
         *
         * Note: this function is long enough to move into a separate method.
         *       On the other hand, its assumptions strongly tie it to
         *       the loop below that collects the block.
         *
         * @return The number of blocks emitted. Since currently
         *         throws if the block is empty, the returned value is
         *         always 1.
         *
         */
        ToIntFunction<List<NumberWithPageInfo>> emitBlock = (List<NumberWithPageInfo> block) -> {
            // uses:  sb, this,

            final int blockSize = block.size();
            if (blockSize == 0) {
                throw new RuntimeException("We should not get here");
                // return 0;
            }

            if (blockSize == 1) {
                // Add single entry:
                final int num = block.get(0).num;
                sb.append(num == notFoundInDatabases
                          ? OOBibStyle.UNDEFINED_CITATION_MARKER
                          : String.valueOf(num));
                // Emit pageInfo
                String pageInfo = block.get(0).pageInfo;
                if (pageInfo != null) {
                    sb.append(getStringCitProperty(PAGE_INFO_SEPARATOR) + pageInfo);
                }
            } else {

                /*
                 * Check assumptions
                 */

                // block has at least 2 elements
                if (blockSize < 2) {
                    throw new RuntimeException("impossible: (blockSize < 2)");
                }
                // None of these elements has a pageInfo,
                // because if it had, we would not join.
                for (NumberWithPageInfo x : block) {
                    if (x.pageInfo != null) {
                        throw new RuntimeException("impossible: (x.pageInfo != null)");
                    }
                }
                // None of these elements needs UNDEFINED_CITATION_MARKER,
                // because if it did, we would not join.
                for (NumberWithPageInfo x : block) {
                    if (x.num == notFoundInDatabases) {
                        throw new RuntimeException("impossible: (x.num == notFoundInDatabases)");
                    }
                }
                // consecutive elements have consecutive numbers
                for (int j = 1; j < blockSize; j++) {
                    if (block.get(j).num != (block.get(j - 1).num + 1)) {
                        throw new RuntimeException("impossible: consecutive elements"
                                                   + " without consecutive numbers");
                    }
                }

                /*
                 * Do the actual work
                 */
                if (blockSize >= minGroupingCount) {
                    int first = block.get(0).num;
                    int last = block.get(blockSize - 1).num;
                    if (((last + 1) - first) != blockSize) {
                        throw new RuntimeException("impossible:"
                                                   + " blockSize and length of num range differ");
                    }
                    // Emit: "first-last"
                    sb.append(first);
                    sb.append(getStringCitProperty(GROUPED_NUMBERS_SEPARATOR));
                    sb.append(last);
                } else {
                    // Emit: first,first+1,...,last
                    for (int j = 0; j < blockSize; j++) {
                        if (j > 0) {
                            sb.append(getStringCitProperty(CITATION_SEPARATOR));
                        }
                        sb.append(block.get(j).num);
                    }
                }
            }
            return 1;
        };

        /*
         *  Original:
         *  [2,3,4]   -> [2-4]
         *  [0,1,2]   -> [??,1,2]
         *  [0,1,2,3] -> [??,1-3]
         *
         *  Now we have to consider: duplicate numbers and pageInfos
         *  [1,1] -> [1]
         *  [1,1 "pp nn"] -> keep separate if pageInfo differs
         *  [1 "pp nn",1 "pp nn"] -> [1 "pp nn"]
         */

        int blocksEmitted = 0;
        List<NumberWithPageInfo> currentBlock = new ArrayList<>();
        List<NumberWithPageInfo> nextBlock = new ArrayList<>();

        for (int i = 0; i < nCitations; i++) {

            final NumberWithPageInfo current = nps.get(i);
            if (current.num < 0) {
                throw new RuntimeException("getNumCitationMarker: found negative value");
            }

            if (currentBlock.size() == 0) {
                currentBlock.add(current);
            } else {
                NumberWithPageInfo prev = currentBlock.get(currentBlock.size() - 1);
                if ((notFoundInDatabases == current.num)
                     || (notFoundInDatabases == prev.num)) {
                    nextBlock.add(current); // do not join if not found
                } else if (joinIsDisabled) {
                    nextBlock.add(current); // join disabled
                } else if (compareNumberWithPageInfo(current, prev) == 0) {
                    // Same as prev, just forget it.
                } else if ((current.num == (prev.num + 1))
                           && (prev.pageInfo == null)
                           && (current.pageInfo == null)) {
                    // Just two consecutive numbers without pageInfo: join
                    currentBlock.add(current);
                } else {
                    // do not join
                    nextBlock.add(current);
                }
            }

            if (nextBlock.size() > 0) {
                // emit current block
                // We are emitting a block
                if (blocksEmitted > 0) {
                    sb.append(getStringCitProperty(CITATION_SEPARATOR));
                }
                int emittedNow = emitBlock.applyAsInt(currentBlock);
                if (emittedNow > 0) {
                    blocksEmitted += emittedNow;
                    currentBlock = nextBlock;
                    nextBlock = new ArrayList<>();
                }
            } // blockSize != 0

        } // for i

        if (nextBlock.size() != 0) {
            throw new RuntimeException("impossible: (nextBlock.size() != 0) after loop");
        }

        if (currentBlock.size() > 0) {
            // We are emitting a block
            if (blocksEmitted > 0) {
                sb.append(getStringCitProperty(CITATION_SEPARATOR));
            }
            emitBlock.applyAsInt(currentBlock);
        }

        // Emit: "]"
        sb.append(bracketAfter);
        return sb.toString();
    }

    /**
     * Format the marker for the in-text citation according to this
     * BIB style. Uniquefier letters are added as provided by the
     * uniquefiers argument. If successive entries within the citation
     * are uniquefied from each other, this method will perform a
     * grouping of these entries.
     *
     * @param entries       The list of JabRef BibEntry providing the data.
     * @param database      A map of BibEntry-BibDatabase pairs.
     * @param inParenthesis Signals whether a parenthesized citation or an in-text citation is wanted.
     * @param uniquefiers   Strings to add behind the year for each entry in case it's needed
     *                      to separate similar entries.
     * @param unlimAuthors  Boolean for each entry. If true, we should not use "et al" formatting
     *                      regardless of the number of authors.
     *                       Can be null to indicate that no entries should have unlimited names.
     * @param pageInfosForCitations  Null for "none", or a list with
     *                      pageInfo for each citation. These can be null as well.
     * @return The formatted citation.
     */
    public String getCitationMarker(List<BibEntry> entries,
                                    Map<BibEntry, BibDatabase> database,
                                    boolean inParenthesis,
                                    String[] uniquefiers,
                                    int[] unlimAuthors,
                                    List<String> pageInfosForCitations
        ) {
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
                        tmpMarker = getAuthorYearParenthesisMarker(Collections.singletonList(currentEntry),
                                                                   database,
                                                                   null,
                                                                   unlimAuthors);
                    } else {
                        // See if this entry can go into a group with the previous one:
                        String thisMarker =
                            getAuthorYearParenthesisMarker(Collections.singletonList(currentEntry),
                                                           database,
                                                           null,
                                                           unlimAuthors);

                        String authorField = getStringCitProperty(AUTHOR_FIELD);
                        int maxAuthors = getIntCitProperty(MAX_AUTHORS);
                        String author = getCitationMarkerField(currentEntry, database.get(currentEntry),
                                authorField);
                        AuthorList al = AuthorList.parse(author);
                        int prevALim = unlimAuthors[i - 1]; // i always at least 1 here
                        if (!thisMarker.equals(tmpMarker)
                            || ((al.getNumberOfAuthors() > maxAuthors)
                                && (unlimAuthors[i] != prevALim))) {
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
     * @param uniquifiers       Optional parameter to separate similar citations.
     *                          Elements can be null if not needed.
     * @return The formatted citation.
     */
    private String getAuthorYearParenthesisMarker(List<BibEntry> entries,
                                                  Map<BibEntry, BibDatabase> database,
                                                  String[] uniquifiers,
                                                  int[] unlimAuthors) {

        // The bibtex field providing author names, e.g. "author" or
        // "editor".
        String authorField = getStringCitProperty(AUTHOR_FIELD);

        // The maximum number of authors to write out in full without
        // using etal. Set to -1 to always write out all authors.
        int maxA = getIntCitProperty(MAX_AUTHORS);

        // The String to separate authors from year, e.g. "; ".
        String yearSep = getStringCitProperty(YEAR_SEPARATOR);

        // The opening parenthesis.
        String startBrace = getStringCitProperty(BRACKET_BEFORE);

        // The closing parenthesis.
        String endBrace = getStringCitProperty(BRACKET_AFTER);

        // The String to separate citations from each other.
        String citationSeparator = getStringCitProperty(CITATION_SEPARATOR);

        // The bibtex field providing the year, e.g. "year".
        String yearField = getStringCitProperty(YEAR_FIELD);

        // The String to add between the two last author names, e.g. " & ".
        String andString = getStringCitProperty(AUTHOR_LAST_SEPARATOR);

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
            int unlimA = (unlimAuthors == null) ? -1 : unlimAuthors[j];
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
    private String getAuthorYearInTextMarker(List<BibEntry> entries,
                                             Map<BibEntry, BibDatabase> database,
                                             String[] uniquefiers,
                                             int[] unlimAuthors) {
        // The bibtex field providing author names, e.g. "author" or "editor".
        String authorField = getStringCitProperty(AUTHOR_FIELD);

        // The maximum number of authors to write out in full without using etal. Set to
        // -1 to always write out all authors.
        int maxA = getIntCitProperty(MAX_AUTHORS);

        // The String to separate authors from year, e.g. "; ".
        String yearSep = getStringCitProperty(IN_TEXT_YEAR_SEPARATOR);

        // The opening parenthesis.
        String startBrace = getStringCitProperty(BRACKET_BEFORE);

        // The closing parenthesis.
        String endBrace = getStringCitProperty(BRACKET_AFTER);

        // The String to separate citations from each other.
        String citationSeparator = getStringCitProperty(CITATION_SEPARATOR);

        // The bibtex field providing the year, e.g. "year".
        String yearField = getStringCitProperty(YEAR_FIELD);

        // The String to add between the two last author names, e.g. " & ".
        String andString = getStringCitProperty(AUTHOR_LAST_SEPARATOR_IN_TEXT);

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
            int unlimA = (unlimAuthors == null) ? -1 : unlimAuthors[i];
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
     * @param fields   The field, or succession of fields, to look up.
     *                 If backup fields are needed, separate
     *                 field names by /. E.g. to use "author" with "editor" as backup,
     *                 specify StandardField.orFields(StandardField.AUTHOR, StandardField.EDITOR).
     * @return The resolved field content, or an empty string if the field(s) were empty.
     */
    private String getCitationMarkerField(BibEntry entry, BibDatabase database, String fields) {
        Objects.requireNonNull(entry, "Entry cannot be null");
        Objects.requireNonNull(database, "database cannot be null");

        Set<Field> authorFields = FieldFactory.parseOrFields(getStringCitProperty(AUTHOR_FIELD));
        for (Field field : FieldFactory.parseOrFields(fields)) {
            Optional<String> content = entry.getResolvedFieldOrAlias(field, database);

            if ((content.isPresent()) && !content.get().trim().isEmpty()) {
                if (authorFields.contains(field) && StringUtil.isInCurlyBrackets(content.get())) {
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
     * Take a finished citation and insert a string at the end (but
     * inside the end bracket) separated by "PageInfoSeparator"
     *
     * @param citation A formatted citation probably ending with BRACKET_AFTER.
     * @param pageInfo Text to be inserted.
     * @return The modified citation.
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

    public boolean isCitationKeyCiteMarkers() {
        return (Boolean) citProperties.get(CITATION_KEY_CITATIONS);
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

    /**
     * Indicate if it is an internal style
     *
     * @return True if an internal style
     */
    public boolean isInternalStyle() {
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
            return Objects.equals(path, otherStyle.path)
                    && Objects.equals(name, otherStyle.name)
                    && Objects.equals(citProperties, otherStyle.citProperties)
                    && Objects.equals(properties, otherStyle.properties);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, name, citProperties, properties);
    }

    /**
     * @param maxAuthors The maximum number of authors to write out in
     *                   full without using etal. Set to -1 to always
     *                   write out all authors.
     */
    private String createAuthorList(String author,
                                    int maxAuthors,
                                    String andString,
                                    String yearSep) {
        Objects.requireNonNull(author);

        // The String to represent authors that are not mentioned,
        // e.g. " et al."
        String etAlString = getStringCitProperty(ET_AL_STRING);

        // The String to add between author names except the last two,
        // e.g. ", ".
        String authorSep = getStringCitProperty(AUTHOR_SEPARATOR);

        // The String to put after the second to last author in case
        // of three or more authors
        String oxfordComma = getStringCitProperty(OXFORD_COMMA);

        StringBuilder sb = new StringBuilder();
        AuthorList al = AuthorList.parse(author);
        final int nAuthors = al.getNumberOfAuthors();

        if (nAuthors > 0) {
            // The first author
            sb.append(getAuthorLastName(al, 0));
        }

        boolean emitAllAuthors = ((nAuthors <= maxAuthors) || (maxAuthors < 0));
        if ((nAuthors >= 2) && emitAllAuthors) {
            // Emit last names, except for the last author
            int j = 1;
            while (j < (nAuthors - 1)) {
                sb.append(authorSep);
                sb.append(getAuthorLastName(al, j));
                j++;
            }
            // oxfordComma if at least 3 authors
            if (nAuthors >= 3) {
                sb.append(oxfordComma);
            }
            // Emit "and LastAuthor"
            sb.append(andString);
            sb.append(getAuthorLastName(al, nAuthors - 1));

        } else if (nAuthors > maxAuthors && nAuthors > 1) {
            // maxAuthors  nAuthors result
            //  0            1       "Smith"
            // -1            1       "Smith"
            // -1            0       ""
            sb.append(etAlString);
        }
        sb.append(yearSep);
        return sb.toString();
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
