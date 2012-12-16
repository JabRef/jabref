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
package net.sf.jabref.oo;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.FieldComparator;

import java.util.Comparator;

/**
 * Comparator for sorting bibliography entries.
 *
 * TODO: is it sufficient with a hardcoded sort algorithm for the bibliography?
 */
public class AlphanumericComparator implements Comparator<BibtexEntry> {

    FieldComparator authComp = new FieldComparator("author"),
        editorComp = new FieldComparator("editor"),
        yearComp = new FieldComparator("year"),
        titleComp = new FieldComparator("title");

    public AlphanumericComparator() {

    }

    public int compare(BibtexEntry o1, BibtexEntry o2) {
        // Author as first criterion:
        int comp = authComp.compare(o1, o2);
        if (comp != 0)
            return comp;
        // Editor as second criterion:
        comp = editorComp.compare(o1, o2);
        if (comp != 0)
            return comp;
        // Year as next criterion:
        comp = yearComp.compare(o1, o2);
        if (comp != 0)
            return comp;
        // Title as next criterion:
        comp = titleComp.compare(o1, o2);
        if (comp != 0)
            return comp;
        // Bibtex key as next criterion:
        return compare(o1.getCiteKey(), o2.getCiteKey());


    }

    private int compare(String k1, String k2) {
        if (k1 != null) {
            if (k2 != null)
                return k1.compareTo(k2);
            else
                return 1;
        }
        else if (k2 != null)
            return -1;
        else return 0;
    }
}
