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
package net.sf.jabref.openoffice.sorting;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.bibtex.comparator.FieldComparator;

import java.util.Comparator;

/**
 * Comparator for sorting bibliography entries according to publication year. This is used to
 * sort entries in multiple citations where the oldest publication should appear first.
 *
 * Sort by ascending: YEAR, AUTHOR, TITLE
 */
public class YearAuthorTitleComparator implements Comparator<BibEntry> {

    private final FieldComparator authComp = new FieldComparator("author");
    private final FieldComparator titleComp = new FieldComparator("title");
    private final FieldComparator yearComp = new FieldComparator("year");

    @Override
    public int compare(BibEntry o1, BibEntry o2) {
        // Year as first criterion:
        int comp = yearComp.compare(o1, o2);
        if (comp != 0) {
            return comp;
        }

        // Author as next criterion:
        comp = authComp.compare(o1, o2);
        if (comp != 0) {
            return comp;
        }

        // Editor as next criterion:
        return titleComp.compare(o1, o2);
    }
}
