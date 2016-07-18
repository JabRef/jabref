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

import java.util.Arrays;
import java.util.Objects;

import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.logic.journals.JournalAbbreviationPreferences;
import net.sf.jabref.model.entry.FieldProperties;
import net.sf.jabref.model.entry.InternalBibtexFields;

/**
 * Returns an autocompleter to a given fieldname.
 *
 * @author kahlert, cordes
 */
public class AutoCompleterFactory {

    private final AutoCompletePreferences preferences;
    private final JournalAbbreviationLoader abbreviationLoader;


    public AutoCompleterFactory(AutoCompletePreferences preferences, JournalAbbreviationLoader abbreviationLoader) {
        this.preferences = Objects.requireNonNull(preferences);
        this.abbreviationLoader = Objects.requireNonNull(abbreviationLoader);
    }

    public AutoCompleter<String> getFor(String fieldName) {
        Objects.requireNonNull(fieldName);

        if (InternalBibtexFields.getFieldExtras(fieldName).contains(FieldProperties.PERSON_NAMES)) {
            return new NameFieldAutoCompleter(fieldName, preferences);
        } else if ("crossref".equals(fieldName)) {
            return new BibtexKeyAutoCompleter(preferences);
        } else if ("journal".equals(fieldName) || "publisher".equals(fieldName)) {
            return new JournalAutoCompleter(fieldName, preferences, abbreviationLoader);
        } else {
            return new DefaultAutoCompleter(fieldName, preferences);
        }
    }

    public AutoCompleter<String> getPersonAutoCompleter() {
        return new NameFieldAutoCompleter(Arrays.asList("author", "editor"), true, preferences);
    }

}
