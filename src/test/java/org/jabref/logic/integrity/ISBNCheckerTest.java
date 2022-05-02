package org.jabref.logic.integrity;

import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.l10n.Localization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

    @ParameterizedTest
    @MethodSource("provideBoundaryArgumentsForISBN13")
    public void checkISBNValue(Optional optValue, String id) {
        assertEquals(optValue, checker.checkValue(id));
    }

    private static Stream<Arguments> provideBoundaryArgumentsForISBN13() {
        return Stream.of(
                Arguments.of(Optional.empty(), "978-0-306-40615-7"),
                Arguments.of(Optional.of(Localization.lang("incorrect control digit")), "978-0-306-40615-2"),
                Arguments.of(Optional.of(Localization.lang("incorrect format")), "978_0_306_40615_7")
        );
    }
}
