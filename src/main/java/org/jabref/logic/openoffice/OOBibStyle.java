package org.jabref.logic.openoffice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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

import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;

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

    /** Unlike many others below, UNDEFINED_CITATION_MARKER is not a
     *  key to properties or citProperties
     */
    public static final String UNDEFINED_CITATION_MARKER = "??";

    /*
     * Keys for the PROPERTIES section of a *.jstyle file.
     */
    private static final String TITLE = "Title";

    private static final String IS_SORT_BY_POSITION = "IsSortByPosition";
    private static final String IS_NUMBER_ENTRIES = "IsNumberEntries";
    private static final String REFERENCE_HEADER_PARAGRAPH_FORMAT = "ReferenceHeaderParagraphFormat";
    private static final String REFERENCE_PARAGRAPH_FORMAT = "ReferenceParagraphFormat";

    /*
     * Keys for the CITATION section of a *.jstyle file.
     */

    /*
     * general
     */
    private static final String CITATION_KEY_CITATIONS = "BibTeXKeyCitations";
    private static final String MULTI_CITE_CHRONOLOGICAL = "MultiCiteChronological";

    // general / formatting citations
    private static final String FORMAT_CITATIONS = "FormatCitations";
    private static final String CITATION_CHARACTER_FORMAT = "CitationCharacterFormat";

    // TODO:  ITALIC_CITATIONS ItalicCitations is not implemented
    // status:
    // - jstyles.jabref.org-master : 4 styles mention it, value is false in all of them.
    // - getter: isItalicCitations() exists, but only called from tests, to assert it is false.
    // - default: citProperties.put(ITALIC_CITATIONS, Boolean.FALSE);
    //
    private static final String ITALIC_CITATIONS = "ItalicCitations";

    // TODO: BOLD_CITATIONS is not implemented
    private static final String BOLD_CITATIONS = "BoldCitations";

    // TODO: SUBSCRIPT_CITATIONS, SUPERSCRIPT_CITATIONS are not implemented
    private static final String SUBSCRIPT_CITATIONS = "SubscriptCitations";
    private static final String SUPERSCRIPT_CITATIONS = "SuperscriptCitations";

    /*
     * common (numeric and author-year)
     */

    /** "{[}Smith 2000]"  "Smith {[}2000]" "{[}1]"  */
    private static final String BRACKET_BEFORE = "BracketBefore";

    /** "[Smith 2000{]}" "Smith [2000{]}" "[1{]}" */
    private static final String BRACKET_AFTER = "BracketAfter";

    /** Entry labels in bibliography: "{[}1]" */
    private static final String BRACKET_BEFORE_IN_LIST = "BracketBeforeInList";

    /** Entry labels in bibliography: "[1{]}" */
    private static final String BRACKET_AFTER_IN_LIST = "BracketAfterInList";

    /** "[Smith 2000a{; }pp 10-13]" "Smith [2000a{; }pp 10-13]" "[1{; }pp 10-13]" */
    private static final String PAGE_INFO_SEPARATOR = "PageInfoSeparator";

    /** "[Smith 2000{; }Jones 2001]" "Smith [2000{; }Jones [2001]" "[1{; }2]" */
    private static final String CITATION_SEPARATOR = "CitationSeparator";

    /*
     * numeric
     */

    /** How many consecutive numbers in "[1; 2; 3]" allows grouping to "[1-3]" */
    private static final String MINIMUM_GROUPING_COUNT = "MinimumGroupingCount";

    /** "[1{-}3]" */
    private static final String GROUPED_NUMBERS_SEPARATOR = "GroupedNumbersSeparator";

    /*
     * author-year
     */

    /** Name of field that contains the authors. May be a list: "author/editor" */
    private static final String AUTHOR_FIELD = "AuthorField";

    /** How many authors to show before switching to "et al." */
    private static final String MAX_AUTHORS = "MaxAuthors";

    /** The first appearance of a source may have a higher limit. */
    private static final String MAX_AUTHORS_FIRST = "MaxAuthorsFirst";

    /**  "[Smith{, }Jones, and Brown 2000]" */
    private static final String AUTHOR_SEPARATOR = "AuthorSeparator";

    /**  "[Smith, Jones{,} and Brown 2000]" */
    private static final String OXFORD_COMMA = "OxfordComma";

    /**  "[Smith, Jones,{ and }Brown 2000]" */
    private static final String AUTHOR_LAST_SEPARATOR = "AuthorLastSeparator";

    /**  "Smith, Jones,{ and }Brown [2000]" */
    private static final String AUTHOR_LAST_SEPARATOR_IN_TEXT = "AuthorLastSeparatorInText";

    private static final String ITALIC_ET_AL = "ItalicEtAl";
    /**  "[Smith{ et al.} 2000]" */
    private static final String ET_AL_STRING = "EtAlString";

    /**  "[Smith et al.{ }2000]" */
    private static final String YEAR_SEPARATOR = "YearSeparator";

    /**  "Smith et al.{ }[2000]" */
    private static final String IN_TEXT_YEAR_SEPARATOR = "InTextYearSeparator";

    /** Name of field that contains the year. May be a list "year/anotherFieldWithYear" */
    private static final String YEAR_FIELD = "YearField";

    /** "[Smith et al. 2000a{,}b; pp 10-13]" */
    private static final String UNIQUEFIER_SEPARATOR = "UniquefierSeparator";
    // end of keys

    // private static final Logger LOGGER = LoggerFactory.getLogger(OOBibStyle.class);

    /** Formatter to be run on fields before they are used as part of citation marker.
     *
     * Usage: String output = fieldFormatter.format(input);
     *
     * Input: LaTeX, output: unicode with a small set of HTML tags
     * indicating character formatting.
     *
     * Note: OOPreFormatter is stateless, the above call is equivalent to
     *
     *  String output = OOPreFormatter.latexToUnicodeWithHtmlTags(input);
     *
     */
    protected final LayoutFormatter fieldFormatter = new OOPreFormatter();

    /*
     * Used or modified in OOBibStyleParser.readFormatFile()
     */
    String localCopy;

    String obsName = "";
    final SortedSet<String> journals = new TreeSet<>();

    final Map<String, Object> obsProperties = new HashMap<>();
    final Map<String, Object> obsCitProperties = new HashMap<>();

    // reference layout mapped from entry type:
    final Map<EntryType, Layout> bibLayout = new HashMap<>();
    Layout defaultBibLayout;
    boolean isDefaultLayoutPresent;

    final LayoutFormatterPreferences prefs;
    boolean valid;

    /** Messages from that parser.
     *
     *  Since we are parsing from the constructor, there is no way to
     *  return it to the caller there.
     */
    OOBibStyleParser.ParseLog parseLog;


    private final boolean fromResource;
    private final String path;
    private final Charset encoding;

    private File styleFile;
    private long styleFileModificationTime = Long.MIN_VALUE;

    /**
     * Construct from user-provided style file.
     */
    public OOBibStyle(File styleFile,
                      LayoutFormatterPreferences prefs,
                      Charset encoding) throws IOException {
        this.prefs = Objects.requireNonNull(prefs);
        this.styleFile = Objects.requireNonNull(styleFile);
        this.encoding = Objects.requireNonNull(encoding);
        // setDefaultProperties(); // moved into initialize()
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
        // setDefaultProperties(); // moved into initialize()
        initialize(OOBibStyle.class.getResourceAsStream(resourcePath), resourcePath);
        fromResource = true;
        path = resourcePath;
    }

    private void setDefaultProperties() {
        // Set default property values:
        obsProperties.put(TITLE, "Bibliography");

        obsProperties.put(IS_SORT_BY_POSITION, Boolean.FALSE);
        obsProperties.put(IS_NUMBER_ENTRIES, Boolean.FALSE);

        obsProperties.put(REFERENCE_PARAGRAPH_FORMAT, "Default");
        obsProperties.put(REFERENCE_HEADER_PARAGRAPH_FORMAT, "Heading 1");

        // Set default obsCitProperties for the citation marker:
        obsCitProperties.put(AUTHOR_FIELD,
                          FieldFactory.serializeOrFields(StandardField.AUTHOR,
                                                         StandardField.EDITOR));
        obsCitProperties.put(YEAR_FIELD, StandardField.YEAR.getName());
        obsCitProperties.put(MAX_AUTHORS, 3);
        obsCitProperties.put(MAX_AUTHORS_FIRST, -1);
        obsCitProperties.put(AUTHOR_SEPARATOR, ", ");
        obsCitProperties.put(AUTHOR_LAST_SEPARATOR, " & ");
        obsCitProperties.put(AUTHOR_LAST_SEPARATOR_IN_TEXT, null);
        obsCitProperties.put(ET_AL_STRING, " et al.");
        obsCitProperties.put(YEAR_SEPARATOR, ", ");
        obsCitProperties.put(IN_TEXT_YEAR_SEPARATOR, " ");

        obsCitProperties.put(BRACKET_BEFORE, "(");
        obsCitProperties.put(BRACKET_AFTER, ")");

        obsCitProperties.put(CITATION_SEPARATOR, "; ");
        obsCitProperties.put(PAGE_INFO_SEPARATOR, "; ");
        obsCitProperties.put(GROUPED_NUMBERS_SEPARATOR, "-");
        obsCitProperties.put(MINIMUM_GROUPING_COUNT, 3);
        obsCitProperties.put(FORMAT_CITATIONS, Boolean.FALSE);
        obsCitProperties.put(CITATION_CHARACTER_FORMAT, "Default");
        obsCitProperties.put(ITALIC_CITATIONS, Boolean.FALSE);
        obsCitProperties.put(BOLD_CITATIONS, Boolean.FALSE);
        obsCitProperties.put(SUPERSCRIPT_CITATIONS, Boolean.FALSE);
        obsCitProperties.put(SUBSCRIPT_CITATIONS, Boolean.FALSE);
        obsCitProperties.put(MULTI_CITE_CHRONOLOGICAL, Boolean.TRUE);
        obsCitProperties.put(CITATION_KEY_CITATIONS, Boolean.FALSE); //"BibTeXKeyCitations"
        obsCitProperties.put(ITALIC_ET_AL, Boolean.FALSE);
        obsCitProperties.put(OXFORD_COMMA, "");
    }

    //    public Layout getDefaultBibLayout() {
    //        return defaultBibLayout;
    //    }

    public String getName() {
        return obsName;
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
    private void initialize(InputStream stream, String filename) throws IOException {
        Objects.requireNonNull(stream);


        // remove data from a previous parse
        localCopy = null;
        obsName = "";
        journals.clear();
        obsProperties.clear();
        obsCitProperties.clear();
        bibLayout.clear();
        defaultBibLayout = null;
        isDefaultLayoutPresent = false;
        valid = false;
        setDefaultProperties();
        this.parseLog = null;

        // https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
        //
        // The try-with-resources Statement
        //
        try (Reader reader = new InputStreamReader(stream, encoding)) {
            this.parseLog = OOBibStyleParser.readFormatFile(reader, this, filename);
            // System.out.print(pp.format());
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
     */
    public void ensureUpToDate() throws IOException {
        if (!isUpToDate()) {
            reload();
        }
    }

    /**
     * If this style was initialized from a file on disk, reload the style
     * information.
     */
    private void reload() throws IOException {
        if (styleFile != null) {
            this.styleFileModificationTime = styleFile.lastModified();
            try (InputStream stream = new FileInputStream(styleFile)) {
                initialize(stream, styleFile.getAbsolutePath());
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

    /**
     * May return null.
     */
    public OOBibStyleParser.ParseLog getParseLog() {
        return parseLog;
    }

    public Layout getReferenceFormat(EntryType type) {
        Layout l = bibLayout.get(type);
        if (l == null) {
            return defaultBibLayout;
        } else {
            return l;
        }
    }

    public static String regularizePageInfo(String p) {
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
     *  but "[1]" form for the bibliography, the style can provide
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

    public String getNormalizedCitationMarker(CitationMarkerEntry ce) {
        return OOBibStyleGetCitationMarker.getNormalizedCitationMarker(this, ce, Optional.empty());
    }

    /**
     * What should getCitationMarker do if it discovers that
     * uniqueLetters provided are not sufficient for unique presentation?
     */
    public enum NonUniqueCitationMarker {
        /** Give an insufficient representation anyway.  */
        FORGIVEN,
        /** Throw a RuntimeException */
        THROWS
    }

    /**
     * Format the marker for an the in-text citation according to
     * this style.
     *
     * Uniquefier letters are added as provided by the
     * CitationMarkerEntry.getUniqueLetterOrNull().
     *
     * If successive entries within the citation are uniquefied from
     * each other, this method will perform a grouping of these
     * entries.
     *
     * @param citationMarkerEntries The list of entries providing the
     *                              data.
     *
     * @param inParenthesis Signals whether a parenthesized citation
     *                      or an in-text citation is wanted.
     *
     * @param nonUniqueCitationMarkerHandling
     *
     *             THROWS : Should throw if finds that uniqueLetters
     *                      provided do not make the entries unique.
     *
     *             FORGIVEN : is needed to allow preliminary markers
     *                        for freshly inserted citations without
     *                        going throw the uniquefication process.
     *
     * @return The formatted citation.
     */
    public String getCitationMarker(List<CitationMarkerEntry> citationMarkerEntries,
                                    boolean inParenthesis,
                                    NonUniqueCitationMarker nonUniqueCitationMarkerHandling) {
        return OOBibStyleGetCitationMarker.getCitationMarker(this,
                                                             citationMarkerEntries,
                                                             inParenthesis,
                                                             nonUniqueCitationMarkerHandling);
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

    private String nullToEmpty(String s) {
        return ( s == null ? "" : s );
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof OOBibStyle) {
            OOBibStyle otherStyle = (OOBibStyle) o;
            return (Objects.equals(path, otherStyle.path)
                    && (nullToEmpty(localCopy)
                        .equals(nullToEmpty(otherStyle.localCopy))));
            //            return (Objects.equals(path, otherStyle.path)
            //                    && Objects.equals(obsName, otherStyle.obsName)
            //                    && Objects.equals(obsCitProperties, otherStyle.obsCitProperties)
            //                    && Objects.equals(obsProperties, otherStyle.obsProperties)
            //                    // bibLayout does no count?
            //                );
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, obsName, obsCitProperties, obsProperties);
    }

    /*
     *
     *  Property getters
     *
     */

    // /**
    //  * Get a style property.
    //  *
    //  * @param propName The property name.
    //  * @return The property value, or null if it doesn't exist.
    //  */
    // private Object getProperty(String propName) {
    //     return obsProperties.get(propName);
    // }

    private boolean getBooleanProperty(String propName) {
        return (Boolean) obsProperties.get(propName);
    }

    private String getStringProperty(String propName) {
        return (String) obsProperties.get(propName);
    }

    //    private int getIntProperty(String key) {
    //        return (Integer) obsProperties.get(key);
    //    }

    /**
     * Get boolean property.
     *
     * @param key The property key
     * @return the value
     */
    private boolean getBooleanCitProperty(String key) {
        return (Boolean) obsCitProperties.get(key);
    }

    private int getIntCitProperty(String key) {
        return (Integer) obsCitProperties.get(key);
    }

    public String getStringCitProperty(String key) {
        return (String) obsCitProperties.get(key);
    }

    /**
     * Minimal number of consecutive citation numbers needed to start
     * replacing with an range like "10-13".
     */
    public int getMinimumGroupingCount() {
        return getIntCitProperty(OOBibStyle.MINIMUM_GROUPING_COUNT);
    }

    /**
     * Used in number ranges like "10-13" in numbered citations.
     */
    public String getGroupedNumbersSeparator() {
        return getStringCitProperty(OOBibStyle.GROUPED_NUMBERS_SEPARATOR);
    }

    /**
     * Shall we use number citations or author-year citations.
     *
     * @return true if we use numbered citations, false otherwise.
     */
    public boolean isNumberEntries() {
        return getBooleanProperty(IS_NUMBER_ENTRIES);
    }

    /**
     * Shall we sort the bibliography entries according to their order
     * of first appearance in the text.
     *
     * @return true to sort by order of appearance, false to sort alphabetically.
     */
    public boolean isSortByPosition() {
        return getBooleanProperty(IS_SORT_BY_POSITION);
    }

    /**
     * Should citation markers be formatted
     * according to the results of the
     *  - isItalicCitations() // not implemented
     *  - isBoldCitations()   // not implemented
     *  - getCitationCharacterFormat() // yes
     *  methods?
     *
     * There is also SUPERSCRIPT_CITATIONS, SUBSCRIPT_CITATIONS
     *
     * @return true to indicate that citations should be formatted to getCitationCharacterFormat()
     *          in italics (or bold).
     */
    public boolean getFormatCitations() {
        return (Boolean) obsCitProperties.get(FORMAT_CITATIONS);
    }

    public String getCitationCharacterFormat() {
        return getStringCitProperty(CITATION_CHARACTER_FORMAT);
    }

    /**
     * Should citation markers be italicized?
     * Only relevant if getFormatCitations() returns true.
     *
     * @return true to indicate that citations should be in italics.
     *
     * TODO: unused
     */
    public boolean isItalicCitations() {
        return getBooleanCitProperty(ITALIC_CITATIONS);
    }

    /**
     * Should citation markers be bold?
     * Only relevant if getFormatCitations() returns true.
     *
     * @return true to indicate that citations should be in bold.
     *
     * TODO: unused
     */
    public boolean isBoldCitations() {
        return (Boolean) obsCitProperties.get(BOLD_CITATIONS);
    }

    

    public boolean isCitationKeyCiteMarkers() {
        return (Boolean) obsCitProperties.get(CITATION_KEY_CITATIONS);
    }

    public boolean getCitPropertyMultiCiteChronological() {
        return this.getBooleanCitProperty(OOBibStyle.MULTI_CITE_CHRONOLOGICAL);
    }

    public boolean getCitPropertyItalicEtAl() {
        return this.getBooleanCitProperty(OOBibStyle.ITALIC_ET_AL);
    }

    /**
     *  @return Names of fields containing authors: the first
     *  non-empty field is will be used.
     */
    protected OrFields getAuthorFieldNames() {
        String authorFieldNamesString = this.getStringCitProperty(OOBibStyle.AUTHOR_FIELD);
        return FieldFactory.parseOrFields(authorFieldNamesString);
    }

    /**
     *  @return Field containing year, with fallback fields.
     */
    protected OrFields getYearFieldNames() {
        String yearFieldNamesString = this.getStringCitProperty(OOBibStyle.YEAR_FIELD);
        return FieldFactory.parseOrFields(yearFieldNamesString);
    }

    /* The String to add between the two last author names, e.g. " & ". */
    protected String getAuthorLastSeparator() {
        return getStringCitProperty(OOBibStyle.AUTHOR_LAST_SEPARATOR);
    }

    /* As getAuthorLastSeparator, for in-text citation. */
    protected String getAuthorLastSeparatorInTextWithFallBack() {
        return Objects.requireNonNullElse(
            getStringCitProperty(OOBibStyle.AUTHOR_LAST_SEPARATOR_IN_TEXT),
            // Use the default one if no explicit separator for text is defined
            getStringCitProperty(OOBibStyle.AUTHOR_LAST_SEPARATOR));
    }

    protected String getPageInfoSeparator() {
        return getStringCitProperty(OOBibStyle.PAGE_INFO_SEPARATOR);
    }

    protected String getUniquefierSeparator() {
        return getStringCitProperty(OOBibStyle.UNIQUEFIER_SEPARATOR);
    }

    protected String getCitationSeparator() {
        return getStringCitProperty(OOBibStyle.CITATION_SEPARATOR);
    }

    protected String getYearSeparator() {
        return getStringCitProperty(OOBibStyle.YEAR_SEPARATOR);
    }

    protected String getYearSeparatorInText() {
        return getStringCitProperty(OOBibStyle.IN_TEXT_YEAR_SEPARATOR);
    }

    /** The maximum number of authors to write out in full without
     *  using "et al." Set to -1 to always write out all authors.
     */
    protected int getMaxAuthors() {
        return getIntCitProperty(OOBibStyle.MAX_AUTHORS);
    }

    public int getMaxAuthorsFirst() {
        return getIntCitProperty(OOBibStyle.MAX_AUTHORS_FIRST);
    }

    /** Opening parenthesis before citation (or year, for in-text) */
    protected String getBracketBefore() {
        return getStringCitProperty(OOBibStyle.BRACKET_BEFORE);
    }

    /** Closing parenthesis after citation */
    protected String getBracketAfter() {
        return getStringCitProperty(OOBibStyle.BRACKET_AFTER);
    }

    /** Opening parenthesis before citation marker in the bibliography. */
    private String getBracketBeforeInList() {
        return getStringCitProperty(OOBibStyle.BRACKET_BEFORE_IN_LIST);
    }

    protected String getBracketBeforeInListWithFallBack() {
        return Objects.requireNonNullElse(getBracketBeforeInList(),
                                          getBracketBefore());
    }

    /** Closing parenthesis after citation marker in the bibliography */
    private String getBracketAfterInList() {
        return getStringCitProperty(OOBibStyle.BRACKET_AFTER_IN_LIST);
    }

    protected String getBracketAfterInListWithFallBack() {
        return Objects.requireNonNullElse(getBracketAfterInList(),
                                          getBracketAfter());
    }

    /** The String to represent authors that are not mentioned,
     * e.g. " et al."
     */
    public String getEtAlString() {
        return getStringCitProperty(OOBibStyle.ET_AL_STRING);
    }

    /** The String to add between author names except the last two:
     *  "[Smith{, }Jones and Brown]"
     */
    protected String getAuthorSeparator() {
        return getStringCitProperty(OOBibStyle.AUTHOR_SEPARATOR);
    }

    /** The String to put after the second to last author in case
     *  of three or more authors: (A, B{,} and C)
     */
    protected String getOxfordComma() {
        return getStringCitProperty(OOBibStyle.OXFORD_COMMA);
    }

    /**
     * Title for the bibliography.
     */
    public String getTitle() {
        return getStringProperty(OOBibStyle.TITLE);
    }

    /**
     * Name of paragraph format (within OO/LO) to be used for
     * the title of the bibliography.
     */
    public String getReferenceHeaderParagraphFormat() {
        return getStringProperty(OOBibStyle.REFERENCE_HEADER_PARAGRAPH_FORMAT);
    }

    /**
     * Name of paragraph format (within OO/LO) to be used for
     * the entries in the bibliography.
     */
    public String getReferenceParagraphFormat() {
        return getStringProperty(OOBibStyle.REFERENCE_PARAGRAPH_FORMAT);
    }

}
