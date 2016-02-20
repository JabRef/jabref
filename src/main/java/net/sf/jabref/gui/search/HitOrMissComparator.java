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
package net.sf.jabref.gui.search;

import ca.odell.glazedlists.matchers.Matcher;
import net.sf.jabref.model.entry.BibEntry;

import java.util.Comparator;
import java.util.Objects;

/**
 * This Comparator compares two objects based on whether none, one of them, or both
 * match a given Matcher. It is used to "float" group and search hits in the main table.
 */
public class HitOrMissComparator implements Comparator<BibEntry> {

    private final Matcher<BibEntry> hitOrMiss;

    public HitOrMissComparator(Matcher<BibEntry> hitOrMiss) {
        this.hitOrMiss = Objects.requireNonNull(hitOrMiss);
    }

    @Override
    public int compare(BibEntry o1, BibEntry o2) {
        if (hitOrMiss == null) {
            return 0;
        }

        return Boolean.compare(hitOrMiss.matches(o2), hitOrMiss.matches(o1));
    }
}
