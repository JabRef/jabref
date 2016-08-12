/*
 * Copyright (C) 2003-2016 JabRef contributors.
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

import java.util.List;
import java.util.Objects;

import net.sf.jabref.logic.journals.Abbreviation;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.logic.journals.JournalAbbreviationPreferences;

public class JournalAutoCompleter extends EntireFieldAutoCompleter {

    private final JournalAbbreviationLoader abbreviationLoader;
    private final JournalAbbreviationPreferences journalAbbreviationPreferences;


    JournalAutoCompleter(String fieldName, AutoCompletePreferences preferences,
            JournalAbbreviationLoader abbreviationLoader) {
        super(fieldName, preferences);
        this.abbreviationLoader = Objects.requireNonNull(abbreviationLoader);
        this.journalAbbreviationPreferences = preferences.getJournalAbbreviationPreferences();
    }

    @Override
    public List<String> complete(String toComplete) {
        List<String> completions = super.complete(toComplete);

        // Also return journal names in the journal abbreviation list
        for (Abbreviation abbreviation : abbreviationLoader
                .getRepository(journalAbbreviationPreferences).getAbbreviations()) {
            if (abbreviation.getName().startsWith(toComplete)) {
                completions.add(abbreviation.getName());
            }
        }

        return completions;
    }
}
