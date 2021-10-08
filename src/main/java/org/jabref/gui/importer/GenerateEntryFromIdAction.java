package org.jabref.gui.importer;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.importer.CompositeIdFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

public class GenerateEntryFromIdAction extends SimpleCommand {
    private final LibraryTab libraryTab;
    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final String id;

    public GenerateEntryFromIdAction(LibraryTab libraryTab, DialogService dialogService, PreferencesService preferencesService, String id) {
        this.libraryTab = libraryTab;
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.id = id;
    }

    @Override
    public void execute() {
        Optional<BibEntry> fetchedEntry;

        CompositeIdFetcher compositeIdFetcher = new CompositeIdFetcher(preferencesService.getImportFormatPreferences());
        fetchedEntry = compositeIdFetcher.performSearchById(id);

        fetchedEntry.ifPresentOrElse(
                libraryTab::insertEntry,
                () -> dialogService.showErrorDialogAndWait("Could not generate an entry from this ID")
        );
    }

}
