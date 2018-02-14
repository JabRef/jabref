package org.jabref.migrations;

import java.util.Objects;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

public class RenameReviewToComment implements PostOpenMigration {

    @Override
    public void performMigration(ParserResult parserResult) {
        Objects.requireNonNull(parserResult);

        for (BibEntry entry : parserResult.getDatabase().getEntries()) {
            if (entry.getField(FieldName.REVIEW).isPresent()) {
                String review = entry.getField(FieldName.REVIEW).get();
                review = concatCommentFieldIfPresent(entry, review);
                updateFields(entry, review);
            }
        }
    }

    private String concatCommentFieldIfPresent(BibEntry entry, String review) {
        if (entry.getField(FieldName.COMMENT).isPresent()) {
            String comment = entry.getField(FieldName.COMMENT).get().trim();
            if (!comment.isEmpty()) {
                return String.format("%s\nReview:\n%s", comment, review.trim());
            }
        }

        return review;
    }

    private void updateFields(BibEntry entry, String review) {
        entry.setField(FieldName.COMMENT, review);
        entry.clearField(FieldName.REVIEW);
    }
}
