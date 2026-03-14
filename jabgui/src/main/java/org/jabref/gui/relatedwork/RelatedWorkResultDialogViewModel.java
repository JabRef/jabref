package org.jabref.gui.relatedwork;

import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.relatedwork.RelatedWorkInsertionResult;
import org.jabref.logic.relatedwork.RelatedWorkInsertionStatus;
import org.jabref.logic.relatedwork.RelatedWorkMatchResult;
import org.jabref.logic.relatedwork.RelatedWorkReferenceResolver;
import org.jabref.logic.relatedwork.RelatedWorkService;
import org.jabref.logic.relatedwork.RelatedWorkTextParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

public class RelatedWorkResultDialogViewModel extends AbstractViewModel {

    private final BibEntry sourceEntry;
    private final String userName;
    private final DialogService dialogService;
    private final UndoManager undoManager;
    private final RelatedWorkService relatedWorkService;
    private final List<RelatedWorkMatchResult> matchedResults;
    private final ObservableList<RelatedWorkMatchResult> matchedReferences = FXCollections.observableArrayList();

    public RelatedWorkResultDialogViewModel(BibEntry sourceEntry,
                                            List<RelatedWorkMatchResult> matchedResults,
                                            String userName,
                                            DialogService dialogService,
                                            UndoManager undoManager,
                                            BibEntryTypesManager entryTypesManager) {
        this.sourceEntry = sourceEntry;
        this.userName = userName.strip();
        this.dialogService = dialogService;
        this.undoManager = undoManager;
        this.relatedWorkService = new RelatedWorkService(
                new RelatedWorkTextParser(),
                new RelatedWorkReferenceResolver(),
                new DuplicateCheck(entryTypesManager)
        );
        this.matchedResults = List.copyOf(matchedResults);
        matchedReferences.setAll(this.matchedResults);
    }

    public ObservableList<RelatedWorkMatchResult> matchedReferencesProperty() {
        return matchedReferences;
    }

    public boolean insertComments() {
        List<RelatedWorkInsertionResult> insertionResults = relatedWorkService.insertMatchedRelatedWork(
                sourceEntry,
                matchedResults,
                userName
        );

        int insertedCount = 0;
        int unchangedCount = 0;
        int skippedCount = 0;
        NamedCompoundEdit compoundEdit = new NamedCompoundEdit(Localization.lang("Insert related work comments"));

        for (RelatedWorkInsertionResult insertionResult : insertionResults) {
            RelatedWorkInsertionStatus insertionStatus = insertionResult.status();
            switch (insertionStatus) {
                case RelatedWorkInsertionStatus.INSERTED ->
                        insertedCount++;
                case RelatedWorkInsertionStatus.UNCHANGED ->
                        unchangedCount++;
                default ->
                        skippedCount++;
            }

            insertionResult.fieldChange().ifPresent(change -> compoundEdit.addEdit(new UndoableFieldChange(change)));
        }

        if (compoundEdit.hasEdits()) {
            undoManager.addEdit(compoundEdit);
        }

        dialogService.notify(Localization.lang(
                "Processed related work comments. Inserted: %0, unchanged: %1, skipped: %2.",
                String.valueOf(insertedCount),
                String.valueOf(unchangedCount),
                String.valueOf(skippedCount)
        ));
        return true;
    }
}
