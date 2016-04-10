package net.sf.jabref.logic.formatter.bibtexfields;

import static org.junit.Assert.*;

import org.junit.Test;


/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
 */
public class UnitsToLatexFormatterTest {
    @Test
    public void test() {
        UnitsToLatexFormatter uf = new UnitsToLatexFormatter();

        assertEquals("1~{A}", uf.format("1 A"));
        assertEquals("1\\mbox{-}{mA}", uf.format("1-mA"));
    }

}
