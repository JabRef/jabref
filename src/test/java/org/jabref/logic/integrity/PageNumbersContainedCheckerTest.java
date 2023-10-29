package org.jabref.logic.integrity;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class PageNumbersContainedCheckerTest {

    private final PageNumbersContainedChecker checker = new PageNumbersContainedChecker();

    @Test
    void booktitleDoesNotAcceptIfPageNumbersContainedAsPg() {
        assertNotEquals(Optional.empty(), checker.checkValue("Service-Oriented Computing, Fifth International Conference, Vienna, Austria, Proceedings, Pg. 22-23"));
    }

    @Test
    void booktitleDoesNotAcceptIfPageNumbersContainedAsPgWithoutDash() {
        assertNotEquals(Optional.empty(), checker.checkValue("Service-Oriented Computing, Fifth International Conference, Vienna, Austria, Proceedings, Pp. 22"));
    }

    @Test
    void booktitleDoesNotAcceptIfPageNumbersContainedAsPage() {
        assertNotEquals(Optional.empty(), checker.checkValue("Service-Oriented Computing, Fifth International Conference, Vienna, Austria, Proceedings, Page. 22-23"));
    }

    @Test
    void booktitleDoesNotAcceptIfPageNumbersContainedAsPageWithoutDash() {
        assertNotEquals(Optional.empty(), checker.checkValue("Service-Oriented Computing, Fifth International Conference, Vienna, Austria, Proceedings, Pp. 22"));
    }

    @Test
    void booktitleDoesNotAcceptIfPageNumbersContainedAsPp() {
        assertNotEquals(Optional.empty(), checker.checkValue("Service-Oriented Computing, Fifth International Conference, Vienna, Austria, Proceedings, Pp. 22-23"));
    }

    @Test
    void booktitleDoesNotAcceptIfPageNumbersContainedAsPPWithoutDash() {
        assertNotEquals(Optional.empty(), checker.checkValue("Service-Oriented Computing, Fifth International Conference, Vienna, Austria, Proceedings, Pp. 22"));
    }

    @Test
    void booktitleDoesAcceptIfPageNumbersNotContained() {
        assertEquals(Optional.empty(), checker.checkValue("Service-Oriented Computing, Fifth International Conference, Vienna, Austria, Proceedings"));
    }
}
