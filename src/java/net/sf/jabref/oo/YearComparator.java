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
 * Comparator for sorting bibliography entries according to publication year. This is used to
 * sort entries in multiple citations where the oldest publication should appear first.
 */
public class YearComparator implements Comparator<BibtexEntry> {

    FieldComparator authComp = new FieldComparator("author"),
        editorComp = new FieldComparator("editor"),
        yearComp = new FieldComparator("year");

    public YearComparator() {

    }

    public int compare(BibtexEntry o1, BibtexEntry o2) {
        // Year as first criterion:
        int comp = yearComp.compare(o1, o2);
        if (comp != 0)
            return comp;
        // TODO: Is it a good idea to try editor if author fields are equal?
        // Author as next criterion:
        comp = authComp.compare(o1, o2);
        if (comp != 0)
            return comp;
        // Editor as next criterion:
        return editorComp.compare(o1, o2);

    }
}
