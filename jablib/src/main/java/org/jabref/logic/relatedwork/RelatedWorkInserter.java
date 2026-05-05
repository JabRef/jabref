package org.jabref.logic.relatedwork;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.logic.os.OS;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.UserSpecificCommentField;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class RelatedWorkInserter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedWorkInserter.class);
    private static final Pattern COMMENT_LINE_SEPARATOR_PATTERN = Pattern.compile("\\R\\R+");

    /// This method appends a “comment” to each matched bib entry unless the same “comment” already exists
    /// Specifically, related work text is inserted into the `comment-{username}` field of the matched bib entries
    ///
    /// @return List of insertion result
    public List<RelatedWorkInsertionResult> insertMatchedRelatedWork(BibEntry sourceEntry,
                                                                     List<RelatedWorkMatchResult> matchResults,
                                                                     String userName) {
        String sourceCitationKey = sourceEntry.getCitationKey().get();

        UserSpecificCommentField userSpecificCommentField = new UserSpecificCommentField(userName);
        List<RelatedWorkInsertionResult> insertionResults = new ArrayList<>(matchResults.size());

        for (RelatedWorkMatchResult matchResult : matchResults) {
            if (matchResult.matchedLibraryBibEntry().isEmpty()) {
                LOGGER.debug("Could not find a matched bib entry for citation {}.", matchResult.citationKey());
                continue;
            }

            Optional<FieldChange> fieldChange = appendRelatedWorkComment(
                    sourceCitationKey,
                    matchResult.contextText(),
                    matchResult.matchedLibraryBibEntry().get(),
                    userSpecificCommentField
            );
            fieldChange.ifPresentOrElse(
                    change -> insertionResults.add(new RelatedWorkInsertionResult.Inserted(matchResult, change)),
                    () -> insertionResults.add(new RelatedWorkInsertionResult.Unchanged(matchResult))
            );
        }

        return insertionResults;
    }

    /// Insert "comment-{username}" to target bib entry
    /// If "comment-{username}" not exists, then create a new field; Otherwise append it and separate by an empty line.
    ///
    /// @param userSpecificCommentField comment-{username}
    /// @return FieldChange represents if the field is changed
    private Optional<FieldChange> appendRelatedWorkComment(String sourceCitationKey,
                                                           String contextText,
                                                           BibEntry matchedLibraryEntry,
                                                           UserSpecificCommentField userSpecificCommentField) {
        String formattedComment = "[%s]: %s".formatted(sourceCitationKey, contextText);
        Optional<String> existingComment = matchedLibraryEntry.getField(userSpecificCommentField);
        if (existingComment.stream()
                           .flatMap(comment -> COMMENT_LINE_SEPARATOR_PATTERN.splitAsStream(comment.strip()))
                           .anyMatch(formattedComment::equals)) {
            LOGGER.debug("Insertion for citation {} is skipped, because comment already exists in {}.",
                    sourceCitationKey,
                    userSpecificCommentField);
            return Optional.empty();
        }

        String updatedComment = existingComment
                .filter(comment -> !comment.isBlank())
                .map(comment -> StringUtil.unifyLineBreaks(comment.stripTrailing(), OS.NEWLINE) + OS.NEWLINE + OS.NEWLINE + formattedComment)
                .orElse(formattedComment);

        return matchedLibraryEntry.setField(userSpecificCommentField, updatedComment);
    }
}
