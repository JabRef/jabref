package org.jabref.logic.importer.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fetcher.GrobidCitationFetcher;
import org.jabref.logic.importer.fileformat.PdfGrobidImporterTest;
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
        String expectedResponse =
                "@article{-1,\n" +
                "  author = {Derwing, Tracey and Rossiter, Marian and Munro, Murray},\n" +
                "  title = {Teaching Native Speakers to Listen to Foreign-accented Speech},\n" +
                "  journal = {Journal of Multilingual and Multicultural Development},\n" +
                "  publisher = {Informa UK Limited},\n" +
                "  date = {2002-09},\n" +
                "  year = {2002},\n" +
                "  month = {9},\n" +
                "  pages = {245-259},\n" +
                "  volume = {23},\n" +
                "  number = {4},\n" +
                "  doi = {10.1080/01434630208666468}\n" +
                "}\n";
        String response = grobidService.processCitation("Derwing, T. M., Rossiter, M. J., & Munro, " +
                "M. J. (2002). Teaching native speakers to listen to foreign-accented speech. " +
                "Journal of Multilingual and Multicultural Development, 23(4), 245-259.", GrobidService.ConsolidateCitations.WITH_METADATA);
        assertEquals(expectedResponse, response);
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
        Path file = Path.of(PdfGrobidImporterTest.class.getResource("LNCS-minimal.pdf").toURI());
        List<BibEntry> response = grobidService.processPDF(file, importFormatPreferences);
        assertEquals(1, response.size());
        BibEntry be0 = response.get(0);
        assertEquals(Optional.of("Lastname, Firstname"), be0.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("Paper Title"), be0.getField(StandardField.TITLE));
        assertEquals(Optional.of("2014-10-05"), be0.getField(StandardField.DATE));
    }

}
