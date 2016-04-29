/*
 * Copyright (C) 2003-2016 JabRef contributors.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package net.sf.jabref.logic.groups;

import java.util.List;

import net.sf.jabref.importer.fileformat.ParseException;
import net.sf.jabref.logic.l10n.Localization;

/**
 * Converts string representation of groups to a parsed {@link GroupTreeNode}.
 */
public class GroupsParser {

    public static GroupTreeNode importGroups(List<String> orderedData) throws ParseException {
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
            AbstractGroup group = AbstractGroup.fromString(string.substring(spaceIndex + 1));
            GroupTreeNode newNode = new GroupTreeNode(group);
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
