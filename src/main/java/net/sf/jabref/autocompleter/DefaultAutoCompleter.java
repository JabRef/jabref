/*  Copyright (C) 2003-2012 JabRef contributors.
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
package net.sf.jabref.autocompleter;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;

import java.util.StringTokenizer;

/**
 * Stores all words which are separated by Globals.SEPARATING_CHARS. This
 * autocompleter only processes the field which is given by the fieldname.
 *
 * @author kahlert, cordes
 */
class DefaultAutoCompleter extends AbstractAutoCompleter {

    private final String fieldName;

    /**
     * @see AutoCompleterFactory
     */
    DefaultAutoCompleter(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public boolean isSingleUnitField() {
        return false;
    }

    @Override
    public void addBibtexEntry(BibtexEntry entry) {
        if (entry == null) {
            return;
        }

        String fieldValue = entry.getField(fieldName);
        if (fieldValue != null) {
            StringTokenizer tok = new StringTokenizer(fieldValue, Globals.SEPARATING_CHARS);
            while (tok.hasMoreTokens()) {
                String word = tok.nextToken();
                addWordToIndex(word);
            }
        }
    }
}
