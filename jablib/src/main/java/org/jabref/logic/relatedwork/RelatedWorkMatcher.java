package org.jabref.logic.relatedwork;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class RelatedWorkMatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedWorkMatcher.class);

    private final RelatedWorkTextParser relatedWorkTextParser;
    private final RelatedWorkReferenceResolver relatedWorkReferenceResolver;
    private final DuplicateCheck duplicateCheck;

    public RelatedWorkMatcher(RelatedWorkTextParser relatedWorkTextParser,
                              RelatedWorkReferenceResolver relatedWorkReferenceResolver,
                              DuplicateCheck duplicateCheck) {
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
}
