package org.jabref.gui.collab;

import javafx.scene.Node;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.preview.PreviewViewer;
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
        undoEdit.addEdit(new UndoableRemoveEntry(database.getDatabase(), memEntry));
    }

    @Override
    public Node description() {
        PreviewViewer previewViewer = new PreviewViewer(new BibDatabaseContext(), JabRefGUI.getMainFrame().getDialogService(), Globals.stateManager);
        previewViewer.setEntry(memEntry);
        return previewViewer;
    }
}
