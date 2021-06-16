package org.jabref.migrations;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergeReviewIntoCommentMigration {
    public static final Logger LOGGER = LoggerFactory.getLogger(MergeReviewIntoCommentMigration.class);

    public static boolean needsMigration(ParserResult parserResult) {
        return parserResult.getDatabase().getEntries().stream()
                           .anyMatch(bibEntry -> bibEntry.getField(StandardField.REVIEW).isPresent());
    }

    public void performMigration(ParserResult parserResult) {
        /* This migration only handles the non-conflicting entries.
         * For the other see this.performConflictingMigration().
         */
        List<BibEntry> entries = Objects.requireNonNull(parserResult).getDatabase().getEntries();

        entries.stream()
               .filter(MergeReviewIntoCommentMigration::hasReviewField)
               .filter(entry -> !MergeReviewIntoCommentMigration.hasCommentField(entry))
               .forEach(entry -> migrate(entry, parserResult));
    }

    public static List<BibEntry> collectConflicts(ParserResult parserResult) {
        List<BibEntry> entries = Objects.requireNonNull(parserResult).getDatabase().getEntries();

        return entries.stream()
                      .filter(MergeReviewIntoCommentMigration::hasReviewField)
                      .filter(MergeReviewIntoCommentMigration::hasCommentField)
                      .collect(Collectors.toList());
    }

    public void performConflictingMigration(ParserResult parserResult) {
        collectConflicts(parserResult).forEach(entry -> migrate(entry, parserResult));
    }

    private static boolean hasCommentField(BibEntry entry) {
        return entry.getField(StandardField.COMMENT).isPresent();
    }

    private static boolean hasReviewField(BibEntry entry) {
        return entry.getField(StandardField.REVIEW).isPresent();
    }

    private String mergeCommentFieldIfPresent(BibEntry entry, String review) {
        if (entry.getField(StandardField.COMMENT).isPresent()) {
            LOGGER.info(String.format("Both Comment and Review fields are present in %s! Merging them into the comment field.", entry.getAuthorTitleYear(150)));
            return String.format("%s\n%s:\n%s", entry.getField(StandardField.COMMENT).get().trim(), Localization.lang("Review"), review.trim());
        }
        return review;
    }

    private void migrate(BibEntry entry, ParserResult parserResult) {
        if (hasReviewField(entry)) {
            updateFields(entry, mergeCommentFieldIfPresent(entry, entry.getField(StandardField.REVIEW).get()));
            parserResult.wasChangedOnMigration();
        }
    }

    private void updateFields(BibEntry entry, String review) {
        entry.setField(StandardField.COMMENT, review);
        entry.clearField(StandardField.REVIEW);
    }
}
