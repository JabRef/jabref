package net.sf.jabref.logic.autocompleter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.Globals;
import net.sf.jabref.MetaData;
import net.sf.jabref.logic.journals.Abbreviation;

public class ContentAutoCompleters extends AutoCompleters {

    private final JournalAbbreviationLoader abbreviationLoader;


    public ContentAutoCompleters(JournalAbbreviationLoader abbreviationLoader) {
        this.abbreviationLoader = Objects.requireNonNull(abbreviationLoader);
    }

    public ContentAutoCompleters(BibDatabase database, MetaData metaData, AutoCompletePreferences preferences,
            JournalAbbreviationLoader abbreviationLoader) {
        this(abbreviationLoader);
        Objects.requireNonNull(preferences);

        AutoCompleterFactory autoCompleterFactory = new AutoCompleterFactory(preferences);
        List<String> completeFields = preferences.getCompleteNames();
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
                List<String> items = metaData.getData(Globals.SELECTOR_META_PREFIX + entry.getKey());
                if (items != null) {
                    items.forEach(ac::addItemToIndex);
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
            for (Abbreviation abbreviation : abbreviationLoader.getRepository().getAbbreviations()) {
                autoCompleter.addItemToIndex(abbreviation.getName());
            }
        }
    }
}
