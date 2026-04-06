package org.jabref.logic.relatedwork;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.os.OS;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.UserSpecificCommentField;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class RelatedWorkService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedWorkService.class);
    private static final Pattern COMMENT_LINE_SEPARATOR_PATTERN = Pattern.compile("\\R\\R+");

    private final RelatedWorkTextParser relatedWorkTextParser;
    private final RelatedWorkReferenceResolver relatedWorkReferenceResolver;
    private final DuplicateCheck duplicateCheck;

    public RelatedWorkService(RelatedWorkTextParser relatedWorkTextParser, RelatedWorkReferenceResolver relatedWorkReferenceResolver, DuplicateCheck duplicateCheck) {
        this.relatedWorkTextParser = relatedWorkTextParser;
        this.relatedWorkReferenceResolver = relatedWorkReferenceResolver;
        this.duplicateCheck = duplicateCheck;
    }

    /// Try to find a matched bib entry in library
    ///
    /// @param sourceEntry selected bib entry in library
    /// @return List of matched result
    public List<RelatedWorkMatchResult> matchRelatedWork(BibDatabaseContext databaseContext,
                                                         BibEntry sourceEntry,
                                                         LinkedFile linkedFile,
                                                         String relatedWorkText,
                                                         FilePreferences filePreferences) throws IOException {
        List<RelatedWorkSnippet> relatedWorkSnippets = relatedWorkTextParser.parseRelatedWork(relatedWorkText);
        if (relatedWorkSnippets.isEmpty()) {
            LOGGER.debug("No citations were parsed from the related work text.");
            return List.of();
        }

        Map<String, BibEntry> parsedReferencesByMarker = relatedWorkReferenceResolver.parseReferences(linkedFile, databaseContext, filePreferences);

        return createMatchResults(sourceEntry, relatedWorkSnippets, parsedReferencesByMarker, databaseContext);
    }

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
            // In case a result does not match any library entry
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

    private List<RelatedWorkMatchResult> createMatchResults(BibEntry sourceEntry,
                                                            List<RelatedWorkSnippet> relatedWorkSnippets,
                                                            Map<String, BibEntry> referencesByMarker,
                                                            BibDatabaseContext databaseContext) {
        List<RelatedWorkMatchResult> matchResults = new ArrayList<>(relatedWorkSnippets.size());

        for (RelatedWorkSnippet relatedWorkSnippet : relatedWorkSnippets) {
            Optional<BibEntry> parsedReference = Optional.ofNullable(referencesByMarker.get(relatedWorkSnippet.citationMarker()));
            Optional<BibEntry> matchedLibraryEntry = parsedReference.flatMap(reference -> findDuplicateBibEntry(sourceEntry, reference, databaseContext));
            matchResults.add(new RelatedWorkMatchResult(
                    relatedWorkSnippet.contextText(),
                    relatedWorkSnippet.citationMarker(),
                    parsedReference,
                    matchedLibraryEntry
            ));
        }

        return matchResults;
    }

    /// Find duplicate entry in library by parsedReference
    private Optional<BibEntry> findDuplicateBibEntry(BibEntry sourceEntry,
                                                     BibEntry parsedReference,
                                                     BibDatabaseContext databaseContext) {
        return duplicateCheck.containsDuplicate(databaseContext.getDatabase(), parsedReference, databaseContext.getMode())
                             .filter(duplicateEntry -> !duplicateEntry.getId().equals(sourceEntry.getId()));
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
                .map(comment -> comment.stripTrailing() + OS.NEWLINE + OS.NEWLINE + formattedComment)
                .orElse(formattedComment);

        return matchedLibraryEntry.setField(userSpecificCommentField, updatedComment);
    }
}
