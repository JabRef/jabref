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
                        new IacrEprint("2019/001").getNormalized()
                ),
                Arguments.arguments(
                        "ignoreLeadingAndTrailingWhitespaces",
                        new IacrEprint(" 2019/001   ").getNormalized()
                ),
                Arguments.arguments(
                        "acceptFullUrlIacrEprint",
                        new IacrEprint("https://eprint.iacr.org/2019/001").getNormalized()
                ),
                Arguments.arguments(
                        "acceptShortenedUrlIacrEprint",
                        new IacrEprint("https://ia.cr/2019/001").getNormalized()
                ),
                Arguments.arguments(
                        "acceptDomainUrlIacrEprint",
                        new IacrEprint("eprint.iacr.org/2019/001").getNormalized()
                ),
                Arguments.arguments(
                        "acceptShortenedDomainUrlIacrEprint",
                        new IacrEprint("ia.cr/2019/001").getNormalized()
                )
        );
    }

    @Test
    public void rejectInvalidIacrEprint() {
        assertThrows(IllegalArgumentException.class, () -> new IacrEprint("2021/12"));
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provideTestData")
    public void acceptCorrectIacrEprintIdentifier(String name, String arg) {
        assertEquals("2019/001", arg);
    }

    @Test
    public void constructValidIacrEprintUrl() {
        assertEquals("https://ia.cr/2019/001", new IacrEprint("2019/001").getAsciiUrl());
    }

}
