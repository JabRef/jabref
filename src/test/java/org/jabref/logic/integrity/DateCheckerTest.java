package org.jabref.logic.integrity;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateCheckerTest {

    private final DateChecker checker = new DateChecker();

    @Test
    void acceptsEmptyInput() {
        assertEquals(Optional.empty(), checker.checkValue(""));
    }

    @Test
    void acceptsValidDates() {
        assertEquals(Optional.empty(), checker.checkValue("2018-04-21"));
        assertEquals(Optional.empty(), checker.checkValue("2018-04"));
        assertEquals(Optional.empty(), checker.checkValue("21-04-2018"));
        assertEquals(Optional.empty(), checker.checkValue("04-2018"));
        assertEquals(Optional.empty(), checker.checkValue("04/18"));
        assertEquals(Optional.empty(), checker.checkValue("04/2018"));
        assertEquals(Optional.empty(), checker.checkValue("April 21, 2018"));
        assertEquals(Optional.empty(), checker.checkValue("April, 2018"));
        assertEquals(Optional.empty(), checker.checkValue("21.04.2018"));
        assertEquals(Optional.empty(), checker.checkValue("2018.04.21"));
        assertEquals(Optional.empty(), checker.checkValue("2018"));
    }

    @Test
    void complainsAboutInvalidIsoLikeDate() {
        assertEquals(Optional.of("incorrect format"), checker.checkValue("2018-04-21TZ"));
    }

    @Test
    void complainsAboutLetterInput() {
        assertEquals(Optional.of("incorrect format"), checker.checkValue("2018-Apr-21"));
        assertEquals(Optional.of("incorrect format"), checker.checkValue("2018-Apr-Twentyone"));
        assertEquals(Optional.of("incorrect format"), checker.checkValue("2018-Apr-Twentyfirst"));
    }

    @Test
    void complainsAboutSpecialCharacterInput() {
        assertEquals(Optional.of("incorrect format"), checker.checkValue("2018_04_21"));
        assertEquals(Optional.of("incorrect format"), checker.checkValue("2018 04 21"));
        assertEquals(Optional.of("incorrect format"), checker.checkValue("2018~04~21"));
    }
}
