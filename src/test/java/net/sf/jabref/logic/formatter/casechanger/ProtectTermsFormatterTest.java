package net.sf.jabref.logic.formatter.casechanger;

import java.util.Collections;

import net.sf.jabref.logic.protectedterms.ProtectedTermsLoader;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
 */
public class ProtectTermsFormatterTest {

    private ProtectedTermsFormatter formatter;

    @Before
    public void setUp() {
        ProtectedTermsFormatter
                .setProtectedTermsLoader(new ProtectedTermsLoader(ProtectedTermsLoader.getInternalLists(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));
        formatter = new ProtectedTermsFormatter();
    }

    @Test
    public void test() {
        assertEquals("{VLSI}", formatter.format("VLSI"));
        assertEquals("{VLSI}", formatter.format("{VLSI}"));
        assertEquals("VLsI", formatter.format("VLsI"));
        assertEquals("{VLSI} {VLSI}", formatter.format("VLSI {VLSI}"));
        assertEquals("{BPEL}", formatter.format("{BPEL}"));
        assertEquals("{Testing BPEL Engine Performance: A Survey}", formatter.format("{Testing BPEL Engine Performance: A Survey}"));
    }

    @Test
    public void formatExample() {
        assertEquals("In {CDMA}", formatter.format(formatter.getExampleInput()));
    }

}
