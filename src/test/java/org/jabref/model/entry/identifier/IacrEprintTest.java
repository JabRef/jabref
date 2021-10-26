package org.jabref.model.entry.identifier;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IacrEprintTest {

    private static Stream<Arguments> provideTestData() {
        return Stream.of(
                Arguments.arguments(
                        "acceptPlainIacrEprint",
                        "2019/001"
                ),
                Arguments.arguments(
                        "ignoreLeadingAndTrailingWhitespaces",
                        " 2019/001   "
                ),
                Arguments.arguments(
                        "acceptFullUrlIacrEprint",
                        "https://eprint.iacr.org/2019/001"
                ),
                Arguments.arguments(
                        "acceptShortenedUrlIacrEprint",
                        "https://ia.cr/2019/001"
                ),
                Arguments.arguments(
                        "acceptDomainUrlIacrEprint",
                        "eprint.iacr.org/2019/001"
                ),
                Arguments.arguments(
                        "acceptShortenedDomainUrlIacrEprint",
                        "ia.cr/2019/001"
                )
        );
    }

    @Test
    public void rejectInvalidIacrEprint() {
        assertThrows(IllegalArgumentException.class, () -> new IacrEprint("2021/12"));
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provideTestData")
    public void acceptCorrectIacrEprintIdentifier(String name, String identifier) {
        assertEquals("2019/001", new IacrEprint(identifier).getNormalized());
    }

    @Test
    public void constructValidIacrEprintUrl() {
        assertEquals("https://ia.cr/2019/001", new IacrEprint("2019/001").getAsciiUrl());
    }

}
