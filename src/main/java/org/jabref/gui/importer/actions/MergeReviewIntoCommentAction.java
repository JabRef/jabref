package org.jabref.gui.importer.actions;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.logic.importer.ParserResult;
import org.jabref.migrations.MergeReviewIntoCommentMigration;
import org.jabref.model.entry.BibEntry;
import org.jabref.logic.preferences.Preferences;

public class MergeReviewIntoCommentAction implements GUIPostOpenAction {

    @Override
    public boolean isActionNecessary(ParserResult parserResult, Preferences preferences) {
        return MergeReviewIntoCommentMigration.needsMigration(parserResult);
    }

    @Override
    public void performAction(ParserResult parserResult, DialogService dialogService, Preferences preferences) {
        MergeReviewIntoCommentMigration migration = new MergeReviewIntoCommentMigration();

        migration.performMigration(parserResult);
        List<BibEntry> conflicts = MergeReviewIntoCommentMigration.collectConflicts(parserResult);
        if (!conflicts.isEmpty() && new MergeReviewIntoCommentConfirmationDialog(dialogService).askUserForMerge(conflicts)) {
            migration.performConflictingMigration(parserResult);
        }
    }
}
