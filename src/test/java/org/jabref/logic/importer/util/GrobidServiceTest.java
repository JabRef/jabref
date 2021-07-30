package org.jabref.logic.importer.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fetcher.GrobidCitationFetcher;
import org.jabref.logic.importer.fileformat.GrobidPdfMetadataImporterTest;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
public class GrobidServiceTest {

    private static GrobidService grobidService;
    private static ImportFormatPreferences importFormatPreferences;

    @BeforeAll
    public static void setup() {
        grobidService = new GrobidService(GrobidCitationFetcher.GROBID_URL);
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.getKeywordSeparator()).thenReturn(',');
    }

    @Test
    public void processValidCitationTest() throws IOException {
        String response = grobidService.processCitation("Derwing, T. M., Rossiter, M. J., & Munro, " +
                "M. J. (2002). Teaching native speakers to listen to foreign-accented speech. " +
                "Journal of Multilingual and Multicultural Development, 23(4), 245-259.", GrobidService.ConsolidateCitations.WITH_METADATA);
        String[] responseRows = response.split("\n");
        assertNotNull(response);
        assertEquals('@', response.charAt(0));
        assertTrue(responseRows[1].contains("author") && responseRows[1].contains("Derwing, T and Rossiter, M"));
        assertTrue(responseRows[2].contains("title") && responseRows[2].contains("Teaching native speakers"));
        assertTrue(responseRows[3].contains("journal") && responseRows[3].contains("Journal of Multilingual and Multicultural"));
        assertTrue(responseRows[4].contains("date") && responseRows[4].contains("2002"));
        assertTrue(responseRows[5].contains("pages") && responseRows[5].contains("245--259"));
        assertTrue(responseRows[6].contains("volume") && responseRows[6].contains("23"));
        assertTrue(responseRows[7].contains("number") && responseRows[7].contains("4"));
    }

    @Test
    public void processEmptyStringTest() throws IOException {
        String response = grobidService.processCitation(" ", GrobidService.ConsolidateCitations.WITH_METADATA);
        assertNotNull(response);
        assertEquals("", response);
    }

    @Test
    public void processInvalidCitationTest() {
        assertThrows(IOException.class, () -> grobidService.processCitation("iiiiiiiiiiiiiiiiiiiiiiii", GrobidService.ConsolidateCitations.WITH_METADATA));
    }

    @Test
    public void processPdfTest() throws IOException, ParseException, URISyntaxException {
        Path file = Path.of(GrobidPdfMetadataImporterTest.class.getResource("LNCS-minimal.pdf").toURI());
        List<BibEntry> response = grobidService.processPDF(file, importFormatPreferences);
        assertEquals(1, response.size());
        BibEntry be0 = response.get(0);
        assertEquals(Optional.of("Lastname, Firstname"), be0.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("Paper Title"), be0.getField(StandardField.TITLE));
        assertEquals(Optional.of("2014-10-5"), be0.getField(StandardField.DATE));
    }

}
