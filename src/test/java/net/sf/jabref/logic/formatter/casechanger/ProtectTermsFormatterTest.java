package net.sf.jabref.logic.formatter.casechanger;

import static org.junit.Assert.*;

import org.junit.Test;


/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
 */
public class ProtectTermsFormatterTest {

    @Test
    public void test() {
        ProtectTermsFormatter ck = new ProtectTermsFormatter();

        assertEquals("{VLSI}", ck.format("VLSI"));
        assertEquals("{VLSI}", ck.format("{VLSI}"));
        assertEquals("VLsI", ck.format("VLsI"));
        assertEquals("{VLSI} {VLSI}", ck.format("VLSI {VLSI}"));
    }

}
