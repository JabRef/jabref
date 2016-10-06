package net.sf.jabref.logic.groups;

import java.util.List;

import net.sf.jabref.logic.importer.ParseException;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.QuotedStringTokenizer;
import net.sf.jabref.model.groups.AbstractGroup;
import net.sf.jabref.model.groups.AllEntriesGroup;
import net.sf.jabref.model.groups.ExplicitGroup;
import net.sf.jabref.model.groups.GroupHierarchyType;
import net.sf.jabref.model.groups.GroupTreeNode;
import net.sf.jabref.model.groups.KeywordGroup;
import net.sf.jabref.model.groups.SearchGroup;
import net.sf.jabref.model.strings.StringUtil;

/**
 * Converts string representation of groups to a parsed {@link GroupTreeNode}.
 */
public class GroupsParser {

    public static GroupTreeNode importGroups(List<String> orderedData, Character keywordSeparator)
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
                AbstractGroup group = GroupsParser.fromString(string.substring(spaceIndex + 1), keywordSeparator);
                GroupTreeNode newNode = GroupTreeNode.fromGroup(group);
                if (cursor == null) {
                    // create new root
                    cursor = newNode;
                    root = cursor;
                } else {
                    // insert at desired location
                    while (level <= cursor.getLevel()) {
                        cursor = cursor.getParent().get();
                    }
                    cursor.addChild(newNode);
                    cursor = newNode;
                }
            }
            return root;
        } catch (ParseException e) {
            throw new ParseException(Localization
                    .lang("Group tree could not be parsed. If you save the BibTeX database, all groups will be lost."),
                    e);
        }
    }

    /**
     * Re-create a group instance from a textual representation.
     *
     * @param s The result from the group's toString() method.
     * @return New instance of the encoded group.
     * @throws ParseException If an error occurred and a group could not be created,
     *                        e.g. due to a malformed regular expression.
     */
    public static AbstractGroup fromString(String s, Character keywordSeparator)
            throws ParseException {
        if (s.startsWith(KeywordGroup.ID)) {
            return GroupsParser.keywordGroupFromString(s, keywordSeparator);
        }
        if (s.startsWith(AllEntriesGroup.ID)) {
            return GroupsParser.allEntriesGroupFromString(s);
        }
        if (s.startsWith(SearchGroup.ID)) {
            return GroupsParser.searchGroupFromString(s);
        }
        if (s.startsWith(ExplicitGroup.ID)) {
            return GroupsParser.explicitGroupFromString(s, keywordSeparator);
        }
        return null; // unknown group
    }

    /**
     * Parses s and recreates the KeywordGroup from it.
     *
     * @param s The String representation obtained from
     *          KeywordGroup.toString()
     */
    public static AbstractGroup keywordGroupFromString(String s, Character keywordSeparator) throws ParseException {
        if (!s.startsWith(KeywordGroup.ID)) {
            throw new IllegalArgumentException("KeywordGroup cannot be created from \"" + s + "\".");
        }
        QuotedStringTokenizer tok = new QuotedStringTokenizer(s.substring(KeywordGroup.ID
                .length()), AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR);

        String name = tok.nextToken();
        int context = Integer.parseInt(tok.nextToken());
        String field = tok.nextToken();
        String expression = tok.nextToken();
        boolean caseSensitive = Integer.parseInt(tok.nextToken()) == 1;
        boolean regExp = Integer.parseInt(tok.nextToken()) == 1;
        return new KeywordGroup(StringUtil.unquote(name, AbstractGroup.QUOTE_CHAR),
                StringUtil.unquote(field, AbstractGroup.QUOTE_CHAR),
                StringUtil.unquote(expression, AbstractGroup.QUOTE_CHAR), caseSensitive, regExp,
                GroupHierarchyType.getByNumber(context), keywordSeparator);
    }

    public static ExplicitGroup explicitGroupFromString(String s, Character keywordSeparator) throws ParseException {
        if (!s.startsWith(ExplicitGroup.ID)) {
            throw new IllegalArgumentException("ExplicitGroup cannot be created from \"" + s + "\".");
        }
        QuotedStringTokenizer tok = new QuotedStringTokenizer(s.substring(ExplicitGroup.ID.length()),
                AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR);

        String name = tok.nextToken();
        int context = Integer.parseInt(tok.nextToken());
        ExplicitGroup newGroup = new ExplicitGroup(name, GroupHierarchyType.getByNumber(context), keywordSeparator);
        GroupsParser.addLegacyEntryKeys(tok, newGroup);
        return newGroup;
    }

    /**
     * Called only when created fromString.
     * JabRef used to store the entries of an explicit group in the serialization, e.g.
     *  ExplicitGroup:GroupName\;0\;Key1\;Key2\;;
     * This method exists for backwards compatibility.
     */
    private static void addLegacyEntryKeys(QuotedStringTokenizer tok, ExplicitGroup group) {
        while (tok.hasMoreTokens()) {
            String key = StringUtil.unquote(tok.nextToken(), AbstractGroup.QUOTE_CHAR);
            group.addLegacyEntryKey(key);
        }
    }

    public static AbstractGroup allEntriesGroupFromString(String s) {
        if (!s.startsWith(AllEntriesGroup.ID)) {
            throw new IllegalArgumentException("AllEntriesGroup cannot be created from \"" + s + "\".");
        }
        return new AllEntriesGroup(Localization.lang("All entries"));
    }

    /**
     * Parses s and recreates the SearchGroup from it.
     *
     * @param s The String representation obtained from
     *          SearchGroup.toString(), or null if incompatible
     */
    public static AbstractGroup searchGroupFromString(String s) {
        if (!s.startsWith(SearchGroup.ID)) {
            throw new IllegalArgumentException("SearchGroup cannot be created from \"" + s + "\".");
        }
        QuotedStringTokenizer tok = new QuotedStringTokenizer(s.substring(SearchGroup.ID.length()),
                AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR);

        String name = tok.nextToken();
        int context = Integer.parseInt(tok.nextToken());
        String expression = tok.nextToken();
        boolean caseSensitive = Integer.parseInt(tok.nextToken()) == 1;
        boolean regExp = Integer.parseInt(tok.nextToken()) == 1;
        // version 0 contained 4 additional booleans to specify search
        // fields; these are ignored now, all fields are always searched
        return new SearchGroup(StringUtil.unquote(name, AbstractGroup.QUOTE_CHAR),
                StringUtil.unquote(expression, AbstractGroup.QUOTE_CHAR), caseSensitive, regExp,
                GroupHierarchyType.getByNumber(context));
    }
}
