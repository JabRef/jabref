package org.jabref.gui.autocompleter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppendPersonNamesStrategyTest {

    @Test
    void withoutParam() {
        AppendPersonNamesStrategy strategy = new AppendPersonNamesStrategy();
        assertEquals(" and ", strategy.getDelimiter());
    }

    @ParameterizedTest(name = "separationBySpace={0}, expectedResult={1}")
    @CsvSource({
            "TRUE, ' '",
            "FALSE, ' and '",
    })
    void withParam(boolean separationBySpace, String expectedResult) {
        AppendPersonNamesStrategy strategy = new AppendPersonNamesStrategy(separationBySpace);
        assertEquals(expectedResult, strategy.getDelimiter());
    }
}
