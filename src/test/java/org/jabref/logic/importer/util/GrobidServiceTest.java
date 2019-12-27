package org.jabref.logic.importer.util;

import org.jabref.preferences.JabRefPreferences;

import org.jabref.testutils.category.FetcherTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest

public class GrobidServiceTest {

    static GrobidService grobidService;

    @BeforeAll
    public static void setup() {
        grobidService = new GrobidService(JabRefPreferences.getInstance());
    }

    @Test
    public void processValidCitationTest() throws GrobidServiceException {
        String response = grobidService.processCitation("Derwing, T. M., Rossiter, M. J., & Munro, " +
                "M. J. (2002). Teaching native speakers to listen to foreign-accented speech. " +
                "Journal of Multilingual and Multicultural Development, 23(4), 245-259.", GrobidService.ConsolidateCitations.WITH_METADATA);
        assertNotNull(response);
        assertTrue(response.charAt(0) == '@');
    }

    @Test
    public void processEmptyStringTest() throws GrobidServiceException {
        String response = grobidService.processCitation(" ", GrobidService.ConsolidateCitations.WITH_METADATA);
        assertNotNull(response);
        assertTrue(response.equals(""));
    }

    @Test
    public void processInvalidCitationTest() throws GrobidServiceException {
        String response = grobidService.processCitation("iiiiiiiiiiiiiiiiiiiiiiii", GrobidService.ConsolidateCitations.WITH_METADATA);
        assertNotNull(response);
        assertTrue(response.equals("@misc{-1,\n" +
                "\n" +
                "}\n"));
    }

}
