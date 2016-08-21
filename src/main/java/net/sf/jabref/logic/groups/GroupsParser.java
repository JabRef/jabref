package net.sf.jabref.logic.groups;

import java.util.List;

import net.sf.jabref.logic.importer.util.ParseException;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.preferences.JabRefPreferences;

/**
 * Converts string representation of groups to a parsed {@link GroupTreeNode}.
 */
class GroupsParser {

    public static GroupTreeNode importGroups(List<String> orderedData, JabRefPreferences jabRefPreferences)
            throws ParseException {
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
                throw new ParseException(Localization.lang("Expected \"%0\" to contain whitespace", string));
            }
            int level = Integer.parseInt(string.substring(0, spaceIndex));
            AbstractGroup group = AbstractGroup.fromString(string.substring(spaceIndex + 1), jabRefPreferences);
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
    }
}
