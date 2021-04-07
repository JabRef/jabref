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
 *
 * This class embodies bibliography formatting for OpenOffice, which
 * is composed of the following elements:
 *
 * <p>
 * 1) For each type of {@code BibEntry} we need a formatting specification,
 *    termed {@code Layout} here.
 *
 *    A formatting is an array of elements, each of which is either
 *    - a piece of constant text,
 *    - an entry field value,
 *    - or a tab.
 *    Each element has a character format associated with it.
 * <p>
 * 2) Many field values (e.g. author) need to be formatted before
 *    input to OpenOffice.
 *
 *    The style has the responsibility of formatting all field
 *    values.
 *
 *    Formatting is handled by {@code LayoutFormatter} classes.
 * <p>
 * 3) A citation marker must be produced for each entry.
 *
 *    For non-numbered styles this operation is performed for each
 *    {@code BibEntry} by {@code getCitationMarker}, for numbered styles by
 *    {@code getNumCitationMarkerForInText}.
 *
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

    // These are used in OOBibStyleGetNumCitationMarker
    protected static final String BRACKET_AFTER = "BracketAfter";
    protected static final String BRACKET_BEFORE = "BracketBefore";
    protected static final String BRACKET_AFTER_IN_LIST = "BracketAfterInList";
    protected static final String BRACKET_BEFORE_IN_LIST = "BracketBeforeInList";
    protected static final String GROUPED_NUMBERS_SEPARATOR = "GroupedNumbersSeparator";
    protected static final String PAGE_INFO_SEPARATOR = "PageInfoSeparator";
    protected static final String CITATION_SEPARATOR = "CitationSeparator";

    // These are used in OOBibStyleGetCitationMarker
    protected static final String AUTHOR_FIELD = "AuthorField";
    protected static final String MAX_AUTHORS = "MaxAuthors";
    protected static final String UNIQUEFIER_SEPARATOR = "UniquefierSeparator";
    protected static final String YEAR_SEPARATOR = "YearSeparator";
    protected static final String AUTHOR_LAST_SEPARATOR = "AuthorLastSeparator";
    protected static final String IN_TEXT_YEAR_SEPARATOR = "InTextYearSeparator";
    protected static final String YEAR_FIELD = "YearField";
    protected static final String AUTHOR_LAST_SEPARATOR_IN_TEXT = "AuthorLastSeparatorInText";
    protected static final String OXFORD_COMMA = "OxfordComma";
    protected static final String AUTHOR_SEPARATOR = "AuthorSeparator";


    private static final String CITATION_KEY_CITATIONS = "BibTeXKeyCitations";
    private static final String SUBSCRIPT_CITATIONS = "SubscriptCitations";
    private static final String SUPERSCRIPT_CITATIONS = "SuperscriptCitations";
    private static final String BOLD_CITATIONS = "BoldCitations";
    private static final String ITALIC_CITATIONS = "ItalicCitations";
    private static final String CITATION_CHARACTER_FORMAT = "CitationCharacterFormat";
    private static final String FORMAT_CITATIONS = "FormatCitations";
    private static final String IS_NUMBER_ENTRIES = "IsNumberEntries";
    private static final String IS_SORT_BY_POSITION = "IsSortByPosition";
    private static final String SORT_ALGORITHM = "SortAlgorithm";

    private static final Logger LOGGER = LoggerFactory.getLogger(OOBibStyle.class);

    // Formatter to be run on fields before they are used as part of citation marker:
    protected final LayoutFormatter fieldFormatter = new OOPreFormatter();


    private final boolean fromResource;
    private final String path;
    private final Charset encoding;

    private File styleFile;
    private long styleFileModificationTime = Long.MIN_VALUE;

    /*
     * Used or modified in OOBibStyleParser.readFormatFile()
     */
    protected String name = "";
    protected String localCopy;
    protected final LayoutFormatterPreferences prefs;
    protected Layout defaultBibLayout;
    protected boolean isDefaultLayoutPresent;
    // reference layout mapped from entry type:
    protected final Map<EntryType, Layout> bibLayout = new HashMap<>();
    protected final Map<String, Object> properties = new HashMap<>();
    protected final Map<String, Object> citProperties = new HashMap<>();
    protected boolean valid;
    protected final SortedSet<String> journals = new TreeSet<>();

    /**
     * Construct from user-provided style file.
     */
    public OOBibStyle(File styleFile,
                      LayoutFormatterPreferences prefs,
                      Charset encoding) throws IOException {
        this.prefs = Objects.requireNonNull(prefs);
        this.styleFile = Objects.requireNonNull(styleFile);
        this.encoding = Objects.requireNonNull(encoding);
        setDefaultProperties();
        reload();
        fromResource = false;
        path = styleFile.getPath();
    }

    /**
     * Construct from resource.
     */
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
        citProperties.put(AUTHOR_FIELD,
                          FieldFactory.serializeOrFields(StandardField.AUTHOR,
                                                         StandardField.EDITOR));
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

    public Layout getDefaultBibLayout() {
        return defaultBibLayout;
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

    /**
     * Note: current values are not reset: there may remain data from
     * earlier versions of the input. This may be confusing for the
     * user (modified file until it works, but next time it fails again).
     */
    private void initialize(InputStream stream) throws IOException {
        Objects.requireNonNull(stream);

        // https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
        //
        // The try-with-resources Statement
        //
        try (Reader reader = new InputStreamReader(stream, encoding)) {
            OOBibStyleParser.readFormatFile(reader, this);
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
     * After initializing this style from a file, this method can be used to check
     * whether the file appeared to be a proper style file.
     *
     * @return true if the file could be parsed as a style file, false otherwise.
     */
    public boolean isValid() {
        return valid;
    }

    public Layout getReferenceFormat(EntryType type) {
        Layout l = bibLayout.get(type);
        if (l == null) {
            return defaultBibLayout;
        } else {
            return l;
        }
    }

    public static String regularizePageInfo( String p ) {
        if (p == null) {
            return null;
        }
        String pt = p.trim();
        if (pt.equals("")) {
            return null;
        } else {
            return pt;
        }
    }

    /**
     *  Make sure that (1) we have exactly one entry for each
     *  citation, (2) each entry is either null or is not empty when trimmed.
     */
    public static List<String> regularizePageInfosForCitations(List<String> pageInfosForCitations,
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
                res.add(regularizePageInfo(p));
            }
            return res;
        }
    }


    /**
     * Defines sort order for pageInfo strings.
     *
     * null comes before non-null
     */
    public static int comparePageInfo(String a, String b) {
        String aa = regularizePageInfo(a);
        String bb = regularizePageInfo(b);
        if (aa == null && bb == null) {
            return 0;
        }
        if (aa == null) {
            return -1;
        }
        if (bb == null) {
            return +1;
        }
        return aa.compareTo(bb);
    }

    /**
     * See {@see getNumCitationMarkerCommon} for details.
     */
    public String getNumCitationMarkerForInText(List<Integer> numbers,
                                                int minGroupingCount,
                                                List<String> pageInfosForCitations) {
        return OOBibStyleGetNumCitationMarker.getNumCitationMarkerForInText(this,
                                                                            numbers,
                                                                            minGroupingCount,
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
        return OOBibStyleGetNumCitationMarker.getNumCitationMarkerForBibliography(this,
                                                                                  number);
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

        OOBibStyle style = this;
        return OOBibStyleGetCitationMarker.getCitationMarker(style,
                                                             entries,
                                                             database,
                                                             inParenthesis,
                                                             uniquefiers,
                                                             unlimAuthors,
                                                             pageInfosForCitations);
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

    public boolean getCitPropertyMultiCiteChronological() {
        return this.getBooleanCitProperty(OOBibStyle.MULTI_CITE_CHRONOLOGICAL);
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
}
