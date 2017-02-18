package org.jabref.logic.formatter.casechanger;

import java.util.Collections;

import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class ProtectTermsFormatterTest {

    private ProtectTermsFormatter formatter;

    @Before
    public void setUp() {
        ProtectTermsFormatter
                .setProtectedTermsLoader(
                        new ProtectedTermsLoader(new ProtectedTermsPreferences(ProtectedTermsLoader.getInternalLists(),
                                Collections.emptyList(), Collections.emptyList(), Collections.emptyList())));
        formatter = new ProtectTermsFormatter();
    }

    @Test
    public void testSingleWord() {
        assertEquals("{VLSI}", formatter.format("VLSI"));
    }

    @Test
    public void testDoNotProtectAlreadyProtected() {
        assertEquals("{VLSI}", formatter.format("{VLSI}"));
    }

    @Test
    public void testCaseSensitivity() {
        assertEquals("VLsI", formatter.format("VLsI"));
    }

    @Test
    public void formatExample() {
        assertEquals("In {CDMA}", formatter.format(formatter.getExampleInput()));
    }

    @Test
    public void testCorrectOrderingOfTerms() {
        assertEquals("{3GPP} {3G}", formatter.format("3GPP 3G"));
    }

    @Test
    public void test() {
        assertEquals("{VLSI} {VLSI}", formatter.format("VLSI {VLSI}"));
        assertEquals("{BPEL}", formatter.format("{BPEL}"));
        assertEquals("{Testing BPEL Engine Performance: A Survey}",
                formatter.format("{Testing BPEL Engine Performance: A Survey}"));
    }
}
