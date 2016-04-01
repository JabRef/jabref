package net.sf.jabref.logic.formatter.casechanger;

import static org.junit.Assert.*;

import org.junit.Test;


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
