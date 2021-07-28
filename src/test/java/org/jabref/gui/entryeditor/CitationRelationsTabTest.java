package org.jabref.gui.entryeditor;

import java.util.ArrayList;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.jabref.gui.entryeditor.CitationRelationsTab.serialize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CitationRelationsTabTest {
    BibEntry bibEntry;
    ArrayList<BibEntry> bList;

    @BeforeEach
    void setup() {
        bibEntry = new BibEntry().withField(StandardField.DOI, "10.1007/s11616-005-0142-4")
                                 .withField(StandardField.CITING, "test")
                                 .withField(StandardField.CITED_BY, "test");
        bList = new ArrayList<>();
        bList.add(new BibEntry());
        bList.add(bibEntry);
    }

    /**
     * Testgoal: Test functionality CitationRelationsTabTest.serialize().
     * Input: A list of BibEntries
     * Output: A Comma-Seperated String, that contains the DOI's of the Articles if present.
     * In this case a List of two BibEntries, one with a DOI, is given to the serialize()-Function
     */
    @Test
    void serializeNormalCaseTest() {
        assertEquals("10.1007/s11616-005-0142-4", serialize(bList));
    }

    /**
     * Testgoal: Test functionality CitationRelationsTabTest.serialize().
     * Input: A list of BibEntries
     * Output: A Comma-Seperated String, that contains the DOI's of the Articles if present.
     * In this case a List of one BibEntry, with no DOI is given to the serialize()-Function
     */
    @Test
    void serializeCornerCaseTest() {
        bList.remove(bibEntry);
        assertEquals("", serialize(bList));
    }

    /**
     * Testgoal: Test whether StandardField.CITING exists when set.
     * Here, one BibEntry is tested. Its CITING Field has been set in the setup()
     * function of the Test.
     */
    @Test
    void citingFieldExists() {
        assertTrue(bibEntry.hasField(StandardField.CITING));
    }

    /**
     * Testgoal: Test whether StandardField.CITEDBY exists when set.
     * Here, one BibEntry is tested. Its CITEDBY Field has been set in the setup()
     * function of the Test.
     */
    @Test
    void citedByFieldExists() {
        assertTrue(bibEntry.hasField(StandardField.CITED_BY));
    }
}
