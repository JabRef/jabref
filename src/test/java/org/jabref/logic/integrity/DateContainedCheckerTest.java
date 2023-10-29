package org.jabref.logic.integrity;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class DateContainedCheckerTest {

    private final DateContainedChecker checker = new DateContainedChecker();

    @Test
    void booktitleDoesNotAcceptIfDateIsContainedWithDash() {
        assertNotEquals(Optional.empty(), checker.checkValue("European Conference on Circuit Theory and Design, {ECCTD} 2015, Trondheim, Norway, August 24-26, 2015"));
    }

    @Test
    void booktitleDoesNotAcceptIfDateIsContainedWithoutDash() {
        assertNotEquals(Optional.empty(), checker.checkValue("European Conference on Circuit Theory and Design, {ECCTD} 2015, Trondheim, Norway, August 24, 2015"));
    }

    @Test
    void booktitleDoesAcceptIfDateIsNotContained() {
        assertEquals(Optional.empty(), checker.checkValue("Service-Oriented Computing, Fifth International Conference, Vienna, Austria, Proceedings"));
    }
}
