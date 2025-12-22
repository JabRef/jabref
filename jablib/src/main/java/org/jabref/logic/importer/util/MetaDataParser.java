package org.jabref.logic.importer.util;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.logic.citationkeypattern.CitationKeyPattern;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanupActions;
import org.jabref.logic.cleanup.FieldFormatterCleanupMapper;
import org.jabref.logic.formatter.bibtexfields.NormalizeDateFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeMonthFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.layout.format.ReplaceUnicodeLigaturesFormatter;
import org.jabref.logic.util.Version;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypeBuilder;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.metadata.ContentSelectors;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writing is done at {@link org.jabref.logic.exporter.MetaDataSerializer}.
 */
public class MetaDataParser {

    public static final List<FieldFormatterCleanup> DEFAULT_SAVE_ACTIONS;
    private static final Logger LOGGER = LoggerFactory.getLogger(MetaDataParser.class);
    private static FileUpdateMonitor fileMonitor;
    private static final Pattern SINGLE_BACKSLASH = Pattern.compile("[^\\\\]\\\\[^\\\\]");

    static {
        DEFAULT_SAVE_ACTIONS = List.of(
                new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter()),
                new FieldFormatterCleanup(StandardField.DATE, new NormalizeDateFormatter()),
                new FieldFormatterCleanup(StandardField.MONTH, new NormalizeMonthFormatter()),
                new FieldFormatterCleanup(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD,
                        new ReplaceUnicodeLigaturesFormatter()));
    }

    public MetaDataParser(FileUpdateMonitor fileMonitor) {
        MetaDataParser.fileMonitor = fileMonitor;
    }

    public static Optional<BibEntryType> parseCustomEntryType(String comment) {
        String rest = comment.substring(MetaData.ENTRYTYPE_FLAG.length());
        int indexEndOfName = rest.indexOf(':');
        if (indexEndOfName < 0) {
            return Optional.empty();
        }
        String fieldsDescription = rest.substring(indexEndOfName + 2);

        int indexEndOfRequiredFields = fieldsDescription.indexOf(']');
        int indexEndOfOptionalFields = fieldsDescription.indexOf(']', indexEndOfRequiredFields + 1);
        if ((indexEndOfRequiredFields < 4) || (indexEndOfOptionalFields < (indexEndOfRequiredFields + 6))) {
            return Optional.empty();
        }
        EntryType type = EntryTypeFactory.parse(rest.substring(0, indexEndOfName));
        String reqFields = fieldsDescription.substring(4, indexEndOfRequiredFields);
        String optFields = fieldsDescription.substring(indexEndOfRequiredFields + 6, indexEndOfOptionalFields);
        BibEntryTypeBuilder entryTypeBuilder = new BibEntryTypeBuilder()
                .withType(type)
                .withRequiredFields(FieldFactory.parseOrFieldsList(reqFields))
                // Important fields are optional fields, but displayed first. Thus, they do not need to be separated by "/".
                // See org.jabref.model.entry.field.FieldPriority for details on important optional fields.
                .withImportantFields(FieldFactory.parseFieldList(optFields));
        if (entryTypeBuilder.hasWarnings()) {
            LOGGER.warn("Following custom entry type definition has duplicate fields: {}", comment);
            return Optional.empty();
        }
        return Optional.of(entryTypeBuilder.build());
    }

    /**
     * Parses the given data map and returns a new resulting {@link MetaData} instance.
     */
    public MetaData parse(Map<String, String> data, Character keywordSeparator, String userAndHost) throws ParseException {
        return parse(new MetaData(), data, keywordSeparator, userAndHost);
    }

    /**
     * Parses the data map and changes the given {@link MetaData} instance respectively.
     *
     * @return the given metaData instance (which is modified, too)
     */
    public MetaData parse(MetaData metaData, Map<String, String> data, Character keywordSeparator, String userAndHost) throws ParseException {
        CitationKeyPattern defaultCiteKeyPattern = CitationKeyPattern.NULL_CITATION_KEY_PATTERN;
        Map<EntryType, CitationKeyPattern> nonDefaultCiteKeyPatterns = new HashMap<>();

        // process groups (GROUPSTREE and GROUPSTREE_LEGACY) at the very end (otherwise it can happen that not all dependent data are set)
        List<Map.Entry<String, String>> entryList = new ArrayList<>(data.entrySet());
        entryList.sort(groupsLast());

        for (Map.Entry<String, String> entry : entryList) {
            List<String> values = getAsList(entry.getValue());

            if (entry.getKey().startsWith(MetaData.PREFIX_KEYPATTERN)) {
                EntryType entryType = EntryTypeFactory.parse(entry.getKey().substring(MetaData.PREFIX_KEYPATTERN.length()));
                nonDefaultCiteKeyPatterns.put(entryType, new CitationKeyPattern(getSingleItem(values)));
            } else if (entry.getKey().startsWith(MetaData.SELECTOR_META_PREFIX)) {
                // edge case, it might be one special field e.g. article from biblatex-apa, but we can't distinguish this from any other field and rather prefer to handle it as UnknownField
                metaData.addContentSelector(ContentSelectors.parse(FieldFactory.parseField(entry.getKey().substring(MetaData.SELECTOR_META_PREFIX.length())), StringUtil.unquote(entry.getValue(), MetaData.ESCAPE_CHARACTER)));
            } else if (MetaData.FILE_DIRECTORY.equals(entry.getKey())) {
                metaData.setLibrarySpecificFileDirectory(parseDirectory(entry.getValue()));
            } else if (entry.getKey().startsWith(MetaData.BLG_FILE_PATH + "-")) {
                handleBlgFilePathEntry(entry, metaData);
            } else if (entry.getKey().startsWith(MetaData.FILE_DIRECTORY + '-')) {
                // The user name starts directly after FILE_DIRECTORY + '-'
                String user = entry.getKey().substring(MetaData.FILE_DIRECTORY.length() + 1);
                metaData.setUserFileDirectory(user, parseDirectory(entry.getValue()));
            } else if (entry.getKey().startsWith(MetaData.FILE_DIRECTORY_LATEX)) {
                // The user-host string starts directly after FILE_DIRECTORY_LATEX + '-'
                String userHostString = entry.getKey().substring(MetaData.FILE_DIRECTORY_LATEX.length() + 1);
                metaData.setLatexFileDirectory(userHostString, parseDirectory(entry.getValue()));
            } else if (MetaData.SAVE_ACTIONS.equals(entry.getKey())) {
                metaData.setSaveActions(fieldFormatterCleanupsParse(values));
            } else if (MetaData.DATABASE_TYPE.equals(entry.getKey())) {
                metaData.setMode(BibDatabaseMode.parse(getSingleItem(values)));
            } else if (MetaData.KEYPATTERNDEFAULT.equals(entry.getKey())) {
                defaultCiteKeyPattern = new CitationKeyPattern(getSingleItem(values));
            } else if (MetaData.PROTECTED_FLAG_META.equals(entry.getKey())) {
                if (Boolean.parseBoolean(getSingleItem(values))) {
                    metaData.markAsProtected();
                } else {
                    metaData.markAsNotProtected();
                }
            } else if (MetaData.SAVE_ORDER_CONFIG.equals(entry.getKey())) {
                metaData.setSaveOrder(SaveOrder.parse(values));
            } else if (MetaData.GROUPSTREE.equals(entry.getKey()) || MetaData.GROUPSTREE_LEGACY.equals(entry.getKey())) {
                metaData.setGroups(GroupsParser.importGroups(values, keywordSeparator, fileMonitor, metaData, userAndHost));
            } else if (MetaData.GROUPS_SEARCH_SYNTAX_VERSION.equals(entry.getKey())) {
                Version version = Version.parse(getSingleItem(values));
                metaData.setGroupSearchSyntaxVersion(version);
            } else if (MetaData.VERSION_DB_STRUCT.equals(entry.getKey())) {
                metaData.setVersionDBStructure(getSingleItem(values));
            } else {
                // Keep meta data items that we do not know in the file
                metaData.putUnknownMetaDataItem(entry.getKey(), values);
            }
        }

        if (!defaultCiteKeyPattern.equals(CitationKeyPattern.NULL_CITATION_KEY_PATTERN) || !nonDefaultCiteKeyPatterns.isEmpty()) {
            metaData.setCiteKeyPattern(defaultCiteKeyPattern, nonDefaultCiteKeyPatterns);
        }

        return metaData;
    }

    /**
     * Parse the content of the value as provided by "raw" content.
     * <p>
     * We do not use unescaped value (created by @link{#getAsList(java.lang.String)}),
     * because this leads to difficulties with UNC names.
     * <p>
     * No normalization is done - the library-specific file directory could be passed as Mac OS X path, but the user could sit on Windows.
     *
     * @param value the raw value (as stored in the .bib file)
     */
    static String parseDirectory(String value) {
        value = StringUtil.removeStringAtTheEnd(value, MetaData.SEPARATOR_STRING);
        if (value.contains("\\\\\\\\")) {
            // This is an escaped Windows UNC path
            return value.replace("\\\\", "\\");
        } else if (value.contains("\\\\") && !SINGLE_BACKSLASH.matcher(value).find()) {
            // All backslashes escaped
            return value.replace("\\\\", "\\");
        } else {
            // No backslash escaping
            return value;
        }
    }

    private static Comparator<? super Map.Entry<String, String>> groupsLast() {
        return (s1, s2) -> MetaData.GROUPSTREE.equals(s1.getKey()) || MetaData.GROUPSTREE_LEGACY.equals(s1.getKey()) ? 1 :
                           MetaData.GROUPSTREE.equals(s2.getKey()) || MetaData.GROUPSTREE_LEGACY.equals(s2.getKey()) ? -1 : 0;
    }

    /**
     * Returns the first item in the list.
     * If the specified list does not contain exactly one item, then a {@link ParseException} will be thrown.
     */
    private static String getSingleItem(List<String> value) throws ParseException {
        if (value.size() == 1) {
            return value.getFirst();
        } else {
            throw new ParseException("Expected a single item but received " + value);
        }
    }

    private static List<String> getAsList(String value) throws ParseException {
        Reader valueReader = Reader.of(value);
        List<String> orderedValue = new ArrayList<>();

        // We must allow for ; and \ in escape sequences.
        try {
            Optional<String> unit;
            while ((unit = getNextUnit(valueReader)).isPresent()) {
                orderedValue.add(unit.get());
            }
        } catch (IOException ex) {
            LOGGER.error("Weird error while parsing meta data.", ex);
            throw new ParseException("Weird error while parsing meta data.", ex);
        }
        return orderedValue;
    }

    /**
     * Reads the next unit. Units are delimited by ';' (MetaData.SEPARATOR_CHARACTER).
     */
    private static Optional<String> getNextUnit(Reader reader) throws IOException {
        int c;
        boolean escape = false;
        StringBuilder res = new StringBuilder();
        while ((c = reader.read()) != -1) {
            if (escape) {
                // at org.jabref.logic.exporter.MetaDataSerializer.serializeMetaData, only MetaData.SEPARATOR_CHARACTER, MetaData.ESCAPE_CHARACTER are quoted
                // That means ; and \\
                char character = (char) c;
                if (character != MetaData.SEPARATOR_CHARACTER && character != MetaData.ESCAPE_CHARACTER) {
                    // Keep the escape character
                    res.append("\\");
                }
                res.append(character);
                escape = false;
            } else if (c == MetaData.ESCAPE_CHARACTER) {
                escape = true;
            } else if (c == MetaData.SEPARATOR_CHARACTER) {
                break;
            } else {
                res.append((char) c);
            }
        }
        if (!res.isEmpty()) {
            return Optional.of(res.toString());
        }
        return Optional.empty();
    }

    public static FieldFormatterCleanupActions fieldFormatterCleanupsParse(List<String> formatterMetaList) {
        if ((formatterMetaList != null) && (formatterMetaList.size() >= 2)) {
            boolean enablementStatus = FieldFormatterCleanupActions.ENABLED.equals(formatterMetaList.getFirst());
            String formatterString = formatterMetaList.get(1);

            return new FieldFormatterCleanupActions(enablementStatus, FieldFormatterCleanupMapper.parseActions(formatterString));
        } else {
            // return default actions
            return new FieldFormatterCleanupActions(false, DEFAULT_SAVE_ACTIONS);
        }
    }

    /**
     * Handles a blgFilePath-* metadata entry. Expects exactly one valid path.
     *
     * @param entry    the metadata entry containing the user-specific .blg path.
     * @param metaData the MetaData object to update.
     */
    private void handleBlgFilePathEntry(Map.Entry<String, String> entry, MetaData metaData) {
        String user = entry.getKey().substring(MetaData.BLG_FILE_PATH.length() + 1);
        List<String> values;
        try {
            values = getAsList(entry.getValue());
        } catch (ParseException e) {
            LOGGER.error("Invalid .blg metadata format for user {}", user, e);
            return;
        }
        if (values.size() != 1) {
            LOGGER.error("Expected single .blg path entry for user {}, but got {}", user, values);
            return;
        }
        String pathStr = values.getFirst();
        try {
            Path path = Path.of(pathStr);
            metaData.setBlgFilePath(user, path);
        } catch (InvalidPathException e) {
            LOGGER.error("Invalid .blg file path for user '{}': '{}'", user, pathStr, e);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Illegal argument in .blg file path '{}' for user '{}'", pathStr, user, e);
        }
    }
}
