package org.jabref.gui.autocompleter;

import java.util.Comparator;
import java.util.Locale;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.ParsedEntryLink;

import org.controlsfx.control.textfield.AutoCompletionBinding.ISuggestionRequest;

public class ParsedEntryLinkSuggestionProvider extends SuggestionProvider<ParsedEntryLink> implements AutoCompleteSuggestionProvider<ParsedEntryLink> {

    @Override
    public void indexEntry(BibEntry entry) {
        if (entry == null) {
            return;
        }

        addPossibleSuggestions(new ParsedEntryLink(entry));
    }

    @Override
    protected Comparator<ParsedEntryLink> getComparator() {
        return (o1, o2) -> o1.getKey().compareTo(o2.getKey());
    }

    @Override
    protected boolean isMatch(ParsedEntryLink suggestion, ISuggestionRequest request) {
        String userTextLower = request.getUserText().toLowerCase(Locale.ROOT);
        return suggestion.getKey().toLowerCase(Locale.ROOT).contains(userTextLower);
    }
}