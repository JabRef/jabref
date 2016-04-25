/*  Copyright (C) 2003-2016 JabRef contributors.
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

import java.util.Comparator;

import net.sf.jabref.model.entry.BibEntry;

/**
 * Compares Bibtex entries based on their 'crossref' fields. Entries including
 * this field are deemed smaller than entries without this field. This serves
 * the purpose of always placing referenced entries after referring entries in
 * the .bib file. After this criterion comes comparisons of individual fields.
 */
public class CrossRefEntryComparator implements Comparator<BibEntry> {

    private static final String CROSS_REF_FIELD = "crossref";


    @Override
    public int compare(BibEntry e1, BibEntry e2) {

        Boolean b1 = e1.hasField(CrossRefEntryComparator.CROSS_REF_FIELD);
        Boolean b2 = e2.hasField(CrossRefEntryComparator.CROSS_REF_FIELD);

        if ((!b1) && (!b2)) {
            return 0; // secComparator.compare(e1, e2);
        }
        if (b1 && b2) {
            return 0; // secComparator.compare(e1, e2);
        }
        if (!b1) {
            return 1;
        } else {
            return -1;
        }
    }
}
