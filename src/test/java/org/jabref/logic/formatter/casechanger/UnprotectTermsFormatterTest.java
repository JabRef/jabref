package org.jabref.logic.formatter.casechanger;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class UnprotectTermsFormatterTest {

    private UnprotectTermsFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new UnprotectTermsFormatter();
    }

    private static Stream<Arguments> terms() throws IOException {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of("VLSI", "{VLSI}"),
                Arguments.of("VLsI", "VLsI"),
                Arguments.of("VLSI", "VLSI"),
                Arguments.of("VLSI VLSI", "{VLSI} {VLSI}"),
                Arguments.of("BPEL", "{BPEL}"),
                Arguments.of("3GPP 3G", "{3GPP} {3G}"),
                Arguments.of("{A} and {B}}", "{A} and {B}}"),
                Arguments.of("Testing BPEL Engine Performance: A Survey", "{Testing BPEL Engine Performance: A Survey}"),
                Arguments.of("Testing BPEL Engine Performance: A Survey", "Testing {BPEL} Engine Performance: A Survey"),
                Arguments.of("Testing BPEL Engine Performance: A Survey", "{Testing {BPEL} Engine Performance: A Survey}"),
                Arguments.of("In CDMA", new UnprotectTermsFormatter().getExampleInput()));
    }

    @ParameterizedTest
    @MethodSource("terms")
    public void test(String expected, String toFormat) {
        assertEquals(expected, formatter.format(toFormat));
    }
}
