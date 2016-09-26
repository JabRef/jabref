package net.sf.jabref.logic.autocompleter;

import java.util.List;
import java.util.Objects;

import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.model.database.BibDatabase;

public class ContentAutoCompleters extends AutoCompleters {

    public ContentAutoCompleters() {
    }

    public ContentAutoCompleters(BibDatabase database, AutoCompletePreferences preferences,
            JournalAbbreviationLoader abbreviationLoader) {
        Objects.requireNonNull(preferences);

        AutoCompleterFactory autoCompleterFactory = new AutoCompleterFactory(preferences, abbreviationLoader);
        List<String> completeFields = preferences.getCompleteNames();
        for (String field : completeFields) {
            AutoCompleter<String> autoCompleter = autoCompleterFactory.getFor(field);
            put(field, autoCompleter);
        }

        addDatabase(database);
    }
}
