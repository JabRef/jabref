package org.jabref.logic.openoffice.style;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.openoffice.ootext.OOFormat;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.CitationMarkerEntry;
import org.jabref.model.openoffice.style.CitationMarkerNormEntry;
import org.jabref.model.openoffice.style.CitationMarkerNumericBibEntry;
import org.jabref.model.openoffice.style.CitationMarkerNumericEntry;
import org.jabref.model.openoffice.style.NonUniqueCitationMarker;

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

    // These two can do what ItalicCitations, BoldCitations,
    // SuperscriptCitations and SubscriptCitations were supposed to do,
    // as well as underline smallcaps and strikeout.
    private static final String CITATION_GROUP_MARKUP_BEFORE = "CitationGroupMarkupBefore";
    private static final String CITATION_GROUP_MARKUP_AFTER = "CitationGroupMarkupAfter";

    private static final String AUTHORS_PART_MARKUP_BEFORE = "AuthorsPartMarkupBefore";
    private static final String AUTHORS_PART_MARKUP_AFTER = "AuthorsPartMarkupAfter";

    private static final String AUTHOR_NAMES_LIST_MARKUP_BEFORE = "AuthorNamesListMarkupBefore";
    private static final String AUTHOR_NAMES_LIST_MARKUP_AFTER = "AuthorNamesListMarkupAfter";

    private static final String AUTHOR_NAME_MARKUP_BEFORE = "AuthorNameMarkupBefore";
    private static final String AUTHOR_NAME_MARKUP_AFTER = "AuthorNameMarkupAfter";

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
    private final LayoutFormatterPreferences prefs;
    private String name = "";
    private Layout defaultBibLayout;
    private boolean valid;
    private Path styleFile;
    private long styleFileModificationTime = Long.MIN_VALUE;
    private String localCopy;
    private boolean isDefaultLayoutPresent;

    public OOBibStyle(Path styleFile, LayoutFormatterPreferences prefs) throws IOException {
        this.prefs = Objects.requireNonNull(prefs);
        this.styleFile = Objects.requireNonNull(styleFile);
        setDefaultProperties();
        reload();
        fromResource = false;
        path = styleFile.toAbsolutePath().toString();
    }

    public OOBibStyle(String resourcePath, LayoutFormatterPreferences prefs) throws IOException {
        this.prefs = Objects.requireNonNull(prefs);
        Objects.requireNonNull(resourcePath);
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
        properties.put(REFERENCE_PARAGRAPH_FORMAT, "Standard");
        properties.put(REFERENCE_HEADER_PARAGRAPH_FORMAT, "Heading 1");

        // Set default properties for the citation marker:
        citProperties.put(AUTHOR_FIELD, FieldFactory.serializeOrFields(StandardField.AUTHOR, StandardField.EDITOR));

        citProperties.put(CITATION_GROUP_MARKUP_BEFORE, "");
        citProperties.put(CITATION_GROUP_MARKUP_AFTER, "");

        citProperties.put(AUTHORS_PART_MARKUP_BEFORE, "");
        citProperties.put(AUTHORS_PART_MARKUP_AFTER, "");

        citProperties.put(AUTHOR_NAMES_LIST_MARKUP_BEFORE, "");
        citProperties.put(AUTHOR_NAMES_LIST_MARKUP_AFTER, "");

        citProperties.put(AUTHOR_NAME_MARKUP_BEFORE, "");
        citProperties.put(AUTHOR_NAME_MARKUP_AFTER, "");

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
        citProperties.put(CITATION_CHARACTER_FORMAT, "Standard");
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

    public Path getFile() {
        return styleFile;
    }

    public Set<String> getJournals() {
        return Collections.unmodifiableSet(journals);
    }

    private void initialize(InputStream stream) throws IOException {
        Objects.requireNonNull(stream);

        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
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
            this.styleFileModificationTime = Files.getLastModifiedTime(styleFile).toMillis();
            try (InputStream stream = Files.newInputStream(styleFile)) {
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
            try {
                return Files.getLastModifiedTime(styleFile).toMillis() == this.styleFileModificationTime;
            } catch (IOException e) {
                return false;
            }
        }
    }

    private void readFormatFile(Reader input) throws IOException {

        // First read all the contents of the file:
        StringBuilder stringBuilder = new StringBuilder();
        int chr;
        while ((chr = input.read()) != -1) {
            stringBuilder.append((char) chr);
        }

        // Store a local copy for viewing
        localCopy = stringBuilder.toString();

        // Break into separate lines:
        String[] lines = stringBuilder.toString().split("\n");
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
                final String typeName = line.substring(0, index);
                final String formatString = line.substring(index + 1);
                Layout layout = new LayoutHelper(new StringReader(formatString), this.prefs).getLayoutFromText();
                EntryType type = EntryTypeFactory.parse(typeName);

                if (!isDefaultLayoutPresent && OOBibStyle.DEFAULT_MARK.equals(typeName)) {
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
            if (NUM_PATTERN.matcher(value.trim()).matches()) {
                toSet = Integer.parseInt(value.trim());
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
        Layout layout = bibLayout.get(type);
        if (layout == null) {
            return defaultBibLayout;
        } else {
            return layout;
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
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof OOBibStyle) {
            OOBibStyle otherStyle = (OOBibStyle) object;
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

    enum BibStyleMode {
        NONE,
        LAYOUT,
        PROPERTIES,
        CITATION,
        NAME,
        JOURNALS
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
    public OOText getReferenceHeaderText() {
        return OOText.fromString(getStringProperty(OOBibStyle.TITLE));
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

    protected LayoutFormatter getFieldFormatter() {
        return fieldFormatter;
    }

    protected Map<EntryType, Layout> getBibLayout() {
        return bibLayout;
    }

    protected Map<String, Object> getProperties() {
        return properties;
    }

    protected Map<String, Object> getCitProperties() {
        return citProperties;
    }

    protected void addJournal(String journalName) {
        journals.add(journalName);
    }

    protected void setLocalCopy(String contentsOfJstyleFile) {
        localCopy = contentsOfJstyleFile;
    }

    protected void setName(String nameOfTheStyle) {
        name = nameOfTheStyle;
    }

    protected boolean getIsDefaultLayoutPresent() {
        return isDefaultLayoutPresent;
    }

    protected void setIsDefaultLayoutPresent(boolean isPresent) {
        isDefaultLayoutPresent = isPresent;
    }

    protected void setValid(boolean isValid) {
        valid = isValid;
    }

    protected LayoutFormatterPreferences getPrefs() {
        return prefs;
    }

    protected void setDefaultBibLayout(Layout layout) {
        defaultBibLayout = layout;
    }

    /**
     * Format a number-based citation marker for the given entries.
     *
     * @return The text for the citation.
     */
    public OOText getNumCitationMarker2(List<CitationMarkerNumericEntry> entries) {
        final int minGroupingCount = this.getMinimumGroupingCount();
        return OOBibStyleGetNumCitationMarker.getNumCitationMarker2(this,
                                                                    entries,
                                                                    minGroupingCount);
    }

    /**
     * For some tests we need to override minGroupingCount.
     */
    public OOText getNumCitationMarker2(List<CitationMarkerNumericEntry> entries,
                                        int minGroupingCount) {
        return OOBibStyleGetNumCitationMarker.getNumCitationMarker2(this,
                                                                    entries,
                                                                    minGroupingCount);
    }

    /**
     * Format a number-based bibliography label for the given number.
     */
    public OOText getNumCitationMarkerForBibliography(CitationMarkerNumericBibEntry entry) {
        return OOBibStyleGetNumCitationMarker.getNumCitationMarkerForBibliography(this, entry);
    }

    public OOText getNormalizedCitationMarker(CitationMarkerNormEntry entry) {
        return OOBibStyleGetCitationMarker.getNormalizedCitationMarker(this, entry, Optional.empty());
    }

    /**
     * Format the marker for the in-text citation according to this
     * BIB style. Uniquefier letters are added as provided by the
     * citationMarkerEntries argument. If successive entries within
     * the citation are uniquefied from each other, this method will
     * perform a grouping of these entries.
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
     * @return The formatted citation. The result does not include
     *         the standard wrappers:
     *         OOFormat.setLocaleNone() and OOFormat.setCharStyle().
     *         These are added by decorateCitationMarker()
     */
    public OOText createCitationMarker(List<CitationMarkerEntry> citationMarkerEntries,
                                       boolean inParenthesis,
                                       NonUniqueCitationMarker nonUniqueCitationMarkerHandling) {
        return OOBibStyleGetCitationMarker.createCitationMarker(this,
                                                                citationMarkerEntries,
                                                                inParenthesis,
                                                                nonUniqueCitationMarkerHandling);
    }

    /**
     * Add setLocaleNone and optionally setCharStyle(CitationCharacterFormat) around
     * citationText.  Called in fillCitationMarkInCursor, so these are
     * also applied to "Unresolved()" entries and numeric styles.
     */
    public OOText decorateCitationMarker(OOText citationText) {
        OOBibStyle style = this;
        OOText citationText2 = OOFormat.setLocaleNone(citationText);
        if (style.isFormatCitations()) {
            String charStyle = style.getCitationCharacterFormat();
            citationText2 = OOFormat.setCharStyle(citationText2, charStyle);
        }
        return citationText2;
    }

    /*
     *
     *  Property getters
     *
     */

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

    private String getStringProperty(String propName) {
        return (String) properties.get(propName);
    }

    /**
     * Should citation markers be italicized?
     *
     */
    public String getCitationGroupMarkupBefore() {
        return getStringCitProperty(CITATION_GROUP_MARKUP_BEFORE);
    }

    public String getCitationGroupMarkupAfter() {
        return getStringCitProperty(CITATION_GROUP_MARKUP_AFTER);
    }

    /** Author list, including " et al." */
    public String getAuthorsPartMarkupBefore() {
        return getStringCitProperty(AUTHORS_PART_MARKUP_BEFORE);
    }

    public String getAuthorsPartMarkupAfter() {
        return getStringCitProperty(AUTHORS_PART_MARKUP_AFTER);
    }

    /** Author list, excluding " et al." */
    public String getAuthorNamesListMarkupBefore() {
        return getStringCitProperty(AUTHOR_NAMES_LIST_MARKUP_BEFORE);
    }

    public String getAuthorNamesListMarkupAfter() {
        return getStringCitProperty(AUTHOR_NAMES_LIST_MARKUP_AFTER);
    }

    /** Author names. Excludes Author separators */
    public String getAuthorNameMarkupBefore() {
        return getStringCitProperty(AUTHOR_NAME_MARKUP_BEFORE);
    }

    public String getAuthorNameMarkupAfter() {
        return getStringCitProperty(AUTHOR_NAME_MARKUP_AFTER);
    }

    public boolean getMultiCiteChronological() {
        // "MultiCiteChronological"
        return this.getBooleanCitProperty(OOBibStyle.MULTI_CITE_CHRONOLOGICAL);
    }

    // Probably obsolete, now we can use " <i>et al.</i>" instead in EtAlString
    public boolean getItalicEtAl() {
        // "ItalicEtAl"
        return this.getBooleanCitProperty(OOBibStyle.ITALIC_ET_AL);
    }

    /**
     *  @return Names of fields containing authors: the first
     *  non-empty field will be used.
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
        String preferred = getStringCitProperty(OOBibStyle.AUTHOR_LAST_SEPARATOR_IN_TEXT);
        String fallback = getStringCitProperty(OOBibStyle.AUTHOR_LAST_SEPARATOR);
        return Objects.requireNonNullElse(preferred, fallback);
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

    public String getBracketBeforeInListWithFallBack() {
        return Objects.requireNonNullElse(getBracketBeforeInList(), getBracketBefore());
    }

    /** Closing parenthesis after citation marker in the bibliography */
    private String getBracketAfterInList() {
        return getStringCitProperty(OOBibStyle.BRACKET_AFTER_IN_LIST);
    }

    String getBracketAfterInListWithFallBack() {
        return Objects.requireNonNullElse(getBracketAfterInList(), getBracketAfter());
    }

    public OOText getFormattedBibliographyTitle() {
        OOBibStyle style = this;
        OOText title = style.getReferenceHeaderText();
        String parStyle = style.getReferenceHeaderParagraphFormat();
        return (parStyle == null
                ? OOFormat.paragraph(title)
                : OOFormat.paragraph(title, parStyle));
    }
}
