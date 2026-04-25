package org.jabref.logic.relatedwork;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class RelatedWorkMatcherTest {

    @Test
    void matchRelatedWorkReturnsContextTextMappedToParsedReferences() throws IOException, URISyntaxException {
        LinkedFile linkedFile = createLinkedFile();
        RelatedWorkMatcher matcher = createMatcher();

        List<RelatedWorkMatchResult> matchResults = matcher.matchRelatedWork(
                new BibDatabaseContext(),
                new BibEntry(StandardEntryType.Article),
                linkedFile,
                "They are facing several challenges in this transformation described along the BAPO framework [80].",
                createFilePreferences()
        );

        RelatedWorkMatchResult matchedResult = matchResults.getFirst();

        assertEquals(1, matchResults.size());
        assertEquals("They are facing several challenges in this transformation described along the BAPO framework.", matchedResult.contextText());
        assertEquals("[80]", matchedResult.citationKey());
        assertEquals("[80] Frank van der Linden, Klaus Schmid, and Eelco Rommes. 2007. Software product lines in action - the best industrial practice in product line engineering. Springer.",
                matchedResult.parsedReference().orElseThrow().getField(StandardField.COMMENT).orElseThrow());
        assertFalse(matchedResult.hasMatchedLibraryEntry());
    }

    @Test
    void matchRelatedWorkReturnsExistingLibraryEntryWhenDuplicateIsFound() throws IOException, URISyntaxException {
        LinkedFile linkedFile = createLinkedFile();
        RelatedWorkReferenceResolver resolver = new RelatedWorkReferenceResolver();
        RelatedWorkMatcher matcher = new RelatedWorkMatcher(
                new RelatedWorkTextParser(),
                resolver,
                new DuplicateCheck(new BibEntryTypesManager())
        );
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        Map<String, BibEntry> referencesByMarker = resolver.parseReferences(linkedFile, new BibDatabaseContext(), createFilePreferences());
        BibEntry existingLibraryEntry = Optional.ofNullable(referencesByMarker.get("[80]")).orElseThrow();
        databaseContext.getDatabase().insertEntry(existingLibraryEntry);

        List<RelatedWorkMatchResult> matchedResults = matcher.matchRelatedWork(
                databaseContext,
                new BibEntry(StandardEntryType.Article),
                linkedFile,
                "They are facing several challenges in this transformation described along the BAPO framework [80].",
                createFilePreferences()
        );

        RelatedWorkMatchResult matchedResult = matchedResults.getFirst();

        assertSame(existingLibraryEntry, matchedResult.matchedLibraryBibEntry().orElseThrow());
    }

    @Test
    void matchRelatedWorkReturnsEmptyWhenTextCannotBeParsed() throws IOException, URISyntaxException {
        LinkedFile linkedFile = createLinkedFile();
        RelatedWorkMatcher matcher = createMatcher();

        List<RelatedWorkMatchResult> matchResults = matcher.matchRelatedWork(
                new BibDatabaseContext(),
                new BibEntry(StandardEntryType.Article),
                linkedFile,
                "This text has no citation markers.",
                createFilePreferences()
        );

        assertTrue(matchResults.isEmpty());
    }

    @Test
    void matchRelatedWorkMatchesEquivalentPagesNotation() throws IOException {
        BibEntry parsedReference = new BibEntry(StandardEntryType.Article)
                .withCitationKey("1")
                .withField(StandardField.AUTHOR, "J. Smith and A. Brown")
                .withField(StandardField.TITLE, "Sustainable agriculture methods for tropical regions")
                .withField(StandardField.JOURNAL, "Journal of Agricultural Science")
                .withField(StandardField.YEAR, "2021")
                .withField(StandardField.VOLUME, "45")
                .withField(StandardField.NUMBER, "3")
                .withField(StandardField.PAGES, "123-145");
        RelatedWorkReferenceResolver resolver = new RelatedWorkReferenceResolver() {
            @Override
            public Map<String, BibEntry> parseReferences(LinkedFile linkedFile,
                                                         BibDatabaseContext databaseContext,
                                                         FilePreferences filePreferences) {
                return Map.of("[1]", parsedReference);
            }
        };
        RelatedWorkMatcher matcher = new RelatedWorkMatcher(
                new RelatedWorkTextParser(),
                resolver,
                new DuplicateCheck(new BibEntryTypesManager())
        );
        BibEntry existingLibraryEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Smith, John and Brown, Alice")
                .withField(StandardField.TITLE, "Sustainable Agriculture Methods for Tropical Regions")
                .withField(StandardField.JOURNAL, "Journal of Agricultural Science")
                .withField(StandardField.YEAR, "2021")
                .withField(StandardField.VOLUME, "45")
                .withField(StandardField.NUMBER, "3")
                .withField(StandardField.PAGES, "123--145");
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        databaseContext.getDatabase().insertEntry(existingLibraryEntry);

        List<RelatedWorkMatchResult> matchResults = matcher.matchRelatedWork(
                databaseContext,
                new BibEntry(StandardEntryType.Article),
                new LinkedFile("", "main.pdf", "PDF"),
                "A short related work sentence [1].",
                createFilePreferences()
        );

        RelatedWorkMatchResult matchResult = matchResults.getFirst();

        assertEquals(1, matchResults.size());
        assertEquals("A short related work sentence.", matchResult.contextText());
        assertEquals("[1]", matchResult.citationKey());
        assertEquals("123-145", matchResult.parsedReference().orElseThrow().getField(StandardField.PAGES).orElseThrow());
        assertSame(existingLibraryEntry, matchResult.matchedLibraryBibEntry().orElseThrow());
    }

    private FilePreferences createFilePreferences() {
        return mock(FilePreferences.class);
    }

    private RelatedWorkMatcher createMatcher() {
        return new RelatedWorkMatcher(
                new RelatedWorkTextParser(),
                new RelatedWorkReferenceResolver(),
                new DuplicateCheck(new BibEntryTypesManager())
        );
    }

    private LinkedFile createLinkedFile() throws URISyntaxException {
        Path pdfPath = Path.of(RelatedWorkMatcherTest.class
                .getResource("/org/jabref/logic/importer/fileformat/pdf/2024_SPLC_Becker.pdf")
                .toURI());
        return new LinkedFile("", pdfPath, "PDF");
    }
}
