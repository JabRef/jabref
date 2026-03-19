package org.jabref.logic.relatedwork;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

public class RelatedWorkService {
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
    public List<RelatedWorkMatchResult> matchRelatedWork(BibEntry sourceEntry,
                                                         String relatedWorkText,
                                                         LinkedFile linkedFile,
                                                         BibDatabaseContext databaseContext,
                                                         FilePreferences filePreferences) throws IOException {
        List<RelatedWorkSnippet> relatedWorkSnippets = relatedWorkTextParser.parseRelatedWork(relatedWorkText);
        if (relatedWorkSnippets.isEmpty()) {
            return List.of();
        }

        Map<String, BibEntry> parsedReferencesByMarker = relatedWorkReferenceResolver.parseReferences(linkedFile, databaseContext, filePreferences);

        return createMatchResults(sourceEntry, relatedWorkSnippets, parsedReferencesByMarker, databaseContext);
    }

    /// Insert {comment-username} to bib entry
    ///
    /// @return List of insertion result
    public List<RelatedWorkInsertionResult> insertMatchedRelatedWork(BibEntry sourceEntry,
                                                                     List<RelatedWorkMatchResult> matchResults,
                                                                     String userName) {
        String sourceCitationKey = sourceEntry.getCitationKey().get();

        UserSpecificCommentField userSpecificCommentField = new UserSpecificCommentField(normalizeOwner(userName));
        List<RelatedWorkInsertionResult> insertionResults = new ArrayList<>(matchResults.size());

        for (RelatedWorkMatchResult matchResult : matchResults) {
            if (matchResult.matchedLibraryBibEntry().isEmpty()) {
                insertionResults.add(new RelatedWorkInsertionResult(
                        matchResult,
                        RelatedWorkInsertionStatus.SKIPPED,
                        Optional.empty()
                ));
                continue;
            }

            Optional<FieldChange> fieldChange = appendRelatedWorkComment(
                    sourceCitationKey,
                    matchResult.contextText(),
                    matchResult.matchedLibraryBibEntry().get(),
                    userSpecificCommentField
            );
            insertionResults.add(new RelatedWorkInsertionResult(
                    matchResult,
                    fieldChange.isPresent() ? RelatedWorkInsertionStatus.INSERTED : RelatedWorkInsertionStatus.UNCHANGED,
                    fieldChange
            ));
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

    /// Insert comment-{username} to target bib entry
    /// If comment-{username} not exist, then create a new field; Otherwise append it and separate by an empty line.
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
            return Optional.empty();
        }

        String updatedComment = existingComment
                .filter(comment -> !comment.isBlank())
                .map(comment -> comment.stripTrailing() + OS.NEWLINE + OS.NEWLINE + formattedComment)
                .orElse(formattedComment);

        return matchedLibraryEntry.setField(userSpecificCommentField, updatedComment);
    }

    /// John Doe -> john-doe
    /// Test -> test
    private String normalizeOwner(String userName) {
        return userName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "-");
    }
}
