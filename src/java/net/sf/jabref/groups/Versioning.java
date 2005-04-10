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

/**
 * Handles versioning of groups, e.g. automatic conversion from previous to
 * current versions, or import of flat groups (JabRef <= 1.6) to tree.
 * 
 * @author jzieren (10.04.2005)
 */
public class Versioning {
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
            root.add(new GroupTreeNode(new KeywordGroup(name, field, regexp)));
        }
        return root;
    }

}
