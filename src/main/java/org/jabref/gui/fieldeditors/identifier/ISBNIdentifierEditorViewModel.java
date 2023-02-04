package org.jabref.gui.fieldeditors.identifier;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGUI;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.mergeentries.FetchAndMergeEntry;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ISBN;
import org.jabref.preferences.PreferencesService;

public class ISBNIdentifierEditorViewModel extends BaseIdentifierEditorViewModel<ISBN> {
    public ISBNIdentifierEditorViewModel(SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, DialogService dialogService, TaskExecutor taskExecutor, PreferencesService preferences) {
        super(StandardField.ISBN, suggestionProvider, fieldCheckers, dialogService, taskExecutor, preferences);
        configure(true, false);
    }

    @Override
    public void fetchBibliographyInformation(BibEntry bibEntry) {
        new FetchAndMergeEntry(JabRefGUI.getMainFrame().getCurrentLibraryTab(), taskExecutor, preferences, dialogService).fetchAndMerge(entry, field);
    }
}
