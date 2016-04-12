package net.sf.jabref.logic.formatter.casechanger;

import static org.junit.Assert.*;

import org.junit.Test;


/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
 */
public class ProtectTermsFormatterTest {

    private final ProtectTermsFormatter formatter = new ProtectTermsFormatter();

    @Test
    public void test() {
        assertEquals("{VLSI}", formatter.format("VLSI"));
        assertEquals("{VLSI}", formatter.format("{VLSI}"));
        assertEquals("VLsI", formatter.format("VLsI"));
        assertEquals("{VLSI} {VLSI}", formatter.format("VLSI {VLSI}"));
    }

    @Test
    public void formatExample() {
        assertEquals("In {CDMA}", formatter.format(formatter.getExampleInput()));
    }

}
