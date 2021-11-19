package org.jabref.logic.importer.util;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.logic.groups.DefaultGroupsFactory;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.MetadataSerializationConfiguration;
import org.jabref.logic.util.strings.QuotedStringTokenizer;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AutomaticKeywordGroup;
import org.jabref.model.groups.AutomaticPersonsGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.KeywordGroup;
import org.jabref.model.groups.RegexKeywordGroup;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.groups.TexGroup;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.model.search.rules.SearchRules.SearchFlags;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.LoggerFactory;

/**
 * Converts string representation of groups to a parsed {@link GroupTreeNode}.
 */
public class GroupsParser {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GroupsParser.class);

    private GroupsParser() {
    }

    public static GroupTreeNode importGroups(List<String> orderedData, Character keywordSeparator, FileUpdateMonitor fileMonitor, MetaData metaData)
            throws ParseException {
        try {
            GroupTreeNode cursor = null;
            GroupTreeNode root = null;
            for (String string : orderedData) {
                // This allows to read databases that have been modified by, e.g., BibDesk
                string = string.trim();
                if (string.isEmpty()) {
                    continue;
                }

                int spaceIndex = string.indexOf(' ');
                if (spaceIndex <= 0) {
                    throw new ParseException("Expected \"" + string + "\" to contain whitespace");
                }
                int level = Integer.parseInt(string.substring(0, spaceIndex));
                AbstractGroup group = GroupsParser.fromString(string.substring(spaceIndex + 1), keywordSeparator, fileMonitor, metaData);
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
     * @param s The result from the group's toString() method.
     * @return New instance of the encoded group.
     * @throws ParseException If an error occurred and a group could not be created, e.g. due to a malformed regular expression.
     */
    public static AbstractGroup fromString(String s, Character keywordSeparator, FileUpdateMonitor fileMonitor, MetaData metaData)
            throws ParseException {
        if (s.startsWith(MetadataSerializationConfiguration.KEYWORD_GROUP_ID)) {
            return keywordGroupFromString(s, keywordSeparator);
        }
        if (s.startsWith(MetadataSerializationConfiguration.ALL_ENTRIES_GROUP_ID)) {
            return allEntriesGroupFromString(s);
        }
        if (s.startsWith(MetadataSerializationConfiguration.SEARCH_GROUP_ID)) {
            return searchGroupFromString(s);
        }
        if (s.startsWith(MetadataSerializationConfiguration.EXPLICIT_GROUP_ID)) {
            return explicitGroupFromString(s, keywordSeparator);
        }
        if (s.startsWith(MetadataSerializationConfiguration.LEGACY_EXPLICIT_GROUP_ID)) {
            return legacyExplicitGroupFromString(s, keywordSeparator);
        }
        if (s.startsWith(MetadataSerializationConfiguration.AUTOMATIC_PERSONS_GROUP_ID)) {
            return automaticPersonsGroupFromString(s);
        }
        if (s.startsWith(MetadataSerializationConfiguration.AUTOMATIC_KEYWORD_GROUP_ID)) {
            return automaticKeywordGroupFromString(s);
        }
        if (s.startsWith(MetadataSerializationConfiguration.TEX_GROUP_ID)) {
            return texGroupFromString(s, fileMonitor, metaData);
        }

        throw new ParseException("Unknown group: " + s);
    }

    private static AbstractGroup texGroupFromString(String string, FileUpdateMonitor fileMonitor, MetaData metaData) throws ParseException {
        QuotedStringTokenizer tok = new QuotedStringTokenizer(string.substring(MetadataSerializationConfiguration.TEX_GROUP_ID
                .length()), MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        String name = StringUtil.unquote(tok.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
        GroupHierarchyType context = GroupHierarchyType.getByNumberOrDefault(Integer.parseInt(tok.nextToken()));
        try {
            Path path = Path.of(tok.nextToken());
            try {
                TexGroup newGroup = TexGroup.create(name, context, path, new DefaultAuxParser(new BibDatabase()), fileMonitor, metaData);
                addGroupDetails(tok, newGroup);
                return newGroup;
            } catch (IOException ex) {
                // Problem accessing file -> create without file monitoring
                LOGGER.warn("Could not access file {}. The group {} will not reflect changes to the aux file.", path, name, ex);

                TexGroup newGroup = TexGroup.createWithoutFileMonitoring(name, context, path, new DefaultAuxParser(new BibDatabase()), fileMonitor, metaData);
                addGroupDetails(tok, newGroup);
                return newGroup;
            }
        } catch (InvalidPathException | IOException ex) {
            throw new ParseException(ex);
        }
    }

    private static AbstractGroup automaticPersonsGroupFromString(String string) {
        if (!string.startsWith(MetadataSerializationConfiguration.AUTOMATIC_PERSONS_GROUP_ID)) {
            throw new IllegalArgumentException("KeywordGroup cannot be created from \"" + string + "\".");
        }
        QuotedStringTokenizer tok = new QuotedStringTokenizer(string.substring(MetadataSerializationConfiguration.AUTOMATIC_PERSONS_GROUP_ID
                .length()), MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        String name = StringUtil.unquote(tok.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
        GroupHierarchyType context = GroupHierarchyType.getByNumberOrDefault(Integer.parseInt(tok.nextToken()));
        Field field = FieldFactory.parseField(StringUtil.unquote(tok.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));
        AutomaticPersonsGroup newGroup = new AutomaticPersonsGroup(name, context, field);
        addGroupDetails(tok, newGroup);
        return newGroup;
    }

    private static AbstractGroup automaticKeywordGroupFromString(String string) {
        if (!string.startsWith(MetadataSerializationConfiguration.AUTOMATIC_KEYWORD_GROUP_ID)) {
            throw new IllegalArgumentException("KeywordGroup cannot be created from \"" + string + "\".");
        }
        QuotedStringTokenizer tok = new QuotedStringTokenizer(string.substring(MetadataSerializationConfiguration.AUTOMATIC_KEYWORD_GROUP_ID
                .length()), MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        String name = StringUtil.unquote(tok.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
        GroupHierarchyType context = GroupHierarchyType.getByNumberOrDefault(Integer.parseInt(tok.nextToken()));
        Field field = FieldFactory.parseField(StringUtil.unquote(tok.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));
        Character delimiter = StringUtil.unquote(tok.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR).charAt(0);
        Character hierarchicalDelimiter = StringUtil.unquote(tok.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR).charAt(0);
        AutomaticKeywordGroup newGroup = new AutomaticKeywordGroup(name, context, field, delimiter, hierarchicalDelimiter);
        addGroupDetails(tok, newGroup);
        return newGroup;
    }

    /**
     * Parses s and recreates the KeywordGroup from it.
     *
     * @param s The String representation obtained from KeywordGroup.toString()
     */
    private static KeywordGroup keywordGroupFromString(String s, Character keywordSeparator) throws ParseException {
        if (!s.startsWith(MetadataSerializationConfiguration.KEYWORD_GROUP_ID)) {
            throw new IllegalArgumentException("KeywordGroup cannot be created from \"" + s + "\".");
        }
        QuotedStringTokenizer tok = new QuotedStringTokenizer(s.substring(MetadataSerializationConfiguration.KEYWORD_GROUP_ID
                .length()), MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        String name = StringUtil.unquote(tok.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
        GroupHierarchyType context = GroupHierarchyType.getByNumberOrDefault(Integer.parseInt(tok.nextToken()));
        Field field = FieldFactory.parseField(StringUtil.unquote(tok.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));
        String expression = StringUtil.unquote(tok.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
        boolean caseSensitive = Integer.parseInt(tok.nextToken()) == 1;
        boolean regExp = Integer.parseInt(tok.nextToken()) == 1;
        KeywordGroup newGroup;
        if (regExp) {
            newGroup = new RegexKeywordGroup(name, context, field, expression, caseSensitive);
        } else {
            newGroup = new WordKeywordGroup(name, context, field, expression, caseSensitive, keywordSeparator, false);
        }
        addGroupDetails(tok, newGroup);
        return newGroup;
    }

    private static ExplicitGroup explicitGroupFromString(String input, Character keywordSeparator) throws ParseException {
        if (!input.startsWith(MetadataSerializationConfiguration.EXPLICIT_GROUP_ID)) {
            throw new IllegalArgumentException("ExplicitGroup cannot be created from \"" + input + "\".");
        }
        QuotedStringTokenizer tok = new QuotedStringTokenizer(input.substring(MetadataSerializationConfiguration.EXPLICIT_GROUP_ID.length()),
                MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        String name = StringUtil.unquote(tok.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
        try {
            int context = Integer.parseInt(tok.nextToken());
            ExplicitGroup newGroup = new ExplicitGroup(name, GroupHierarchyType.getByNumberOrDefault(context), keywordSeparator);
            addGroupDetails(tok, newGroup);
            return newGroup;
        } catch (NumberFormatException exception) {
            throw new ParseException("Could not parse context in " + input);
        }
    }

    private static ExplicitGroup legacyExplicitGroupFromString(String input, Character keywordSeparator) throws ParseException {
        if (!input.startsWith(MetadataSerializationConfiguration.LEGACY_EXPLICIT_GROUP_ID)) {
            throw new IllegalArgumentException("ExplicitGroup cannot be created from \"" + input + "\".");
        }
        QuotedStringTokenizer tok = new QuotedStringTokenizer(input.substring(MetadataSerializationConfiguration.LEGACY_EXPLICIT_GROUP_ID.length()),
                MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        String name = StringUtil.unquote(tok.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
        try {
            int context = Integer.parseInt(tok.nextToken());
            ExplicitGroup newGroup = new ExplicitGroup(name, GroupHierarchyType.getByNumberOrDefault(context), keywordSeparator);
            GroupsParser.addLegacyEntryKeys(tok, newGroup);
            return newGroup;
        } catch (NumberFormatException exception) {
            throw new ParseException("Could not parse context in " + input);
        }
    }

    /**
     * Called only when created fromString.
     * JabRef used to store the entries of an explicit group in the serialization, e.g.
     *  ExplicitGroup:GroupName\;0\;Key1\;Key2\;;
     * This method exists for backwards compatibility.
     */
    private static void addLegacyEntryKeys(QuotedStringTokenizer tok, ExplicitGroup group) {
        while (tok.hasMoreTokens()) {
            String key = StringUtil.unquote(tok.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
            group.addLegacyEntryKey(key);
        }
    }

    private static AbstractGroup allEntriesGroupFromString(String s) {
        if (!s.startsWith(MetadataSerializationConfiguration.ALL_ENTRIES_GROUP_ID)) {
            throw new IllegalArgumentException("AllEntriesGroup cannot be created from \"" + s + "\".");
        }
        return DefaultGroupsFactory.getAllEntriesGroup();
    }

    /**
     * Parses s and recreates the SearchGroup from it.
     *
     * @param s The String representation obtained from SearchGroup.toString(), or null if incompatible
     */
    private static AbstractGroup searchGroupFromString(String s) {
        if (!s.startsWith(MetadataSerializationConfiguration.SEARCH_GROUP_ID)) {
            throw new IllegalArgumentException("SearchGroup cannot be created from \"" + s + "\".");
        }
        QuotedStringTokenizer tok = new QuotedStringTokenizer(s.substring(MetadataSerializationConfiguration.SEARCH_GROUP_ID.length()),
                MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR, MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        String name = StringUtil.unquote(tok.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
        int context = Integer.parseInt(tok.nextToken());
        String expression = StringUtil.unquote(tok.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);
        EnumSet<SearchFlags> searchFlags = EnumSet.noneOf(SearchFlags.class);
        if (Integer.parseInt(tok.nextToken()) == 1) {
            searchFlags.add(SearchRules.SearchFlags.CASE_SENSITIVE);
        }
        if (Integer.parseInt(tok.nextToken()) == 1) {
            searchFlags.add(SearchRules.SearchFlags.REGULAR_EXPRESSION);
        }
        // version 0 contained 4 additional booleans to specify search
        // fields; these are ignored now, all fields are always searched
        SearchGroup searchGroup = new SearchGroup(name,
                GroupHierarchyType.getByNumberOrDefault(context), expression, searchFlags
        );
        addGroupDetails(tok, searchGroup);
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
