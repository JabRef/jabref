package org.jabref.gui.mergeentries;

import java.util.Arrays;
import java.util.List;

import org.jabref.gui.Globals;
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
    private final StateManager stateManager;

    public MergeTwoEntriesAction(EntriesMergeResult entriesMergeResult, StateManager stateManager) {
        this.entriesMergeResult = entriesMergeResult;
        this.stateManager = stateManager;
    }

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        BibDatabase database = stateManager.getActiveDatabase().get().getDatabase();
        List<BibEntry> entriesToRemove = Arrays.asList(entriesMergeResult.originalLeftEntry(), entriesMergeResult.originalRightEntry());

        database.insertEntry(entriesMergeResult.mergedEntry());
        database.removeEntries(entriesToRemove);

        NamedCompound ce = new NamedCompound(Localization.lang("Merge entries"));
        ce.addEdit(new UndoableInsertEntries(stateManager.getActiveDatabase().get().getDatabase(), entriesMergeResult.mergedEntry()));
        ce.addEdit(new UndoableRemoveEntries(database, entriesToRemove));
        ce.end();

        Globals.undoManager.addEdit(ce);
    }
}
