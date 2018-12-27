package org.jabref.gui.autocompleter;

import java.util.Collection;

import javafx.util.Callback;

import org.jabref.model.entry.BibEntry;

import org.controlsfx.control.textfield.AutoCompletionBinding;

public interface AutoCompleteSuggestionProvider<T> extends Callback<AutoCompletionBinding.ISuggestionRequest, Collection<T>> {
    void indexEntry(BibEntry entry);
}
