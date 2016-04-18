/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.logic.id;

import java.util.Comparator;

import net.sf.jabref.model.entry.BibEntry;

/**
 * Comparator for sorting BibEntry objects based on their ID. This
 * can be used to sort entries back into the order they were created,
 * provided the IDs given to entries are lexically monotonically increasing.
 */
public class IdComparator implements Comparator<BibEntry> {

    @Override
    public int compare(BibEntry one, BibEntry two) {
        return one.getId().compareTo(two.getId());
    }
}
