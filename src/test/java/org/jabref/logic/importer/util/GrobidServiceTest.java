package org.jabref.logic.importer.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.PdfGrobidImporterTest;
import org.jabref.logic.importer.importsettings.ImportSettingsPreferences;
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
    private static ImportSettingsPreferences importSettingsPreferences = new ImportSettingsPreferences(false, true, "http://grobid.jabref.org:8070");
    private static ImportFormatPreferences importFormatPreferences;

    @BeforeAll
    public static void setup() {
        grobidService = new GrobidService(importSettingsPreferences);
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
        assertTrue(responseRows[1].contains("author") && responseRows[1].contains("Derwing, Tracey and Rossiter, Marian and Munro, Murray"));
        assertTrue(responseRows[2].contains("title") && responseRows[2].contains("Teaching Native Speakers to Listen to Foreign-accented Speech"));
        assertTrue(responseRows[3].contains("journal") && responseRows[3].contains("Journal of Multilingual and Multicultural"));
        assertTrue(responseRows[4].contains("publisher") && responseRows[4].contains("Informa UK Limited"));
        assertTrue(responseRows[5].contains("date") && responseRows[5].contains("2002-09"));
        assertTrue(responseRows[6].contains("year") && responseRows[6].contains("2002"));
        assertTrue(responseRows[7].contains("month") && responseRows[7].contains("9"));
        assertTrue(responseRows[8].contains("pages") && responseRows[8].contains("245-259"));
        assertTrue(responseRows[9].contains("volume") && responseRows[9].contains("23"));
        assertTrue(responseRows[10].contains("number") && responseRows[10].contains("4"));
        assertTrue(responseRows[11].contains("doi") && responseRows[11].contains("10.1080/01434630208666468"));
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
    public void failsWhenGrobidDisabled() {
        assertThrows(UnsupportedOperationException.class, () -> new GrobidService(new ImportSettingsPreferences(false, false, "http://grobid.jabref.org:8070")));
    }

    @Test
    public void processPdfTest() throws IOException, ParseException, URISyntaxException {
        Path file = Path.of(PdfGrobidImporterTest.class.getResource("LNCS-minimal.pdf").toURI());
        List<BibEntry> response = grobidService.processPDF(file, importFormatPreferences);
        assertEquals(1, response.size());
        BibEntry be0 = response.get(0);
        assertEquals(Optional.of("Lastname, Firstname"), be0.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("Paper Title"), be0.getField(StandardField.TITLE));
        assertEquals(Optional.of("2014-10-05"), be0.getField(StandardField.DATE));
    }

}
