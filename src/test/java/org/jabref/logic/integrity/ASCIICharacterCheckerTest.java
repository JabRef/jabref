package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ASCIICharacterCheckerTest {

    private final ASCIICharacterChecker checker = new ASCIICharacterChecker();
    private final BibEntry entry = new BibEntry();

    @Test
    void fieldAcceptsAsciiCharacters() {
        entry.setField(StandardField.TITLE, "Only ascii characters!'@12");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void fieldDoesNotAcceptUmlauts() {
        entry.setField(StandardField.MONTH, "Umlauts are nöt ällowed");
        assertEquals(List.of(new IntegrityMessage("Non-ASCII encoded character found", entry, StandardField.MONTH)), checker.check(entry));
    }

    @Test
    void fieldDoesNotAcceptUnicode() {
        entry.setField(StandardField.AUTHOR, "Some unicode ⊕");
        assertEquals(List.of(new IntegrityMessage("Non-ASCII encoded character found", entry, StandardField.AUTHOR)), checker.check(entry));
    }

}
