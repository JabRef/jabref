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

/**
 * Delivers auto-completions for BibTeX keys.
 * Only applied for auto-completion inside crossref fields.
 *
 * @author kahlert, cordes
 */
class BibtexKeyAutoCompleter extends AbstractAutoCompleter {
    private static final int SHORTEST_WORD_TO_ADD = 1;

    public BibtexKeyAutoCompleter(AutoCompletePreferences preferences) {
        super(preferences);
    }

    @Override
    public boolean isSingleUnitField() {
        // TODO: Why is this not a single unit field?
        return false;
    }

    /**
     * {@inheritDoc}
     * The bibtex key of the entry will be added to the index.
     */
    @Override
    public void addToIndex(BibEntry entry) {
        if (entry == null) {
            return;
        }

        String key = entry.getCiteKey();
        if (key != null) {
            insertIntoIndex(key.trim());
        }
    }

    @Override
    protected int getLengthOfShortestWordToAdd() {
        return SHORTEST_WORD_TO_ADD;
    }
}
