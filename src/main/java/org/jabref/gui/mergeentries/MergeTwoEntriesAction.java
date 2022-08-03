package org.jabref.gui.mergeentries;

import java.util.Arrays;
import java.util.List;

import org.jabref.gui.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.gui.undo.UndoableRemoveEntries;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

public class MergeTwoEntriesAction extends SimpleCommand {
    private final EntriesMergeResult entriesMergeResult;
    private final JabRefFrame frame;
    private final StateManager stateManager;

    public MergeTwoEntriesAction(EntriesMergeResult entriesMergeResult, JabRefFrame frame, StateManager stateManager) {
        this.entriesMergeResult = entriesMergeResult;
        this.frame = frame;
        this.stateManager = stateManager;
    }

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        BibDatabase database = stateManager.getActiveDatabase().get().getDatabase();

        frame.getCurrentLibraryTab().insertEntry(entriesMergeResult.mergedEntry());

        NamedCompound ce = new NamedCompound(Localization.lang("Merge entries"));
        ce.addEdit(new UndoableInsertEntries(stateManager.getActiveDatabase().get().getDatabase(), entriesMergeResult.mergedEntry()));
        List<BibEntry> entriesToRemove = Arrays.asList(entriesMergeResult.originalLeftEntry(), entriesMergeResult.originalRightEntry());
        ce.addEdit(new UndoableRemoveEntries(database, entriesToRemove));
        database.removeEntries(entriesToRemove);
        ce.end();
        Globals.undoManager.addEdit(ce);
    }
}
