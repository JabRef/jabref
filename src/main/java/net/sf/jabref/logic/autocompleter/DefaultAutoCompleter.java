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
package net.sf.jabref.logic.autocompleter;

import net.sf.jabref.model.entry.BibEntry;

import java.util.Objects;
import java.util.StringTokenizer;

/**
 * Delivers possible completions for a given string.
 * Stores all words in the given field which are separated by SEPARATING_CHARS.
 *
 * @author kahlert, cordes
 */
class DefaultAutoCompleter extends AbstractAutoCompleter {

    private final String fieldName;

    private static final String SEPARATING_CHARS = ";,\n ";

    /**
     * @see AutoCompleterFactory
     */
    DefaultAutoCompleter(String fieldName, AutoCompletePreferences preferences) {
        super(preferences);

        this.fieldName = Objects.requireNonNull(fieldName);
    }

    @Override
    public boolean isSingleUnitField() {
        return false;
    }

    /**
     * {@inheritDoc}
     * Stores all words in the given field which are separated by SEPARATING_CHARS.
     */
    @Override
    public void addBibtexEntry(BibEntry entry) {
        if (entry == null) {
            return;
        }

        entry.getFieldOptional(fieldName).ifPresent(fieldValue -> {
            StringTokenizer tok = new StringTokenizer(fieldValue, SEPARATING_CHARS);
            while (tok.hasMoreTokens()) {
                addItemToIndex(tok.nextToken());
            }
        });
    }
}
