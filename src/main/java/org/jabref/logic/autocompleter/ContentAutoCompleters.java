package org.jabref.logic.autocompleter;

import java.util.List;
import java.util.Objects;

import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.metadata.MetaData;

public class ContentAutoCompleters extends AutoCompleters {

    public ContentAutoCompleters() {
    }

    public ContentAutoCompleters(BibDatabase database, MetaData metaData, AutoCompletePreferences preferences,
            JournalAbbreviationLoader abbreviationLoader) {
        Objects.requireNonNull(preferences);

        AutoCompleterFactory autoCompleterFactory = new AutoCompleterFactory(preferences, abbreviationLoader);
        List<String> completeFields = preferences.getCompleteNames();
        for (String field : completeFields) {
            AutoCompleteSuggestionProvider<?> autoCompleter = autoCompleterFactory.getForField(field);
            put(field, autoCompleter);
        }
        addContentSelectorValuesToAutoCompleters(metaData);

        addDatabase(database);
    }

    /**
     * For all fields with both autocompletion and content selector, add content selector
     * values to the autocompleter list:
     */
    public void addContentSelectorValuesToAutoCompleters(MetaData metaData) {
        /*
        for (Map.Entry<String, AutoCompleteSuggestionProvider<?>> entry : this.autoCompleters.entrySet()) {
            AutoCompleteSuggestionProvider<?> ac = entry.getValue();
            metaData.getContentSelectorValuesForField(entry.getKey()).forEach(ac::addItemToIndex);
        }
        */
    }
}
