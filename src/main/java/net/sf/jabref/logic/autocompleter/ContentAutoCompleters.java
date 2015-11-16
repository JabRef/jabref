package net.sf.jabref.logic.autocompleter;

import java.util.Map;
import java.util.Vector;

import net.sf.jabref.logic.journals.Abbreviations;
import net.sf.jabref.model.database.BibtexDatabase;
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
        for (Map.Entry<String, AutoCompleter<String>> entry : this.autoCompleters.entrySet()) {
            AutoCompleter<String> ac = entry.getValue();
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
        AutoCompleter<String> autoCompleter = get("journal");
        if(autoCompleter != null) {
            for(Abbreviation abbreviation : Abbreviations.journalAbbrev.getAbbreviations()) {
                autoCompleter.addWordToIndex(abbreviation.getName());
            }
        }
    }
}
