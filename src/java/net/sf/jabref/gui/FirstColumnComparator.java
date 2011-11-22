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
package net.sf.jabref.gui;

import java.util.Comparator;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexDatabase;

public class FirstColumnComparator implements Comparator<BibtexEntry> {
    private BibtexDatabase database;

    public FirstColumnComparator(BibtexDatabase database) {

        this.database = database;
    }

    public int compare(BibtexEntry e1, BibtexEntry e2) {

		int score1 = 0, score2 = 0;

		if (e1.hasAllRequiredFields(database))
			score1++;

		if (e2.hasAllRequiredFields(database))
			score2++;

		return score1 - score2;
	}

}
