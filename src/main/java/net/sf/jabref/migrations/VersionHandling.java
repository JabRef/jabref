/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.migrations;

import java.util.List;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.groups.structure.AbstractGroup;
import net.sf.jabref.groups.structure.AllEntriesGroup;
import net.sf.jabref.groups.structure.GroupHierarchyType;
import net.sf.jabref.groups.structure.KeywordGroup;
import net.sf.jabref.logic.util.strings.StringUtil;

/**
 * Handles versioning of groups, e.g. automatic conversion from previous to
 * current versions, or import of flat groups (JabRef <= 1.6) to tree.
 *
 * @author jzieren (10.04.2005)
 */
public class VersionHandling {

    public static final int CURRENT_VERSION = 3;


    /**
     * Imports old (flat) groups data and converts it to a 2-level tree with an
     * AllEntriesGroup at the root.
     *
     * @return the root of the generated tree.
     */
    public static GroupTreeNode importFlatGroups(List<String> groups)
            throws IllegalArgumentException {
        GroupTreeNode root = new GroupTreeNode(new AllEntriesGroup());
        final int number = groups.size() / 3;
        String name;
        String field;
        String regexp;
        for (int i = 0; i < number; ++i) {
            field = groups.get(3 * i);
            name = groups.get((3 * i) + 1);
            regexp = groups.get((3 * i) + 2);
            root.add(new GroupTreeNode(new KeywordGroup(name, field, regexp,
                    false, true, GroupHierarchyType.INDEPENDENT)));
        }
        return root;
    }

    public static GroupTreeNode importGroups(List<String> orderedData,
            BibDatabase db, int version) throws Exception {
        switch (version) {
        case 0:
        case 1:
            return Version0_1.fromString(orderedData.get(0),
                    db, version);
        case 2:
        case 3:
            return Version2_3.fromString(orderedData, db, version);
        default:
            throw new IllegalArgumentException(
                    "Failed to read groups data (unsupported version: " +
                    Integer.toString(version) + ")");
        }
    }


    /** Imports groups version 0 and 1. */
    private static class Version0_1 {

        /**
         * Parses the textual representation obtained from
         * GroupTreeNode.toString() and recreates that node and all of its
         * children from it.
         *
         * @throws Exception
         *             When a group could not be recreated
         */
        private static GroupTreeNode fromString(String str, BibDatabase db,
                int version) throws Exception {
            GroupTreeNode root = null;
            GroupTreeNode newNode;
            int i;
            String g;
            String s = str;
            while (!s.isEmpty()) {
                if (s.startsWith("(")) {
                    String subtree = Version0_1.getSubtree(s);
                    newNode = Version0_1.fromString(subtree, db, version);
                    // continue after this subtree by removing it
                    // and the leading/trailing braces, and
                    // the comma (that makes 3) that always trails it
                    // unless it's at the end of s anyway.
                    i = 3 + subtree.length();
                    s = i >= s.length() ? "" : s.substring(i);
                } else {
                    i = Version0_1.indexOfUnquoted(s, ',');
                    g = i < 0 ? s : s.substring(0, i);
                    if (i >= 0) {
                        s = s.substring(i + 1);
                    } else {
                        s = "";
                    }
                    newNode = new GroupTreeNode(AbstractGroup.fromString(StringUtil
                            .unquote(g, '\\'), db, version));
                }
                if (root == null) {
                    root = newNode;
                } else {
                    root.add(newNode);
                }
            }
            return root;
        }

        /**
         * Returns the substring delimited by a pair of matching braces, with
         * the first brace at index 0. Quoted characters are skipped.
         *
         * @return the matching substring, or "" if not found.
         */
        private static String getSubtree(String s) {
            int i = 1;
            int level = 1;
            while (i < s.length()) {
                switch (s.charAt(i)) {
                case '\\':
                    ++i;
                    break;
                case '(':
                    ++level;
                    break;
                case ')':
                    --level;
                    if (level == 0) {
                        return s.substring(1, i);
                    }
                    break;
                default:
                    break;
                }
                ++i;
            }
            return "";
        }

        /**
         * Returns the index of the first occurrence of c, skipping quoted
         * special characters (escape character: '\\').
         *
         * @param s
         *            The String to search in.
         * @param c
         *            The character to search
         * @return The index of the first unescaped occurrence of c in s, or -1
         *         if not found.
         */
        private static int indexOfUnquoted(String s, char c) {
            int i = 0;
            while (i < s.length()) {
                if (s.charAt(i) == '\\') {
                    ++i; // skip quoted special
                } else {
                    if (s.charAt(i) == c) {
                        return i;
                    }
                }
                ++i;
            }
            return -1;
        }
    }

    private static class Version2_3 {

        private static GroupTreeNode fromString(List<String> data, BibDatabase db,
                int version) throws Exception {
            GroupTreeNode cursor = null;
            GroupTreeNode root = null;
            GroupTreeNode newNode;
            AbstractGroup group;
            int spaceIndex;
            int level;
            String s;
            for (int i = 0; i < data.size(); ++i) {
                s = data.get(i);

                // This allows to read databases that have been modified by, e.g., BibDesk
                s = s.trim();
                if (s.isEmpty()) {
                    continue;
                }

                spaceIndex = s.indexOf(' ');
                if (spaceIndex <= 0) {
                    throw new Exception("bad format");
                }
                level = Integer.parseInt(s.substring(0, spaceIndex));
                group = AbstractGroup.fromString(s.substring(spaceIndex + 1),
                        db, version);
                newNode = new GroupTreeNode(group);
                if (cursor == null) {
                    // create new root
                    cursor = newNode;
                    root = cursor;
                } else {
                    // insert at desired location
                    while (level <= cursor.getLevel()) {
                        cursor = (GroupTreeNode) cursor.getParent();
                    }
                    cursor.add(newNode);
                    cursor = newNode;
                }
            }
            return root;
        }
    }
}
