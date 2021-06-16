package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@FetcherTest
public class DiVATest {

    private DiVA fetcher;

    @BeforeEach
    public void setUp() {
        fetcher = new DiVA(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
    }

    @Test
    public void testGetName() {
        assertEquals("DiVA", fetcher.getName());
    }

    @Test
    public void testPerformSearchById() throws Exception {
        BibEntry entry = new BibEntry();
        entry.setType(StandardEntryType.Article);
        entry.setCitationKey("Gustafsson260746");
        entry.setField(StandardField.AUTHOR, "Gustafsson, Oscar");
        entry.setField(StandardField.INSTITUTION, "Linköping University, The Institute of Technology");
        entry.setField(StandardField.JOURNAL,
                "IEEE transactions on circuits and systems. 2, Analog and digital signal processing (Print)");
        entry.setField(StandardField.NUMBER, "11");
        entry.setField(StandardField.PAGES, "974--978");
        entry.setField(StandardField.TITLE, "Lower bounds for constant multiplication problems");
        entry.setField(StandardField.VOLUME, "54");
        entry.setField(StandardField.YEAR, "2007");
        entry.setField(StandardField.ABSTRACT, "Lower bounds for problems related to realizing multiplication by constants with shifts, adders, and subtracters are presented. These lower bounds are straightforwardly calculated and have applications in proving the optimality of solutions obtained by heuristics. ");
        entry.setField(StandardField.DOI, "10.1109/TCSII.2007.903212");

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

    @Test
    public void testEmptyId() throws Exception {
        assertEquals(Optional.empty(), fetcher.performSearchById(""));
    }
}
