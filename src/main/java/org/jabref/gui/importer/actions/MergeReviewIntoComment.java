package org.jabref.gui.importer.actions;

import org.jabref.gui.BasePanel;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.migrations.MergeReviewIntoCommentMigration;

public class MergeReviewIntoComment implements GUIPostOpenAction {

    @Override
    public boolean isActionNecessary(ParserResult parserResult) {
        return MergeReviewIntoCommentMigration.needsMigration(parserResult);
    }

    @Override
    public void performAction(BasePanel basePanel, ParserResult parserResult) {
        MergeReviewIntoCommentMigration migration = new MergeReviewIntoCommentMigration();

        migration.performMigration(parserResult);
        if (new MergeReviewIntoCommentConfirmation(basePanel).askUserForMerge(MergeReviewIntoCommentMigration.collectConflicts(parserResult))) {
            migration.performConflictingMigration(parserResult);
        }
    }
}
