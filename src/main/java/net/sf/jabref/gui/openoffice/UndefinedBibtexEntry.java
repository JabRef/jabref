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
package net.sf.jabref.gui.openoffice;

import net.sf.jabref.logic.openoffice.OOBibStyle;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.IdGenerator;

/**
 * Subclass of BibEntry for representing entries referenced in a document that can't
 * be found in JabRef's current database.
 */
class UndefinedBibtexEntry extends BibEntry {

    private final String key;


    public UndefinedBibtexEntry(String key) {
        super(IdGenerator.next());
        this.key = key;
        setField("author", OOBibStyle.UNDEFINED_CITATION_MARKER);
    }

    public String getKey() {
        return key;
    }
}
