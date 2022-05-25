package org.jabref.gui.autocompleter;

import java.util.Comparator;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.comparator.EntryComparator;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.strings.StringUtil;

import com.google.common.base.Equivalence;
import org.controlsfx.control.textfield.AutoCompletionBinding;

/**
 * Delivers possible completions of a BibEntry title based on the search bar entry.
 */
public class TitleSuggestionProvider extends SuggestionProvider<BibEntry> {

    private final BibDatabase database;

    public TitleSuggestionProvider(BibDatabase database) {
        this.database = database;
    }

    @Override
    protected Equivalence<BibEntry> getEquivalence() {
        return Equivalence.equals().onResultOf(BibEntry::getTitle);
    }

    @Override
    protected Comparator<BibEntry> getComparator() {
        return new EntryComparator(false, true, InternalField.KEY_FIELD);
    }

    @Override
    protected boolean isMatch(BibEntry entry, AutoCompletionBinding.ISuggestionRequest request) {
        String userText = request.getUserText();
        return entry.getTitle()
                    .map(key -> StringUtil.containsIgnoreCase(key, userText))
                    .orElse(false);
    }

    @Override
    public Stream<BibEntry> getSource() {
        return database.getEntries().parallelStream();
    }
}
