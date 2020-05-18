package org.jabref.logic.integrity;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateCheckerTest {

    private final DateChecker checker = new DateChecker();

    @Test
    void complainsAboutInvalidIsoLikeDate() {
        assertEquals(Optional.of("incorrect format"), checker.checkValue("2018-04-21TZ"));
    }

    @Test
    void acceptsValidIsoDate() {
        assertEquals(Optional.empty(), checker.checkValue("2018-04-21"));
    }
}
