package org.jabref.gui.collab;

import javafx.scene.Node;

import org.jabref.Globals;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.PreviewPanel;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableRemoveEntry;
import org.jabref.logic.bibtex.DuplicateCheck;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EntryDeleteChangeViewModel extends DatabaseChangeViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryDeleteChangeViewModel.class);
    private final BibEntry memEntry;
    private final BibEntry tmpEntry;

    public EntryDeleteChangeViewModel(BibEntry memEntry, BibEntry tmpEntry) {
        super(Localization.lang("Deleted entry"));
        this.memEntry = memEntry;
        this.tmpEntry = tmpEntry;

        // Compare the deleted entry in memory with the one in the tmpfile. The
        // entry could have been removed in memory.
        double matchWithTmp = DuplicateCheck.compareEntriesStrictly(memEntry, tmpEntry);

        // Check if it has been modified locally, since last tempfile was saved.
        boolean isModifiedLocally = (matchWithTmp <= 1);

        LOGGER.debug("Modified entry: " + memEntry.getCiteKeyOptional().orElse("<no BibTeX key set>")
                + "\n Modified locally: " + isModifiedLocally);
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        database.getDatabase().removeEntry(memEntry);
        undoEdit.addEdit(new UndoableRemoveEntry(database.getDatabase(), memEntry, null));
    }

    @Override
    public Node description() {
        PreviewPanel previewPanel = new PreviewPanel(null, null, Globals.getKeyPrefs(), Globals.prefs.getPreviewPreferences(), new FXDialogService(), ExternalFileTypes.getInstance());
        previewPanel.setEntry(memEntry);
        return previewPanel;
    }
}
