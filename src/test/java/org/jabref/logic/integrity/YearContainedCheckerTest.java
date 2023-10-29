package org.jabref.logic.integrity;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class YearContainedCheckerTest {

    private final YearContainedChecker checker = new YearContainedChecker();

    @Test
    void booktitleDoesNotAcceptIfYearIsContainedAfterAuthor() {
        assertNotEquals(Optional.empty(), checker.checkValue("European Conference on Circuit Theory and Design, {ECCTD} 2015, Trondheim, Norway"));
    }

    @Test
    void booktitleDoesNotAcceptIfYearIsContainedBeforeAuthor() {
        assertNotEquals(Optional.empty(), checker.checkValue("European Conference on Circuit Theory and Design, 2015 {ECCTD}, Trondheim, Norway"));
    }

    @Test
    void booktitleDoesAcceptIfYearIsNotContained() {
        assertEquals(Optional.empty(), checker.checkValue("European Conference on Circuit Theory and Design, {ECCTD}, Trondheim, Norway"));
    }
}
