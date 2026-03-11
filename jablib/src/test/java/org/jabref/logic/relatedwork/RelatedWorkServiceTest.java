package org.jabref.logic.relatedwork;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.os.OS;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatedWorkServiceTest {

    @Test
    void matchRelatedWorkReturnsContextTextMappedToParsedReferences() throws IOException, URISyntaxException {
        LinkedFile linkedFile = createLinkedFile();
        RelatedWorkService service = createService();

        List<RelatedWorkMatchResult> matchResults = service.matchRelatedWork(
                new BibEntry(StandardEntryType.Article),
                "They are facing several challenges in this transformation described along the BAPO framework [80].",
                linkedFile,
                new BibDatabaseContext(),
                createFilePreferences()
        );

        RelatedWorkMatchResult matchedResult = matchResults.getFirst();

        assertEquals(1, matchResults.size());
        assertEquals("They are facing several challenges in this transformation described along the BAPO framework.", matchedResult.contextText());
        assertEquals("[80]", matchedResult.citationKey());
        assertEquals("[80] Frank van der Linden, Klaus Schmid, and Eelco Rommes. 2007. Software product lines in action - the best industrial practice in product line engineering. Springer.",
                matchedResult.parsedReference().getField(StandardField.COMMENT).orElseThrow());
        assertFalse(matchedResult.hasMatchedLibraryEntry());
    }

    @Test
    void matchRelatedWorkReturnsExistingLibraryEntryWhenDuplicateIsFound() throws IOException, URISyntaxException {
        LinkedFile linkedFile = createLinkedFile();
        RelatedWorkReferenceResolver resolver = new RelatedWorkReferenceResolver();
        RelatedWorkService service = new RelatedWorkService(
                new RelatedWorkTextParser(),
                resolver,
                new DuplicateCheck(new BibEntryTypesManager())
        );
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        Map<String, BibEntry> referencesByMarker = resolver.parseReferences(linkedFile, new BibDatabaseContext(), createFilePreferences());
        BibEntry existingLibraryEntry = referencesByMarker.get("[80]");
        databaseContext.getDatabase().insertEntry(existingLibraryEntry);

        List<RelatedWorkMatchResult> matchedResults = service.matchRelatedWork(
                new BibEntry(StandardEntryType.Article),
                "They are facing several challenges in this transformation described along the BAPO framework [80].",
                linkedFile,
                databaseContext,
                createFilePreferences()
        );

        RelatedWorkMatchResult matchedResult = matchedResults.getFirst();

        assertSame(existingLibraryEntry, matchedResult.matchedLibraryBibEntry().orElseThrow());
    }

    @Test
    void matchRelatedWorkReturnsEmptyWhenTextCannotBeParsed() throws IOException, URISyntaxException {
        LinkedFile linkedFile = createLinkedFile();
        RelatedWorkService service = createService();

        List<RelatedWorkMatchResult> matchResults = service.matchRelatedWork(
                new BibEntry(StandardEntryType.Article),
                "This text has no citation markers.",
                linkedFile,
                new BibDatabaseContext(),
                createFilePreferences()
        );

        assertTrue(matchResults.isEmpty());
    }

    @Test
    void insertMatchedRelatedWorkWritesUserSpecificCommentField() {
        RelatedWorkService service = createService();
        BibEntry sourceEntry = new BibEntry(StandardEntryType.Article).withCitationKey("LunaOstos_2024");
        BibEntry matchedLibraryEntry = new BibEntry(StandardEntryType.Misc).withCitationKey("Agency2021");
        RelatedWorkMatchResult matchResult = new RelatedWorkMatchResult(
                "Colombia is a middle-income country with a population of approximately 50 million.",
                "[1]",
                new BibEntry(StandardEntryType.Misc),
                Optional.of(matchedLibraryEntry)
        );

        List<RelatedWorkInsertionResult> insertionResults = service.insertMatchedRelatedWork(sourceEntry, List.of(matchResult), "koppor");
        RelatedWorkInsertionResult InsertionResult = insertionResults.getFirst();

        assertEquals(1, insertionResults.size());
        assertEquals(RelatedWorkInsertionStatus.INSERTED, InsertionResult.status());
        assertFalse(InsertionResult.fieldChange().isEmpty());
        assertEquals("[LunaOstos_2024]: Colombia is a middle-income country with a population of approximately 50 million.",
                matchedLibraryEntry.getField(new UserSpecificCommentField("koppor")).orElseThrow());
    }

    @Test
    void insertMatchedRelatedWorkAppendsToExistingUserSpecificCommentField() {
        RelatedWorkService service = createService();
        BibEntry sourceEntry = new BibEntry(StandardEntryType.Article).withCitationKey("LunaOstos_2024");
        UserSpecificCommentField userSpecificCommentField = new UserSpecificCommentField("koppor");
        BibEntry matchedLibraryEntry = new BibEntry(StandardEntryType.Misc)
                .withCitationKey("Agency2021")
                .withField(userSpecificCommentField, "[test]: blahblah");
        RelatedWorkMatchResult matchResult = new RelatedWorkMatchResult(
                "Colombia is a middle-income country with a population of approximately 50 million.",
                "[1]",
                new BibEntry(StandardEntryType.Misc),
                Optional.of(matchedLibraryEntry)
        );

        List<RelatedWorkInsertionResult> insertionResults = service.insertMatchedRelatedWork(sourceEntry, List.of(matchResult), "koppor");

        assertEquals(RelatedWorkInsertionStatus.INSERTED, insertionResults.getFirst().status());
        assertEquals("[test]: blahblah" + OS.NEWLINE + OS.NEWLINE + "[LunaOstos_2024]: Colombia is a middle-income country with a population of approximately 50 million.",
                matchedLibraryEntry.getField(userSpecificCommentField).orElseThrow());
    }

    @Test
    void insertMatchedRelatedWorkReturnsSkippedResultWhenReferenceIsNotMatchedToLibraryEntry() {
        RelatedWorkService service = createService();
        BibEntry sourceEntry = new BibEntry(StandardEntryType.Article).withCitationKey("LunaOstos_2024");
        RelatedWorkMatchResult matchResult = new RelatedWorkMatchResult(
                "Colombia is a middle-income country with a population of approximately 50 million.",
                "[1]",
                new BibEntry(StandardEntryType.Misc).withCitationKey("Agency2021"),
                Optional.empty()
        );

        List<RelatedWorkInsertionResult> insertionResults = service.insertMatchedRelatedWork(sourceEntry, List.of(matchResult), "koppor");

        assertEquals(1, insertionResults.size());
        assertEquals(RelatedWorkInsertionStatus.SKIPPED, insertionResults.getFirst().status());
        assertTrue(insertionResults.getFirst().fieldChange().isEmpty());
    }

    private FilePreferences createFilePreferences() {
        return new FilePreferences(
                "",
                "",
                false,
                false,
                FilePreferences.DEFAULT_FILENAME_PATTERNS[0],
                "",
                false,
                false,
                Path.of(""),
                false,
                Path.of(""),
                false,
                false,
                false,
                false,
                false,
                false,
                Path.of(""),
                false,
                false
        );
    }

    private RelatedWorkService createService() {
        return new RelatedWorkService(
                new RelatedWorkTextParser(),
                new RelatedWorkReferenceResolver(),
                new DuplicateCheck(new BibEntryTypesManager())
        );
    }

    private LinkedFile createLinkedFile() throws URISyntaxException {
        Path pdfPath = Path.of(RelatedWorkServiceTest.class
                .getResource("/org/jabref/logic/importer/fileformat/pdf/2024_SPLC_Becker.pdf")
                .toURI());
        return new LinkedFile("", pdfPath, "PDF");
    }
}
