package org.jabref.logic.search.indexing;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DocumentReaderTest {

    private static Stream<Arguments> getLinesToMerge() {
        return Stream.of(
                Arguments.of("Sentences end with periods.", "Sentences end\nwith periods."),
                Arguments.of("Text is usually wrapped with hyphens.", "Text is us-\nually wrapp-\ned with hyphens."),
                Arguments.of("Longer texts often have both.", "Longer te-\nxts often\nhave both."),
                Arguments.of("No lines to break here", "No lines to break here")
        );
    }

    @ParameterizedTest
    @MethodSource("getLinesToMerge")
    public void mergeLinesTest(String expected, String linesToMerge) {
        String result = DocumentReader.mergeLines(linesToMerge);
        assertEquals(expected, result);
    }
}
