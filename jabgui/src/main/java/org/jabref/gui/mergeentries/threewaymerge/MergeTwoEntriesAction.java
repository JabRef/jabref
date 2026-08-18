package org.jabref.gui.mergeentries.threewaymerge;

import java.util.Arrays;
import java.util.List;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.UndoManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.change.UndoableInsertEntries;
import org.jabref.model.change.UndoableRemoveEntries;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

public class MergeTwoEntriesAction extends SimpleCommand {
    private final EntriesMergeResult entriesMergeResult;
    private final StateManager stateManager;
    private final UndoManager undoManager;

    public MergeTwoEntriesAction(EntriesMergeResult entriesMergeResult, StateManager stateManager, UndoManager undoManager) {
        this.entriesMergeResult = entriesMergeResult;
        this.stateManager = stateManager;
        this.undoManager = undoManager;
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

        undoManager.record(Localization.lang("Merge entries"), recorder -> {
            recorder.record(new UndoableInsertEntries(stateManager.getActiveDatabase().get().getDatabase(), entriesMergeResult.mergedEntry()));
            recorder.record(new UndoableRemoveEntries(database, entriesToRemove));
        });
    }
}
