package net.sf.jabref.logic.importer.fetcher;

import java.util.Optional;

import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class DiVATest {

    private DiVA fetcher;


    @Before
    public void setUp() {
        fetcher = new DiVA(JabRefPreferences.getInstance().getImportFormatPreferences());
    }

    @Test
    public void testGetName() {
        assertEquals("DiVA", fetcher.getName());
    }

    @Test
    public void testGetHelpPage() {
        assertEquals(HelpFile.FETCHER_DIVA_TO_BIBTEX, HelpFile.FETCHER_DIVA_TO_BIBTEX);
    }

    @Test
    @Ignore("Server currently sends 500")
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
