package net.sf.jabref.logic.util.strings;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;


public class StringLengthComparatorTest {

    StringLengthComparator slc;


    @Before
    public void setUp() throws Exception {
        slc = new StringLengthComparator();
    }

    @Test
    public void test() {
        assertEquals(-1, slc.compare("AAA", "AA"));
        assertEquals(0, slc.compare("AA", "AA"));
        assertEquals(1, slc.compare("AA", "AAA"));
    }

}
