package net.sf.jabref.logic.formatter.casechanger;

import static org.junit.Assert.*;

import org.junit.Test;


public class CaseKeeperTest {

    @Test
    public void test() {
        CaseKeeper ck = new CaseKeeper();

        assertEquals("{VLSI}", ck.format("VLSI"));
        assertEquals("{VLSI}", ck.format("{VLSI}"));
        assertEquals("VLsI", ck.format("VLsI"));
        assertEquals("{VLSI} {VLSI}", ck.format("VLSI {VLSI}"));
    }

}
