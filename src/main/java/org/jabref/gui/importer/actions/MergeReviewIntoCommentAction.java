package org.jabref.gui.importer.actions;

import java.util.List;

import org.jabref.gui.LibraryTab;
import org.jabref.logic.importer.ParserResult;
import org.jabref.migrations.MergeReviewIntoCommentMigration;
import org.jabref.model.entry.BibEntry;

public class MergeReviewIntoCommentAction implements GUIPostOpenAction {

    @Override
    public boolean isActionNecessary(ParserResult parserResult) {
        return MergeReviewIntoCommentMigration.needsMigration(parserResult);
    }

    @Override
    public void performAction(LibraryTab libraryTab, ParserResult parserResult) {
        MergeReviewIntoCommentMigration migration = new MergeReviewIntoCommentMigration();

        migration.performMigration(parserResult);
        List<BibEntry> conflicts = MergeReviewIntoCommentMigration.collectConflicts(parserResult);
        if (!conflicts.isEmpty() && new MergeReviewIntoCommentConfirmationDialog(libraryTab.frame().getDialogService()).askUserForMerge(conflicts)) {
            migration.performConflictingMigration(parserResult);
        }
    }
}
