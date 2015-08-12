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

import java.util.Map;
import java.util.Vector;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.logic.journals.Abbreviation;

public class ContentAutoCompleters extends AutoCompleters {

    public ContentAutoCompleters(BibtexDatabase database, MetaData metaData) {
        String[] completeFields = Globals.prefs.getStringArray(JabRefPreferences.AUTO_COMPLETE_FIELDS);
        for (String field : completeFields) {
            AutoCompleter autoCompleter = AutoCompleterFactory.getFor(field);
            put(field, autoCompleter);
        }

        addDatabase(database);

        addJournalListToAutoCompleter();
        addContentSelectorValuesToAutoCompleters(metaData);
    }

    /**
     * For all fields with both autocompletion and content selector, add content selector
     * values to the autocompleter list:
     */
    public void addContentSelectorValuesToAutoCompleters(MetaData metaData) {
        for (Map.Entry<String, AutoCompleter> entry : this.autoCompleters.entrySet()) {
            AutoCompleter ac = entry.getValue();
            if (metaData.getData(Globals.SELECTOR_META_PREFIX + entry.getKey()) != null) {
                Vector<String> items = metaData.getData(Globals.SELECTOR_META_PREFIX + entry.getKey());
                if (items != null) {
                    for (String item : items) {
                        ac.addWordToIndex(item);
                    }
                }
            }
        }
    }

    /**
     * If an autocompleter exists for the "journal" field, add all
     * journal names in the journal abbreviation list to this autocompleter.
     */
    public void addJournalListToAutoCompleter() {
        AutoCompleter autoCompleter = get("journal");
        if(autoCompleter != null) {
            for(Abbreviation abbreviation : Globals.journalAbbrev.getAbbreviations()) {
                autoCompleter.addWordToIndex(abbreviation.getName());
            }
        }
    }

}
