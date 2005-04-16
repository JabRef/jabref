/*
 All programs in this directory and subdirectories are published under the 
 GNU General Public License as described below.

 This program is free software; you can redistribute it and/or modify it 
 under the terms of the GNU General Public License as published by the Free 
 Software Foundation; either version 2 of the License, or (at your option) 
 any later version.

 This program is distributed in the hope that it will be useful, but WITHOUT 
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for 
 more details.

 You should have received a copy of the GNU General Public License along 
 with this program; if not, write to the Free Software Foundation, Inc., 59 
 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html
 */

package net.sf.jabref.groups;

import java.util.Vector;

import net.sf.jabref.*;
import net.sf.jabref.BibtexDatabase;

/**
 * Handles versioning of groups, e.g. automatic conversion from previous to
 * current versions, or import of flat groups (JabRef <= 1.6) to tree.
 * 
 * @author jzieren (10.04.2005)
 */
public class VersionHandling {
    public static final int CURRENT_VERSION = 1;

    /**
     * Imports old (flat) groups data and converts it to a 2-level tree with an
     * AllEntriesGroup at the root.
     * 
     * @return the root of the generated tree.
     */
    public static GroupTreeNode importFlatGroups(Vector groups)
            throws IllegalArgumentException {
        GroupTreeNode root = new GroupTreeNode(new AllEntriesGroup());
        final int number = groups.size() / 3;
        String name, field, regexp;
        for (int i = 0; i < number; ++i) {
            field = (String) groups.elementAt(3 * i + 0);
            name = (String) groups.elementAt(3 * i + 1);
            regexp = (String) groups.elementAt(3 * i + 2);
            root.add(new GroupTreeNode(new KeywordGroup(name, field, regexp,
                    false, true)));
        }
        return root;
    }

    public static GroupTreeNode importGroups(Vector orderedData,
            BibtexDatabase db, int version) throws Exception {
        switch (version) {
        case 0:
        case 1:
            return Version0_1.fromString((String) orderedData.firstElement(),
                    db, version);
        default: // JZTODO translation
            throw new IllegalArgumentException(
                    "Failed to read groups data (version: " + version
                            + " is not supported)");
        }
    }

    /** Imports groups version 0. */
    private static class Version0_1 {
        /**
         * Parses the textual representation obtained from
         * GroupTreeNode.toString() and recreates that node and all of its
         * children from it.
         * 
         * @throws Exception
         *             When a group could not be recreated
         */
        private static GroupTreeNode fromString(String s, BibtexDatabase db,
                int version) throws Exception {
            GroupTreeNode root = null;
            GroupTreeNode newNode;
            int i;
            String g;
            while (s.length() > 0) {
                if (s.startsWith("(")) {
                    String subtree = getSubtree(s);
                    newNode = fromString(subtree, db, version);
                    // continue after this subtree by removing it
                    // and the leading/trailing braces, and
                    // the comma (that makes 3) that always trails it
                    // unless it's at the end of s anyway.
                    i = 3 + subtree.length();
                    s = i >= s.length() ? "" : s.substring(i);
                } else {
                    i = indexOfUnquoted(s, ',');
                    g = i < 0 ? s : s.substring(0, i);
                    if (i >= 0)
                        s = s.substring(i + 1);
                    else
                        s = "";
                    newNode = new GroupTreeNode(AbstractGroup.fromString(Util
                            .unquote(g, '\\'), db, version));
                }
                if (root == null) // first node will be root
                    root = newNode;
                else
                    root.add(newNode);
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
                    if (level == 0)
                        return s.substring(1, i);
                    break;
                }
                ++i;
            }
            return "";
        }

        /**
         * Returns the index of the first occurence of c, skipping quoted
         * special characters (escape character: '\\').
         * 
         * @param s
         *            The String to search in.
         * @param c
         *            The character to search
         * @return The index of the first unescaped occurence of c in s, or -1
         *         if not found.
         */
        private static int indexOfUnquoted(String s, char c) {
            int i = 0;
            while (i < s.length()) {
                if (s.charAt(i) == '\\') {
                    ++i; // skip quoted special
                } else {
                    if (s.charAt(i) == c)
                        return i;
                }
                ++i;
            }
            return -1;
        }

    }
}
