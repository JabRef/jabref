package org.jabref.logic.formatter.casechanger;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;


class ProtectTermsFormatterTest {

    private ProtectTermsFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new ProtectTermsFormatter(
                new ProtectedTermsLoader(new ProtectedTermsPreferences(ProtectedTermsLoader.getInternalLists(),
                        List.of(), List.of(), List.of())));
    }

    @ParameterizedTest(name = "format(\"{1}\") should return \"{0}\"")
    @MethodSource("provideTestCases")
    void formatProtectedTerms(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }

    private static Stream<Arguments> provideTestCases() {
        return Stream.of(
                // Single word protection
                Arguments.of("{VLSI}", "VLSI"),

                // Do not protect already protected terms
                Arguments.of("{VLSI}", "{VLSI}"),

                // Case sensitivity - mixed case should not be protected
                Arguments.of("VLsI", "VLsI"),

                // Correct ordering of multiple terms
                Arguments.of("{3GPP} {3G}", "3GPP 3G"),

                // Mixed protected and unprotected terms
                Arguments.of("{VLSI} {VLSI}", "VLSI {VLSI}"),

                // Already protected term remains unchanged
                Arguments.of("{BPEL}", "{BPEL}"),

                // Complex case with nested protection
                Arguments.of("{Testing {BPEL} Engine Performance: A Survey}",
                        "{Testing BPEL Engine Performance: A Survey}")
        );
    }

    @Test
    void formatExample() {
        assertEquals("In {CDMA}", formatter.format(formatter.getExampleInput()));
    }
}
