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
package net.sf.jabref;

import java.util.Comparator;

/**
 * Compares Bibtex entries based on their 'crossref' fields. Entries including
 * this field are deemed smaller than entries without this field. This serves
 * the purpose of always placing referenced entries after referring entries in
 * the .bib file. After this criterion comes comparisons of individual fields.
 */
public class CrossRefEntryComparator implements Comparator<BibtexEntry> {

	private String crossRefField = "crossref";

	public int compare(BibtexEntry e1, BibtexEntry e2)
		throws ClassCastException {

		Object f1 = e1.getField(crossRefField), f2 = e2.getField(crossRefField);

		if ((f1 == null) && (f2 == null))
			return 0; // secComparator.compare(e1, e2);
		if ((f1 != null) && (f2 != null))
			return 0; // secComparator.compare(e1, e2);
		if (f1 != null)
			return -1;
		else
			return 1;
	}

}
