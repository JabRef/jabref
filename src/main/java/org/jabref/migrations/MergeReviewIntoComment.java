package org.jabref.migrations;

import java.util.Objects;

import org.jabref.gui.dialogs.MergeReviewIntoCommentUIManager;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergeReviewIntoComment implements PostOpenMigration {
    public static final Logger LOGGER = LoggerFactory.getLogger(MergeReviewIntoComment.class);

    @Override
    public void performMigration(ParserResult parserResult) {
        Objects.requireNonNull(parserResult);

        for (BibEntry entry : parserResult.getDatabase().getEntries()) {
            if (entry.getField(FieldName.REVIEW).isPresent()) {
                String review = entry.getField(FieldName.REVIEW).get();
                review = mergeCommentFieldIfPresent(entry, review);
                updateFields(entry, review);
            }
        }
    }

    private String mergeCommentFieldIfPresent(BibEntry entry, String review) {
        if (entry.getField(FieldName.COMMENT).isPresent()) {
            String comment = entry.getField(FieldName.COMMENT).get().trim();
            if (!comment.isEmpty()) {
                if (new MergeReviewIntoCommentUIManager().showMergeReviewIntoCommentConflictDialog(entry)) {
                    LOGGER.info(String.format("Both Comment and Review fields are present in %s! Merging them into the comment field.", entry.getAuthorTitleYear(150)));
                    return String.format("%s\n%s:\n%s", comment, Localization.lang("Review"), review.trim());
                }
            }
        }

        return review;
    }

    private void updateFields(BibEntry entry, String review) {
        entry.setField(FieldName.COMMENT, review);
        entry.clearField(FieldName.REVIEW);
    }
}
