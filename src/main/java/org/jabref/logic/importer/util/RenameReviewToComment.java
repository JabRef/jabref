package org.jabref.logic.importer.util;

import java.util.Objects;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

public class RenameReviewToComment implements PostOpenAction {

    @Override
    public void performAction(ParserResult parserResult) {
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
                review = String.format("%s\n Review:\n%s", comment, review.trim());
            }
        }

        return review;
    }

    private void updateFields(BibEntry entry, String review) {
        entry.setField(FieldName.COMMENT, review);
        entry.clearField(FieldName.REVIEW);
    }
}
