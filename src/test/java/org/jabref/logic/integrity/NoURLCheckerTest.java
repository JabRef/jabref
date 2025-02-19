package org.jabref.logic.integrity;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class NoURLCheckerTest {

    private final NoURLChecker checker = new NoURLChecker();

    @ParameterizedTest(name = "{index}. Title: \"{0}\"")
    @CsvSource({
            "Proceedings of the https://example.com/conference",
            "Find more at http://mywebsite.org/article",
            "Visit ftp://files.example.com/download",
    })
    void fieldShouldRaiseWarningForFullURLs(String title) {
        assertNotEquals(Optional.empty(), checker.checkValue(title));
    }

    @Test
    void fieldShouldAcceptURLWithoutProtocol() {
        assertEquals(Optional.empty(), checker.checkValue("Applying Trip@dvice Recommendation Technology to www.visiteurope.com"));
    }
}
