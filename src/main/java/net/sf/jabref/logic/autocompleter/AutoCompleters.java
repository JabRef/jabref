/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.logic.autocompleter;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;

import java.util.HashMap;

class AutoCompleters {

    final HashMap<String, AutoCompleter> autoCompleters = new HashMap<>();
    // Hashtable that holds as keys the names of the fields where
    // autocomplete is active, and references to the autocompleter objects.

    public AutoCompleter get(String fieldName) {
        return autoCompleters.get(fieldName);
    }

    void addDatabase(BibtexDatabase database) {
        for (BibtexEntry entry : database.getEntries()) {
            addEntry(entry);
        }
    }

    /**
     * This methods assures all words in the given entry are recorded in their
     * respective Completers, if any.
     */
    public void addEntry(BibtexEntry bibtexEntry) {
        for (AutoCompleter autoCompleter : autoCompleters.values()) {
            autoCompleter.addBibtexEntry(bibtexEntry);
        }
    }

    void put(String field, AutoCompleter autoCompleter) {
        autoCompleters.put(field, autoCompleter);
    }

}
