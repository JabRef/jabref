package org.jabref.gui.mergeentries.threewaymerge;

import java.util.Arrays;
import java.util.List;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.logic.undo.UndoManager;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.undo.UndoableInsertEntries;
import org.jabref.model.undo.UndoableRemoveEntries;

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

        BibDatabaseContext databaseContext = stateManager.getActiveDatabase().get();
        BibDatabase database = databaseContext.getDatabase();
        List<BibEntry> entriesToRemove = Arrays.asList(entriesMergeResult.originalLeftEntry(), entriesMergeResult.originalRightEntry());
        BibEntry mergedEntry = entriesMergeResult.mergedEntry();

        undoManager.addEdit(StandardActions.MERGE_ENTRIES.getText(), edit -> {
            edit.applyEdit(new UndoableInsertEntries(database, mergedEntry));
            edit.applyEdit(new UndoableRemoveEntries(database, entriesToRemove));
        });

        stateManager.setSelectedEntries(List.of(mergedEntry));
        stateManager.activeTabProperty().get()
                    .filter(tab -> databaseContext.getUid().equals(tab.getBibDatabaseContext().getUid()))
                    .ifPresent(tab -> tab.clearAndSelect(mergedEntry));
    }
}
