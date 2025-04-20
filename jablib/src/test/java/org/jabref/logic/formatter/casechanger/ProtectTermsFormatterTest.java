package org.jabref.logic.formatter.casechanger;

import java.util.Collections;

import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
class ProtectTermsFormatterTest {

    private ProtectTermsFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new ProtectTermsFormatter(
                new ProtectedTermsLoader(new ProtectedTermsPreferences(ProtectedTermsLoader.getInternalLists(),
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList())));
    }

    @Test
    void singleWord() {
        assertEquals("{VLSI}", formatter.format("VLSI"));
    }

    @Test
    void doNotProtectAlreadyProtected() {
        assertEquals("{VLSI}", formatter.format("{VLSI}"));
    }

    @Test
    void caseSensitivity() {
        assertEquals("VLsI", formatter.format("VLsI"));
    }

    @Test
    void formatExample() {
        assertEquals("In {CDMA}", formatter.format(formatter.getExampleInput()));
    }

    @Test
    void correctOrderingOfTerms() {
        assertEquals("{3GPP} {3G}", formatter.format("3GPP 3G"));
    }

    @Test
    void test() {
        assertEquals("{VLSI} {VLSI}", formatter.format("VLSI {VLSI}"));
        assertEquals("{BPEL}", formatter.format("{BPEL}"));
        assertEquals("{Testing {BPEL} Engine Performance: A Survey}",
                formatter.format("{Testing BPEL Engine Performance: A Survey}"));
    }
}
