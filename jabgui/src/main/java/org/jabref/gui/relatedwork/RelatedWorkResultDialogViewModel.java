package org.jabref.gui.relatedwork;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.undo.UndoManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.relatedwork.RelatedWorkInserter;
import org.jabref.logic.relatedwork.RelatedWorkInsertionResult;
import org.jabref.logic.relatedwork.RelatedWorkMatchResult;
import org.jabref.model.entry.BibEntry;

public class RelatedWorkResultDialogViewModel extends AbstractViewModel {
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]");

    private final DialogService dialogService;
    private final UndoManager undoManager;
    private final BibEntry sourceEntry;
    private final RelatedWorkInserter relatedWorkInserter;
    private final List<RelatedWorkMatchResult> matchedResults;
    private final ObservableList<RelatedWorkMatchResult> matchedReferences = FXCollections.observableArrayList();
    private final String userName;

    public RelatedWorkResultDialogViewModel(BibEntry sourceEntry,
                                            List<RelatedWorkMatchResult> matchedResults,
                                            String userName,
                                            DialogService dialogService,
                                            UndoManager undoManager) {
        this.sourceEntry = sourceEntry;
        this.userName = NON_ALPHANUMERIC.matcher(userName.toLowerCase(Locale.ROOT)).replaceAll("-");
        this.dialogService = dialogService;
        this.undoManager = undoManager;
        this.relatedWorkInserter = new RelatedWorkInserter();
        this.matchedResults = matchedResults;
        matchedReferences.setAll(this.matchedResults);
    }

    public ObservableList<RelatedWorkMatchResult> matchedReferencesProperty() {
        return matchedReferences;
    }

    public boolean insertComments() {
        List<RelatedWorkInsertionResult> insertionResults = relatedWorkInserter.insertMatchedRelatedWork(
                sourceEntry,
                matchedResults,
                userName
        );

        AtomicInteger insertedCount = new AtomicInteger();
        AtomicInteger unchangedCount = new AtomicInteger();

        undoManager.record(Localization.lang("Insert related work comments"), recorder -> {
            for (RelatedWorkInsertionResult insertionResult : insertionResults) {
                switch (insertionResult) {
                    case RelatedWorkInsertionResult.Inserted inserted -> {
                        insertedCount.incrementAndGet();
                        recorder.record(inserted.fieldChange());
                    }
                    case RelatedWorkInsertionResult.Unchanged unchanged ->
                            unchangedCount.incrementAndGet();
                }
            }
        });

        dialogService.notify(Localization.lang(
                "Processed related work comments. Inserted: %0, unchanged: %1.",
                String.valueOf(insertedCount.get()),
                String.valueOf(unchangedCount.get())
        ));
        return true;
    }
}
