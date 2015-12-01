package net.sf.jabref.logic.cleanup;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;


public class UnitFormatterTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void test() {
        UnitFormatter uf = new UnitFormatter();

        assertEquals("1~{A}", uf.format("1 A"));
        assertEquals("1\\mbox{-}{mA}", uf.format("1-mA"));
    }

}
