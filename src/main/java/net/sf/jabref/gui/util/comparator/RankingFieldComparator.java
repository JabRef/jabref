/*  Copyright (C) 2012 JabRef contributors.
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
package net.sf.jabref.gui.util.comparator;

import java.util.Comparator;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.specialfields.SpecialFieldsUtils;

/**
 * Comparator that handles the ranking icon column
 *
 * Based on IconComparator
 * Only comparing ranking field
 * inverse comparison of ranking as rank5 is higher than rank1
 */
public class RankingFieldComparator implements Comparator<BibEntry> {

    @Override
    public int compare(BibEntry e1, BibEntry e2) {
        String val1 = e1.getField(SpecialFieldsUtils.FIELDNAME_RANKING);
        String val2 = e2.getField(SpecialFieldsUtils.FIELDNAME_RANKING);
        if (val1 == null) {
            if (val2 == null) {
                return 0;
            } else {
                return 1;
            }
        } else {
            if (val2 == null) {
                return -1;
            } else {
                // val1 is not null AND val2 is not null
                int compareToRes = val1.compareTo(val2);
                if (compareToRes == 0) {
                    return 0;
                } else {
                    return compareToRes * -1;
                }
            }
        }
    }

}
