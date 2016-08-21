package net.sf.jabref.logic.autocompleter;

import java.util.Objects;

import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.model.entry.FieldName;
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
        } else if (InternalBibtexFields.getFieldExtras(fieldName).contains(FieldProperties.SINGLE_ENTRY_LINK)) {
            return new BibtexKeyAutoCompleter(preferences);
        } else if (InternalBibtexFields.getFieldExtras(fieldName).contains(FieldProperties.JOURNAL_NAME)
                || FieldName.PUBLISHER.equals(fieldName)) {
            return new JournalAutoCompleter(fieldName, preferences, abbreviationLoader);
        } else {
            return new DefaultAutoCompleter(fieldName, preferences);
        }
    }

    public AutoCompleter<String> getPersonAutoCompleter() {
        return new NameFieldAutoCompleter(InternalBibtexFields.getPersonNameFields(), true, preferences);
    }

}
