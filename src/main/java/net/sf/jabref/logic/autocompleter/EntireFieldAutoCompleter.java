package net.sf.jabref.logic.autocompleter;

import java.util.Objects;

import net.sf.jabref.model.entry.BibEntry;

/**
 * Delivers possible completions for a given string.
 * Stores the full original value of one field of the given BibtexEntries.
 *
 * @author kahlert, cordes
 */
class EntireFieldAutoCompleter extends AbstractAutoCompleter {

    private final String fieldName;

    /**
     * @see AutoCompleterFactory
     */
    EntireFieldAutoCompleter(String fieldName, AutoCompletePreferences preferences) {
        super(preferences);

        this.fieldName = Objects.requireNonNull(fieldName);
    }

    @Override
    public boolean isSingleUnitField() {
        return true;
    }

    /**
     * {@inheritDoc}
     * Stores the full original value of the given field.
     */
    @Override
    public void addBibtexEntry(BibEntry entry) {
        if (entry == null) {
            return;
        }

        entry.getField(fieldName).ifPresent(fieldValue -> addItemToIndex(fieldValue.trim()));
    }
}
