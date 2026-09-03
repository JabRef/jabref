package org.jabref.gui.mergeentries.threewaymerge;

import java.util.Arrays;
import java.util.List;

import javax.swing.undo.UndoManager;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.gui.undo.UndoableRemoveEntries;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
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

        BibDatabaseContext databaseContext = stateManager.getActiveDatabase().get();
        BibDatabase database = databaseContext.getDatabase();
        List<BibEntry> entriesToRemove = Arrays.asList(entriesMergeResult.originalLeftEntry(), entriesMergeResult.originalRightEntry());
        BibEntry mergedEntry = entriesMergeResult.mergedEntry();

        database.insertEntry(mergedEntry);
        database.removeEntries(entriesToRemove);

        NamedCompoundEdit compoundEdit = new NamedCompoundEdit(Localization.lang("Merge entries"));
        compoundEdit.addEdit(new UndoableInsertEntries(database, mergedEntry));
        compoundEdit.addEdit(new UndoableRemoveEntries(database, entriesToRemove));
        compoundEdit.end();

        undoManager.addEdit(compoundEdit);

        stateManager.setSelectedEntries(List.of(mergedEntry));
        stateManager.activeTabProperty().get()
                    .filter(tab -> databaseContext.getUid().equals(tab.getBibDatabaseContext().getUid()))
                    .ifPresent(tab -> tab.clearAndSelect(mergedEntry));
    }
}
