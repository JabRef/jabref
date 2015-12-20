package net.sf.jabref.logic.autocompleter;

import java.util.Map;
import java.util.Objects;
import java.util.Vector;

import net.sf.jabref.logic.journals.Abbreviations;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.Globals;
import net.sf.jabref.MetaData;
import net.sf.jabref.logic.journals.Abbreviation;

public class ContentAutoCompleters extends AutoCompleters {

    AutoCompletePreferences preferences;


    public ContentAutoCompleters(AutoCompletePreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    public ContentAutoCompleters(BibDatabase database, MetaData metaData, AutoCompletePreferences preferences) {
        this(preferences);

        AutoCompleterFactory autoCompleterFactory = new AutoCompleterFactory(preferences);
        String[] completeFields = preferences.getCompleteNames();
        for (String field : completeFields) {
            AutoCompleter<String> autoCompleter = autoCompleterFactory.getFor(field);
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
                        ac.addItemToIndex(item);
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
                autoCompleter.addItemToIndex(abbreviation.getName());
            }
        }
    }
}
