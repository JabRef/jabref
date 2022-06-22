package org.jabref.logic.integrity;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateCheckerTest {

    private final DateChecker checker = new DateChecker();

    @Test
    void acceptsEmptyInput() {
        assertEquals(Optional.empty(), checker.checkValue(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2018-04-21", "2018-04", "21-04-2018", "04-2018", "04/18", "04/2018", "April 21, 2018", "April, 2018", "21.04.2018", "2018.04.21", "2018"})
    void acceptsValidDates(String s) {
        assertEquals(Optional.empty(), checker.checkValue(s));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2018-04-21TZ", "2018-Apr-21", "2018-Apr-Twentyone", "2018-Apr-Twentyfirst", "2018_04_21", "2018 04 21", "2018~04~21"})
    void complainsAboutInvalidInput(String s) {
        assertEquals(Optional.of("incorrect format"), checker.checkValue(s));
    }
}
