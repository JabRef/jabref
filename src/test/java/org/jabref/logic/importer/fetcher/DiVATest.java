package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.testutils.category.FetcherTests;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Answers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@Category(FetcherTests.class)
public class DiVATest {

    private DiVA fetcher;

    @Before
    public void setUp() {
        fetcher = new DiVA(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
    }

    @Test
    public void testGetName() {
        assertEquals("DiVA", fetcher.getName());
    }

    @Test
    public void testGetHelpPage() {
        assertEquals(HelpFile.FETCHER_DIVA, HelpFile.FETCHER_DIVA);
    }

    @Test
    public void testPerformSearchById() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setType("article");
        entry.setField("author", "Gustafsson, Oscar");
        entry.setField("institution", "Linköping University, The Institute of Technology");
        entry.setCiteKey("Gustafsson260746");
        entry.setField("journal",
                "IEEE transactions on circuits and systems. 2, Analog and digital signal processing (Print)");
        entry.setField("number", "11");
        entry.setField("pages", "974--978");
        entry.setField("title", "Lower bounds for constant multiplication problems");
        entry.setField("volume", "54");
        entry.setField("year", "2007");

        assertEquals(Optional.of(entry), fetcher.performSearchById("diva2:260746"));
    }

    @Test
    public void testValidIdentifier() {
        assertTrue(fetcher.isValidId("diva2:260746"));
    }

    @Test
    public void testInvalidIdentifier() {
        assertFalse(fetcher.isValidId("banana"));
    }
}
