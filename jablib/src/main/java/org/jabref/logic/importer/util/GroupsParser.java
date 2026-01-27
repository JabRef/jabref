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

public class GroupsParser {

    @Deprecated private static final String SMART_GROUP_ID_FOR_MIGRATION = "SmartGroup:";

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

                string = string.trim();

                if (string.isEmpty()) {
                    continue;
                }

                int spaceIndex = string.indexOf(' ');

                if (spaceIndex <= 0) {
                    throw new ParseException("Expected \"" + string + "\" to contain whitespace");
                }

                int level = Integer.parseInt(string.substring(0, spaceIndex));

                AbstractGroup group = GroupsParser.fromString(
                                                              string.substring(spaceIndex + 1),
                                                              keywordSeparator,
                                                              fileMonitor,
                                                              metaData,
                                                              userAndHost);

                // Skip unknown groups
                if (group == null) {
                    continue;
                }

                GroupTreeNode newNode = GroupTreeNode.fromGroup(group);

                if (cursor == null) {
                    cursor = newNode;
                    root = cursor;
                } else {

                    while ((level <= cursor.getLevel()) && (cursor.getParent().isPresent())) {
                        cursor = cursor.getParent().get();
                    }

                    cursor.addChild(newNode);
                    cursor = newNode;
                }
            }

            return root;

        } catch (ParseException e) {

            throw new ParseException(
                                     Localization.lang("Group tree could not be parsed. If you save the BibTeX library, all groups will be lost."),
                                     e);
        }
    }

    public static AbstractGroup fromString(String input,
                                           Character keywordSeparator,
                                           FileUpdateMonitor fileMonitor,
                                           MetaData metaData,
                                           String userAndHost)
        throws ParseException {

        if (input.startsWith(MetadataSerializationConfiguration.KEYWORD_GROUP_ID)) {
            return keywordGroupFromString(input, keywordSeparator);
        }

        if (input.startsWith(MetadataSerializationConfiguration.ALL_ENTRIES_GROUP_ID)) {
            return allEntriesGroupFromString(input);
        }

        if (input.startsWith(SMART_GROUP_ID_FOR_MIGRATION)) {
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

        // Skip unknown group instead of throwing error
        LOGGER.warn("Skipping unknown group: {}", input);
        return null;
    }

    private static AbstractGroup texGroupFromString(String input,
                                                    FileUpdateMonitor fileMonitor,
                                                    MetaData metaData,
                                                    String userAndHost)
        throws ParseException {

        QuotedStringTokenizer token = new QuotedStringTokenizer(
                                                                input.substring(MetadataSerializationConfiguration.TEX_GROUP_ID.length()),
                                                                MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR,
                                                                MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        String name = StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        GroupHierarchyType context = GroupHierarchyType.getByNumberOrDefault(Integer.parseInt(token.nextToken()));

        try {

            Path path = Path.of(token.nextToken());

            try {

                TexGroup newGroup = TexGroup.create(
                                                    name,
                                                    context,
                                                    path,
                                                    new DefaultAuxParser(new BibDatabase()),
                                                    fileMonitor,
                                                    metaData,
                                                    userAndHost);

                addGroupDetails(token, newGroup);
                return newGroup;

            } catch (IOException ex) {

                LOGGER.warn("Could not access file {}. The group {} will not reflect changes.", path, name);

                TexGroup newGroup = TexGroup.create(
                                                    name,
                                                    context,
                                                    path,
                                                    new DefaultAuxParser(new BibDatabase()),
                                                    metaData,
                                                    userAndHost);

                addGroupDetails(token, newGroup);
                return newGroup;
            }

        } catch (InvalidPathException | IOException ex) {

            throw new ParseException(ex);
        }
    }

    private static AbstractGroup automaticPersonsGroupFromString(String input) {

        QuotedStringTokenizer token = new QuotedStringTokenizer(
                                                                input.substring(MetadataSerializationConfiguration.AUTOMATIC_PERSONS_GROUP_ID.length()),
                                                                MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR,
                                                                MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        String name = StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        GroupHierarchyType context = GroupHierarchyType.getByNumberOrDefault(Integer.parseInt(token.nextToken()));

        Field field = FieldFactory.parseField(
                                              StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));

        AutomaticPersonsGroup newGroup = new AutomaticPersonsGroup(name, context, field);

        addGroupDetails(token, newGroup);

        return newGroup;
    }

    private static AbstractGroup automaticDateGroupFromString(String input) {

        QuotedStringTokenizer token = new QuotedStringTokenizer(
                                                                input.substring(MetadataSerializationConfiguration.AUTOMATIC_DATE_GROUP_ID.length()),
                                                                MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR,
                                                                MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        String name = StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        GroupHierarchyType context = GroupHierarchyType.getByNumberOrDefault(Integer.parseInt(token.nextToken()));

        Field field = FieldFactory.parseField(
                                              StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));

        String granularityString = StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        DateGranularity granularity = DateGranularity.valueOf(granularityString);

        AutomaticDateGroup newGroup = new AutomaticDateGroup(name, context, field, granularity);

        addGroupDetails(token, newGroup);

        return newGroup;
    }

    private static AbstractGroup automaticKeywordGroupFromString(String input) {

        QuotedStringTokenizer token = new QuotedStringTokenizer(
                                                                input.substring(MetadataSerializationConfiguration.AUTOMATIC_KEYWORD_GROUP_ID.length()),
                                                                MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR,
                                                                MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        String name = StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        GroupHierarchyType context = GroupHierarchyType.getByNumberOrDefault(Integer.parseInt(token.nextToken()));

        Field field = FieldFactory.parseField(
                                              StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));

        Character delimiter = StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR).charAt(0);

        Character hierarchicalDelimiter = StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR).charAt(0);

        AutomaticKeywordGroup newGroup = new AutomaticKeywordGroup(name, context, field, delimiter, hierarchicalDelimiter);

        addGroupDetails(token, newGroup);

        return newGroup;
    }

    private static KeywordGroup keywordGroupFromString(String input, Character keywordSeparator) {

        QuotedStringTokenizer token = new QuotedStringTokenizer(
                                                                input.substring(MetadataSerializationConfiguration.KEYWORD_GROUP_ID.length()),
                                                                MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR,
                                                                MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        String name = StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

        GroupHierarchyType context = GroupHierarchyType.getByNumberOrDefault(Integer.parseInt(token.nextToken()));

        Field field = FieldFactory.parseField(
                                              StringUtil.unquote(token.nextToken(), MetadataSerializationConfiguration.GROUP_QUOTE_CHAR));

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

    private static AbstractGroup allEntriesGroupFromString(String input) {
        return GroupsFactory.createAllEntriesGroup();
    }

    private static AbstractGroup searchGroupFromString(String input) {

        QuotedStringTokenizer token = new QuotedStringTokenizer(
                                                                input.substring(MetadataSerializationConfiguration.SEARCH_GROUP_ID.length()),
                                                                MetadataSerializationConfiguration.GROUP_UNIT_SEPARATOR,
                                                                MetadataSerializationConfiguration.GROUP_QUOTE_CHAR);

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

        SearchGroup newGroup = new SearchGroup(name,
                                               GroupHierarchyType.getByNumberOrDefault(context),
                                               expression,
                                               searchFlags);

        addGroupDetails(token, newGroup);

        return newGroup;
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
