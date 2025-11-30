package org.jabref.logic.importer.util;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.logic.groups.GroupsFactory;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.MetadataSerializationConfiguration;
import org.jabref.logic.util.strings.QuotedStringTokenizer;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AutomaticDateGroup;
import org.jabref.model.groups.AutomaticKeywordGroup;
import org.jabref.model.groups.AutomaticPersonsGroup;
import org.jabref.model.groups.DateGranularity;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.KeywordGroup;
import org.jabref.model.groups.RegexKeywordGroup;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.groups.TexGroup;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts string representation of groups to a parsed {@link GroupTreeNode}.
 */
public class GroupsParser {

    /**
     * Identifier for SmartGroup (deprecated, replaced by {@link ExplicitGroup}).
     * Kept for backward compatibility during migration.
     *
     */
    @Deprecated
    private static final String SMART_GROUP_ID_FOR_MIGRATION = "SmartGroup:";

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupsParser.class);

    private GroupsParser() {
    }

    public static GroupTreeNode importGroups(List<String> orderedData,
                                             Character keywordSeparator,
                                             FileUpdateMonitor fileMonitor,
                                             MetaData metaData,
                                             String userAndHost)
            throws ParseException {
        try {
            GroupTreeNode cursor = null;
            GroupTreeNode root = null;
            for (String string : orderedData) {
                // This allows reading databases that have been modified by, e.g., BibDesk
                string = string.trim();
                if (string.isEmpty()) {
                    continue;
                }

                int spaceIndex = string.indexOf(' ');
                if (spaceIndex <= 0) {
                    throw new ParseException("Expected \"" + string + "\" to contain whitespace");
                }
                int level = Integer.parseInt(string.substring(0, spaceIndex));
                AbstractGroup group = GroupsParser.fromString(string.substring(spaceIndex + 1), keywordSeparator, fileMonitor, metaData, userAndHost);
                GroupTreeNode newNode = GroupTreeNode.fromGroup(group);
                if (cursor == null) {
                    // create new root
                    cursor = newNode;
                    root = cursor;
                } else {
                    // insert at desired location
                    while ((level <= cursor.getLevel()) && (cursor.getParent().isPresent())) {
                        cursor = cursor.getParent().get();
                    }
                    cursor.addChild(newNode);
                    cursor = newNode;
                }
            }
            return root;
        } catch (ParseException e) {
            throw new ParseException(Localization
                    .lang("Group tree could not be parsed. If you save the BibTeX library, all groups will be lost."),
                    e);
        }
    }

    /**
     * Re-create a group instance from a textual representation.
     *
     * @param input The result from the group's toString() method.
     * @return New instance of the encoded group.
     * @throws ParseException If an error occurred and a group could not be created, e.g. due to a malformed regular expression.
     */
    public static AbstractGroup fromString(String input, Character keywordSeparator, FileUpdateMonitor fileMonitor, MetaData metaData, String userAndHost)
            throws ParseException {
        if (input.startsWith(MetadataSerializationConfiguration.KEYWORD_GROUP_ID)) {
            return keywordGroupFromString(input, keywordSeparator);
        }
        if (input.startsWith(MetadataSerializationConfiguration.ALL_ENTRIES_GROUP_ID)) {
            return allEntriesGroupFromString(input);
        }
        if (input.startsWith(SMART_GROUP_ID_FOR_MIGRATION)) {
            // Migration: SmartGroup is replaced by ExplicitGroup
            return smartGroupFromString(input, keywordSeparator);
        }
        if (input.startsWith(MetadataSerializationConfiguration.SEARCH_GROUP_ID)) {
            return searchGroupFromString(input);
        }
        if (input.startsWith(MetadataSerializationConfiguration.EXPLICIT_GROUP_ID)) {
            return explicitGroupFromString(input, keywordSeparator);
        }
        if (input.startsWith(MetadataSerializationConfiguration.LEGACY_EXPLICIT_GROUP_ID)) {
            return legacyExplicitGroupFromString(input, keywordSeparator);
        }
        if (input.startsWith(MetadataSerializationConfiguration.AUTOMATIC_PERSONS_GROUP_ID)) {
            return automaticPersonsGroupFromString(input);
        }
        if (input.startsWith(MetadataSerializationConfiguration.AUTOMATIC_KEYWORD_GROUP_ID)) {
            return automaticKeywordGroupFromString(input);
        }
        if (input.startsWith(MetadataSerializationConfiguration.AUTOMATIC_DATE_GROUP_ID)) {
            return automaticDateGroupFromString(input);
        }
        if (input.startsWith(MetadataSerializationConfiguration.TEX_GROUP_ID)) {
            return texGroupFromString(input, fileMonitor, metaData, userAndHost);
        }

        throw new ParseException("Unknown group: " + input);
    }

    private static AbstractGroup texGroupFromString(String input, FileUpdateMonitor fileMonitor, MetaData metaData, String userAndHost) throws ParseException {
        assert input.startsWith(MetadataSerializationConfiguration.TEX_GROUP_ID);

        QuotedStringTokenizer token = new QuotedStringTokenizer(input.substring(MetadataSerializationConfiguration.TEX_GROUP_ID
                .length()), MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        String name = StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
        GroupHierarchyType context = GroupHierarchyType.getByNumberOrDefault(Integer.parseInt(token.nextToken()));
        try {
            Path path = Path.of(token.nextToken());
            try {
                TexGroup newGroup = TexGroup.create(name, context, path, new DefaultAuxParser(new BibDatabase()), fileMonitor, metaData, userAndHost);
                addGroupDetails(token, newGroup);
                return newGroup;
            } catch (IOException ex) {
                // Problem accessing file -> create without file monitoring
                LOGGER.warn("Could not access file {}. The group {} will not reflect changes to the aux file.", path, name, ex);

                TexGroup newGroup = TexGroup.create(name, context, path, new DefaultAuxParser(new BibDatabase()), metaData, userAndHost);
                addGroupDetails(token, newGroup);
                return newGroup;
            }
        } catch (InvalidPathException | IOException ex) {
            throw new ParseException(ex);
        }
    }

    private static AbstractGroup automaticPersonsGroupFromString(String input) {
        assert input.startsWith(MetadataSerializationConfiguration.AUTOMATIC_PERSONS_GROUP_ID);

        QuotedStringTokenizer token = new QuotedStringTokenizer(input.substring(MetadataSerializationConfiguration.AUTOMATIC_PERSONS_GROUP_ID
                .length()), MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        String name = StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
        GroupHierarchyType context = GroupHierarchyType.getByNumberOrDefault(Integer.parseInt(token.nextToken()));
        Field field = FieldFactory.parseField(StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));
        AutomaticPersonsGroup newGroup = new AutomaticPersonsGroup(name, context, field);
        addGroupDetails(token, newGroup);
        return newGroup;
    }

    private static AbstractGroup automaticDateGroupFromString(String input) {
        assert input.startsWith(MetadataSerializationConfiguration.AUTOMATIC_DATE_GROUP_ID);

        QuotedStringTokenizer token = new QuotedStringTokenizer(input.substring(MetadataSerializationConfiguration.AUTOMATIC_DATE_GROUP_ID
                .length()), MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        String name = StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
        GroupHierarchyType context = GroupHierarchyType.getByNumberOrDefault(Integer.parseInt(token.nextToken()));
        Field field = FieldFactory.parseField(StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));
        String granularityString = StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
        DateGranularity granularity = DateGranularity.valueOf(granularityString);
        AutomaticDateGroup newGroup = new AutomaticDateGroup(name, context, field, granularity);
        addGroupDetails(token, newGroup);
        return newGroup;
    }

    private static AbstractGroup automaticKeywordGroupFromString(String input) {
        assert input.startsWith(MetadataSerializationConfiguration.AUTOMATIC_KEYWORD_GROUP_ID);

        QuotedStringTokenizer token = new QuotedStringTokenizer(input.substring(MetadataSerializationConfiguration.AUTOMATIC_KEYWORD_GROUP_ID
                .length()), MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        String name = StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
        GroupHierarchyType context = GroupHierarchyType.getByNumberOrDefault(Integer.parseInt(token.nextToken()));
        Field field = FieldFactory.parseField(StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));
        Character delimiter = StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR).charAt(0);
        Character hierarchicalDelimiter = StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR).charAt(0);
        AutomaticKeywordGroup newGroup = new AutomaticKeywordGroup(name, context, field, delimiter, hierarchicalDelimiter);
        addGroupDetails(token, newGroup);
        return newGroup;
    }

    /**
     * Parses s and recreates the KeywordGroup from it.
     *
     * @param input The String representation obtained from KeywordGroup.toString()
     */
    private static KeywordGroup keywordGroupFromString(String input, Character keywordSeparator) {
        assert input.startsWith(MetadataSerializationConfiguration.KEYWORD_GROUP_ID);

        QuotedStringTokenizer token = new QuotedStringTokenizer(input.substring(MetadataSerializationConfiguration.KEYWORD_GROUP_ID
                .length()), MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        String name = StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
        GroupHierarchyType context = GroupHierarchyType.getByNumberOrDefault(Integer.parseInt(token.nextToken()));
        Field field = FieldFactory.parseField(StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));
        String expression = StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
        boolean caseSensitive = Integer.parseInt(token.nextToken()) == 1;
        boolean regExp = Integer.parseInt(token.nextToken()) == 1;
        KeywordGroup newGroup;
        if (regExp) {
            newGroup = new RegexKeywordGroup(name, context, field, expression, caseSensitive);
        } else {
            newGroup = new WordKeywordGroup(name, context, field, expression, caseSensitive, keywordSeparator, false);
        }
        addGroupDetails(token, newGroup);
        return newGroup;
    }

    /**
     * Migration method: Converts old SmartGroup serializations to ExplicitGroup.
     * SmartGroup has been replaced by ExplicitGroup for the "Imported Entries" group <a href="https://github.com/JabRef/jabref/issues/14143">Issue 14143</a>.
     */
    private static ExplicitGroup smartGroupFromString(String input, Character keywordSeparator) throws ParseException {
        assert input.startsWith(SMART_GROUP_ID_FOR_MIGRATION);

        input = input.replace(SMART_GROUP_ID_FOR_MIGRATION, MetadataSerializationConfiguration.EXPLICIT_GROUP_ID);
        return explicitGroupFromString(input, keywordSeparator);
    }

    private static ExplicitGroup explicitGroupFromString(String input, Character keywordSeparator) throws ParseException {
        assert input.startsWith(MetadataSerializationConfiguration.EXPLICIT_GROUP_ID);

        QuotedStringTokenizer token = new QuotedStringTokenizer(input.substring(MetadataSerializationConfiguration.EXPLICIT_GROUP_ID.length()),
                MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        String name = StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
        try {
            int context = Integer.parseInt(token.nextToken());
            ExplicitGroup newGroup = new ExplicitGroup(name, GroupHierarchyType.getByNumberOrDefault(context), keywordSeparator);
            addGroupDetails(token, newGroup);
            return newGroup;
        } catch (NumberFormatException exception) {
            throw new ParseException("Could not parse context in " + input);
        }
    }

    private static ExplicitGroup legacyExplicitGroupFromString(String input, Character keywordSeparator) throws ParseException {
        assert input.startsWith(MetadataSerializationConfiguration.LEGACY_EXPLICIT_GROUP_ID);

        QuotedStringTokenizer token = new QuotedStringTokenizer(input.substring(MetadataSerializationConfiguration.LEGACY_EXPLICIT_GROUP_ID.length()),
                MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        String name = StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
        try {
            int context = Integer.parseInt(token.nextToken());
            ExplicitGroup newGroup = new ExplicitGroup(name, GroupHierarchyType.getByNumberOrDefault(context), keywordSeparator);
            GroupsParser.addLegacyEntryKeys(token, newGroup);
            return newGroup;
        } catch (NumberFormatException exception) {
            throw new ParseException("Could not parse context in " + input);
        }
    }

    /**
     * Called only when created fromString.
     * JabRef used to store the entries of an explicit group in the serialization, e.g.
     * ExplicitGroup:GroupName\;0\;Key1\;Key2\;;
     * This method exists for backwards compatibility.
     */
    private static void addLegacyEntryKeys(QuotedStringTokenizer tok, ExplicitGroup group) {
        while (tok.hasMoreTokens()) {
            String key = StringUtil.unquote(tok.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
            group.addLegacyEntryKey(key);
        }
    }

    private static AbstractGroup allEntriesGroupFromString(String input) {
        assert input.startsWith(MetadataSerializationConfiguration.ALL_ENTRIES_GROUP_ID);

        return GroupsFactory.createAllEntriesGroup();
    }

    /**
     * Parses s and recreates the SearchGroup from it.
     *
     * @param input The String representation obtained from SearchGroup.toString(), or null if incompatible
     */
    private static AbstractGroup searchGroupFromString(String input) {
        assert input.startsWith(MetadataSerializationConfiguration.SEARCH_GROUP_ID);
        QuotedStringTokenizer token = new QuotedStringTokenizer(input.substring(MetadataSerializationConfiguration.SEARCH_GROUP_ID.length()),
                MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        String name = StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
        int context = Integer.parseInt(token.nextToken());
        String expression = StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
        EnumSet<SearchFlags> searchFlags = EnumSet.noneOf(SearchFlags.class);
        if (Integer.parseInt(token.nextToken()) == 1) {
            searchFlags.add(SearchFlags.CASE_SENSITIVE);
        }
        if (Integer.parseInt(token.nextToken()) == 1) {
            searchFlags.add(SearchFlags.REGULAR_EXPRESSION);
        }
        // version 0 contained 4 additional booleans to specify search
        // fields; these are ignored now, all fields are always searched
        SearchGroup searchGroup = new SearchGroup(name,
                GroupHierarchyType.getByNumberOrDefault(context), expression, searchFlags
        );
        addGroupDetails(token, searchGroup);
        return searchGroup;
    }

    private static void addGroupDetails(QuotedStringTokenizer tokenizer, AbstractGroup group) {
        if (tokenizer.hasMoreTokens()) {
            group.setExpanded(Integer.parseInt(tokenizer.nextToken()) == 1);
            group.setColor(tokenizer.nextToken());
            group.setIconName(tokenizer.nextToken());
            group.setDescription(tokenizer.nextToken());
        }
    }
}
