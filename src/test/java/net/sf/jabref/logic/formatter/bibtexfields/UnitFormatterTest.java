package net.sf.jabref.logic.formatter.bibtexfields;

import static org.junit.Assert.*;

import org.junit.Test;


public class UnitFormatterTest {
    @Test
    public void test() {
        UnitFormatter uf = new UnitFormatter();

        assertEquals("1~{A}", uf.format("1 A"));
        assertEquals("1\\mbox{-}{mA}", uf.format("1-mA"));
    }

}
