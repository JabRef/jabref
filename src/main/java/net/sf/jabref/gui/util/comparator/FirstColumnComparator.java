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
package net.sf.jabref.gui.util.comparator;

import java.util.Comparator;
import java.util.Optional;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.logic.TypedBibEntry;
import net.sf.jabref.model.entry.BibEntry;

public class FirstColumnComparator implements Comparator<BibEntry> {

    private final BibDatabaseContext database;

    public FirstColumnComparator(BibDatabaseContext database) {
        this.database = database;
    }

    @Override
    public int compare(BibEntry e1, BibEntry e2) {
        int score1 = 0;
        int score2 = 0;

        TypedBibEntry typedEntry1 = new TypedBibEntry(e1, Optional.of(database.getDatabase()), database.getMode());
        TypedBibEntry typedEntry2 = new TypedBibEntry(e2, Optional.of(database.getDatabase()), database.getMode());
        if (typedEntry1.hasAllRequiredFields()) {
            score1++;
        }

        if (typedEntry2.hasAllRequiredFields()) {
            score2++;
        }

        return score1 - score2;
    }

}
