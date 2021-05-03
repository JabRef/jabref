package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.jabref.logic.layout.ParamLayoutFormatter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WrapContentTest {

    private ParamLayoutFormatter wrapContentParamLayoutFormatter = new WrapContent();

    @ParameterizedTest
    @MethodSource("provideContent")
    void formatContent(String formattedContent, String originalContent, String desiredFormat) {
        if (!desiredFormat.isEmpty()) {
            wrapContentParamLayoutFormatter.setArgument(desiredFormat);
        }

        assertEquals(formattedContent, wrapContentParamLayoutFormatter.format(originalContent));
    }

    private static Stream<Arguments> provideContent() {
        return Stream.of(
                Arguments.of("<Bob>", "Bob", "<,>"),
                Arguments.of("Bob:", "Bob", ",:"),
                Arguments.of("Content: Bob", "Bob", "Content: ,"),
                Arguments.of("Name,Field,Bob,Author", "Bob", "Name\\,Field\\,,\\,Author"),
                Arguments.of(null, null, "Eds.,Ed."),
                Arguments.of("", "", "Eds.,Ed."),
                Arguments.of("Bob Bruce and Jolly Jumper", "Bob Bruce and Jolly Jumper", ""),
                Arguments.of("Bob Bruce and Jolly Jumper", "Bob Bruce and Jolly Jumper", "Eds.")
        );
    }
}
