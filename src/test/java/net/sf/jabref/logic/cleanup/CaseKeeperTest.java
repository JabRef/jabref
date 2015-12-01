package net.sf.jabref.logic.cleanup;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;


public class CaseKeeperTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void test() {
        CaseKeeper ck = new CaseKeeper();

        assertEquals("{VLSI}", ck.format("VLSI"));
        assertEquals("{VLSI}", ck.format("{VLSI}"));
    }

}
