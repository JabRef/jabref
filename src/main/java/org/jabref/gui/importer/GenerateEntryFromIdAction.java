package org.jabref.gui.importer;


import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.importer.CompositeIdFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

import java.util.Optional;

public class GenerateEntryFromIdAction extends SimpleCommand {
    private final JabRefFrame jabRefFrame;
    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final String id;

    public GenerateEntryFromIdAction(JabRefFrame jabRefFrame, DialogService dialogService, PreferencesService preferencesService, String id) {
        this.jabRefFrame = jabRefFrame;
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
                bibEntry -> jabRefFrame.getCurrentLibraryTab().insertEntry(bibEntry),
                () -> dialogService.showErrorDialogAndWait("Could not generate an entry from this ID")
        );
    }

}
