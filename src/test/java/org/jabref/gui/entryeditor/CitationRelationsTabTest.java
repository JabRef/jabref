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
    BibEntry b1;
    BibEntry b2;
    ArrayList<BibEntry> bList;

    @BeforeEach
    void setup() {
        b1 = new BibEntry();
        b2 = new BibEntry();
        bList = new ArrayList<>();

        b2.setField(StandardField.DOI, "12.3456/1");
        b2.setField(StandardField.CITING, "test");
        b2.setField(StandardField.CITEDBY, "test");
        bList.add(b1);
        bList.add(b2);
    }

    @Test
    void serializeNormalCaseTest() {
        assertEquals("12.3456/1", serialize(bList));
    }

    @Test
    void serializeCornerCaseTest() {
        bList.remove(b2);
        assertEquals("", serialize(bList));
    }

    @Test
    void citingFieldExists() {
        assertTrue(b2.hasField(StandardField.CITING));
    }

    @Test
    void citedByFieldExists() {
        assertTrue(b2.hasField(StandardField.CITEDBY));
    }
}
