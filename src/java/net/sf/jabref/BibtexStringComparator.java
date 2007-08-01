/*
Copyright (C) 2003 Nizar N. Batada, Morten O. Alver

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/

package net.sf.jabref;

import java.util.Comparator;

public class BibtexStringComparator implements Comparator<BibtexString> {

    protected boolean considerRefs;
    private static final String MARKER = "__MARKER__";
    private static final String PADDED_MARKER = " "+MARKER+" ";

    /**
     * @param considerRefs Indicates whether the strings should be
     *                     sorted according to internal references in addition to
     *                     alphabetical sorting.
     */
    public BibtexStringComparator(boolean considerRefs) {
        this.considerRefs = considerRefs;
    }

    public int compare(BibtexString s1, BibtexString s2) {

        /*
         If crossreferences are to be considered, the following block sorts by the number of string
         references, so strings with less references precede those with more.
        */
        if (considerRefs) {
            //Pattern refPat = Pattern.compile("#[A-Za-z]+#");
            int ref1 = s1.getContent().replaceAll("#[A-Za-z]+#", PADDED_MARKER).split(MARKER).length,
                ref2 = s2.getContent().replaceAll("#[A-Za-z]+#", PADDED_MARKER).split(MARKER).length;

            if (ref1 != ref2)
                return ref1-ref2;
        }

        int res = 0;

        // First check their names:
        String name1 = s1.getName().toLowerCase(),
                name2 = s2.getName().toLowerCase();

        res = name1.compareTo(name2);

        if (res == 0)
            return res;

        // Then, if we are supposed to, see if the ordering needs
        // to be changed because of one string referring to the other.x
        if (considerRefs) {

            // First order them:
            BibtexString pre, post;
            if (res < 0) {
                pre = s1;
                post = s2;
            } else {
                pre = s2;
                post = s1;
            }

            // Then see if "pre" refers to "post", which is the only
            // situation when we must change the ordering:
            String namePost = post.getName().toLowerCase(),
                    textPre = pre.getContent().toLowerCase();

            // If that is the case, reverse the order found:
            if (textPre.indexOf("#" + namePost + "#") >= 0)
                res = -res;


        }

        return res;
    }

}
