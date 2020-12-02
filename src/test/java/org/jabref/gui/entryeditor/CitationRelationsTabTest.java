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

    /**
     * Testziel: CitationRelationsTabTest.serialize() auf Funktion testen.
     * Input: Eine Liste von BibEntries
     * Output: Kommagetrennter String der DOI's der BibEntries
     * Hier wird eine Liste von 2 Entrie, davon einem mit DOI übergeben
     */
    @Test
    void serializeNormalCaseTest() {
        assertEquals("12.3456/1", serialize(bList));
    }

    /**
     * Testziel: CitationRelationsTabTest.serialize() auf Funktion testen.
     * Input: Eine Liste von BibEntries
     * Output: Kommagetrennter String der DOI's der BibEntries
     * Hier wird die Liste auf ein Element reduziert welches keine DOI besitzt
     */
    @Test
    void serializeCornerCaseTest() {
        bList.remove(b2);
        assertEquals("", serialize(bList));
    }

    /**
     * Testziel: Testet ob das StandartField CITING existiert
     * Ein Entry wird auf die Existenz des CITING Field geprüft,
     * dieses sollte vorher im setup gesetzt worden sein
     */
    @Test
    void citingFieldExists() {
        assertTrue(b2.hasField(StandardField.CITING));
    }

    /**
     * Testziel: Testet ob das StandartField CITEDBY existiert
     * Ein Entry wird auf die Existenz des CITEDBY Field geprüft,
     * dieses sollte vorher im setup gesetzt worden sein
     */
    @Test
    void citedByFieldExists() {
        assertTrue(b2.hasField(StandardField.CITEDBY));
    }
}
