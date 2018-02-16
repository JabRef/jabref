package org.jabref.migrations;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.collections.ObservableList;

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
        ObservableList<BibEntry> entries = Objects.requireNonNull(parserResult).getDatabase().getEntries();

        // migrate non-conflicting entries first
        entries.stream()
                .filter(this::hasReviewField)
                .filter(entry -> !this.hasCommentField(entry))
                .forEach(entry -> migrate(entry, parserResult));

        // determine conflicts
        List<BibEntry> conflicts = entries.stream()
                .filter(this::hasReviewField)
                .filter(this::hasCommentField)
                .collect(Collectors.toList());

        // resolve conflicts if users agrees
        if (!conflicts.isEmpty() && new MergeReviewIntoCommentUIManager().askUserForMerge(conflicts)) {
            conflicts.stream()
                    .filter(this::hasReviewField)
                    .forEach(entry -> migrate(entry, parserResult));
        }
    }

    private String mergeCommentFieldIfPresent(BibEntry entry, String review) {
        if (entry.getField(FieldName.COMMENT).isPresent()) {
            LOGGER.info(String.format("Both Comment and Review fields are present in %s! Merging them into the comment field.", entry.getAuthorTitleYear(150)));
            return String.format("%s\n%s:\n%s", entry.getField(FieldName.COMMENT).get().trim(), Localization.lang("Review"), review.trim());
        }
        return review;
    }

    private boolean hasCommentField(BibEntry entry) {
        return entry.getField(FieldName.COMMENT).isPresent();
    }

    private boolean hasReviewField(BibEntry entry) {
        return entry.getField(FieldName.REVIEW).isPresent();
    }

    private void migrate(BibEntry entry, ParserResult parserResult) {
        updateFields(entry, mergeCommentFieldIfPresent(entry, entry.getField(FieldName.REVIEW).get()));
        parserResult.wasChangedOnMigration = true;
    }

    private void updateFields(BibEntry entry, String review) {
        entry.setField(FieldName.COMMENT, review);
        entry.clearField(FieldName.REVIEW);
    }
}
