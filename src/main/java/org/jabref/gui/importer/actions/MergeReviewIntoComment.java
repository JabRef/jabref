package org.jabref.gui.importer.actions;

import org.jabref.gui.BasePanel;
import org.jabref.logic.importer.MergeReviewIntoCommentMigration;
import org.jabref.logic.importer.ParserResult;

public class MergeReviewIntoComment implements GUIPostOpenAction {

    @Override
    public boolean isActionNecessary(ParserResult parserResult) {
        return !MergeReviewIntoCommentMigration.collectConflicts(parserResult).isEmpty();
    }

    @Override
    public void performAction(BasePanel basePanel, ParserResult parserResult) {
        if (new MergeReviewIntoCommentConfirmation(basePanel).askUserForMerge(MergeReviewIntoCommentMigration.collectConflicts(parserResult))) {
            new MergeReviewIntoCommentMigration().performConflictingMigration(parserResult);
        }
    }
}
