package org.jabref.logic.integrity;

import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

public class ASCIICharacterCheckerTest {

    @Test
    void fieldAcceptsASCIICharacters() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.TITLE, "Only ascii characters!'@12"));
    }

    @Test
    void fieldDoesNotAcceptUmlauts() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.MONTH, "Umlauts are nöt ällowed"));
    }

    @Test
    void fieldDoesNotAcceptUnicode() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.AUTHOR, "Some unicode ⊕"));
    }

}
