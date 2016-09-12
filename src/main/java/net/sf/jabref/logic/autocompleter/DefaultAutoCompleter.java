package net.sf.jabref.logic.autocompleter;

import java.util.Objects;
import java.util.StringTokenizer;

import net.sf.jabref.model.entry.BibEntry;

/**
 * Delivers possible completions for a given string.
 * Stores all words in the given field which are separated by SEPARATING_CHARS.
 *
 * @author kahlert, cordes
 */
class DefaultAutoCompleter extends AbstractAutoCompleter {

    private final String fieldName;

    private static final String SEPARATING_CHARS = ";,\n ";

    /**
     * @see AutoCompleterFactory
     */
    DefaultAutoCompleter(String fieldName, AutoCompletePreferences preferences) {
        super(preferences);

        this.fieldName = Objects.requireNonNull(fieldName);
    }

    @Override
    public boolean isSingleUnitField() {
        return false;
    }

    /**
     * {@inheritDoc}
     * Stores all words in the given field which are separated by SEPARATING_CHARS.
     */
    @Override
    public void addBibtexEntry(BibEntry entry) {
        if (entry == null) {
            return;
        }

        entry.getField(fieldName).ifPresent(fieldValue -> {
            StringTokenizer tok = new StringTokenizer(fieldValue, SEPARATING_CHARS);
            while (tok.hasMoreTokens()) {
                addItemToIndex(tok.nextToken());
            }
        });
    }
}
