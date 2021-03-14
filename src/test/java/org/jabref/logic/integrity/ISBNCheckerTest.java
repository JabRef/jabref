package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.logic.l10n.Localization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ISBNCheckerTest {

    private final ISBNChecker checker = new ISBNChecker();

    @Test
    void isbnAcceptsValidInput() {
        assertEquals(Optional.empty(), checker.checkValue("0-201-53082-1"));
    }

    @Test
    void isbnAcceptsNumbersAndCharacters() {
        assertEquals(Optional.empty(), checker.checkValue("0-9752298-0-X"));
    }

    @Test
    void isbnDoesNotAcceptRandomInput() {
        assertNotEquals(Optional.empty(), checker.checkValue("Some other stuff"));
    }

    @Test
    void isbnDoesNotAcceptInvalidInput() {
        assertNotEquals(Optional.empty(), checker.checkValue("0-201-53082-2"));
    }

    @Test
    void isbnAcceptsCorrectControlDigitForIsbn13() {
        assertEquals(Optional.empty(), checker.checkValue("978-0-306-40615-7"));
    }

    @Test
    void isbnDoesNotAcceptIncorrectControlDigitForIsbn13() {
        assertEquals(Optional.of(Localization.lang("incorrect control digit")), checker.checkValue("978-0-306-40615-2"));
    }

    @Test
    void isbnDoesNotAcceptInvalidFormatForIsbn13() {
        assertEquals(Optional.of(Localization.lang("incorrect format")), checker.checkValue("978_0_306_40615_7"));
    }
}
