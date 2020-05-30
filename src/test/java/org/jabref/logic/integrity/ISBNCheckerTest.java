package org.jabref.logic.integrity;

import java.util.Optional;

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

}
