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
package net.sf.jabref.bibtex.comparator;

import net.sf.jabref.model.entry.BibtexString;

import java.util.Comparator;

public class BibtexStringComparator implements Comparator<BibtexString> {

    private final boolean considerRefs;


    /**
     * @param considerRefs Indicates whether the strings should be
     *                     sorted according to internal references in addition to
     *                     alphabetical sorting.
     */
    public BibtexStringComparator(boolean considerRefs) {
        this.considerRefs = considerRefs;
    }

    @Override
    public int compare(BibtexString s1, BibtexString s2) {

        int res;

        // First check their names:
        String name1 = s1.getName().toLowerCase();
        String name2 = s2.getName().toLowerCase();

        res = name1.compareTo(name2);

        if (res == 0) {
            return res;
        }

        // Then, if we are supposed to, see if the ordering needs
        // to be changed because of one string referring to the other.x
        if (considerRefs) {

            // First order them:
            BibtexString pre;
            BibtexString post;
            if (res < 0) {
                pre = s1;
                post = s2;
            } else {
                pre = s2;
                post = s1;
            }

            // Then see if "pre" refers to "post", which is the only
            // situation when we must change the ordering:
            String namePost = post.getName().toLowerCase();
            String textPre = pre.getContent().toLowerCase();

            // If that is the case, reverse the order found:
            if (textPre.contains("#" + namePost + "#")) {
                res = -res;
            }

        }

        return res;
    }

}
