package org.jabref.logic.importer.util;

import java.io.IOException;

import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest
public class GrobidServiceTest {

    private static GrobidService grobidService;

    @BeforeAll
    public static void setup() {
        grobidService = new GrobidService("http://grobid.cm.in.tum.de:8070");
    }

    @Test
    public void processValidCitationTest() throws IOException {
        String response = grobidService.processCitation("Derwing, T. M., Rossiter, M. J., & Munro, " +
                "M. J. (2002). Teaching native speakers to listen to foreign-accented speech. " +
                "Journal of Multilingual and Multicultural Development, 23(4), 245-259.", GrobidService.ConsolidateCitations.WITH_METADATA);
        String[] responseRows = response.split("\n");
        assertNotNull(response);
        assertEquals('@', response.charAt(0));
        assertTrue(responseRows[1].contains("author") && responseRows[1].contains( "Derwing and M Rossiter"));
        assertTrue(responseRows[2].contains("title") && responseRows[2].contains( "Teaching native speakers"));
        assertTrue(responseRows[3].contains("journal") && responseRows[3].contains( "Journal of Multilingual and Multicultural"));
        assertTrue(responseRows[4].contains("year") && responseRows[4].contains( "2002"));
        assertTrue(responseRows[5].contains("pages") && responseRows[5].contains( "245--259"));
        assertTrue(responseRows[6].contains("volume") && responseRows[6].contains( "23"));
        assertTrue(responseRows[7].contains("number") && responseRows[7].contains( "4"));
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

}
