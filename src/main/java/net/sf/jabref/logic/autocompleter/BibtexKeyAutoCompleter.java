package net.sf.jabref.logic.autocompleter;

import net.sf.jabref.model.entry.BibEntry;

/**
 * Delivers possible completions for a given string based on the key fields of the added items.
 *
 * @author kahlert, cordes
 */
class BibtexKeyAutoCompleter extends AbstractAutoCompleter {

    public BibtexKeyAutoCompleter(AutoCompletePreferences preferences) {
        super(preferences);
    }

    @Override
    public boolean isSingleUnitField() {
        return false;
    }

    /**
     * {@inheritDoc}
     * The bibtex key of the entry will be added to the index.
     */
    @Override
    public void addBibtexEntry(BibEntry entry) {
        if (entry == null) {
            return;
        }

        entry.getCiteKeyOptional().ifPresent(key -> addItemToIndex(key.trim()));
    }

    @Override
    protected int getLengthOfShortestWordToAdd() {
        return 1;
    }
}
