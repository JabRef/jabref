package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EntryTypeFormatterTest {

    private EntryTypeFormatter formatter = new EntryTypeFormatter();

    @ParameterizedTest
    @MethodSource("formatTests")
    void testCorrectFormat(String expectedString, String inputString) {
        assertEquals(expectedString, formatter.format(inputString));
    }

    private static Stream<Arguments> formatTests() {
        return Stream.of(
                Arguments.of("Article", "article"),
                Arguments.of("Banana", "banana"),
                Arguments.of("InBook", "inbook"),
                Arguments.of("Aarticle", "aarticle")
        );
    }
}
