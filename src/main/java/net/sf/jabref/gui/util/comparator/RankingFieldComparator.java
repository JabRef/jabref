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
import java.util.Optional;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.SpecialFields;

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
        Optional<String> val1 = e1.getFieldOptional(SpecialFields.FIELDNAME_RANKING);
        Optional<String> val2 = e2.getFieldOptional(SpecialFields.FIELDNAME_RANKING);
        if (val1.isPresent()) {
            if (val2.isPresent()) {
                // val1 is not null AND val2 is not null
                int compareToRes = val1.get().compareTo(val2.get());
                if (compareToRes == 0) {
                    return 0;
                } else {
                    return compareToRes * -1;
                }
            } else {
                return -1;
            }
        } else {
            if (val2.isPresent()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

}
