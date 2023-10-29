package org.jabref.logic.integrity;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class AuthorsContainedCheckerTest {

    private final AuthorsContainedChecker checker = new AuthorsContainedChecker();

    @Test
    void booktitleDoesNotAcceptIfItContainsAuthor() {
        assertNotEquals(Optional.empty(), checker.checkValue("Service-Oriented Computing - {ICSOC}, Fifth International Conference, Vienna, Austria, Proceedings"));
    }

    @Test
    void booktitleDoesAcceptIfItContainsAuthor() {
        assertEquals(Optional.empty(), checker.checkValue("Service-Oriented Computing, Fifth International Conference, Vienna, Austria, Proceedings"));
    }
}
