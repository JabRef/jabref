package net.sf.jabref.logic.formatter.bibtexfields;

import static org.junit.Assert.*;

import org.junit.Test;


/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
 */
public class UnitsToLatexFormatterTest {

    private final UnitsToLatexFormatter formatter = new UnitsToLatexFormatter();


    @Test
    public void test() {
        assertEquals("1~{A}", formatter.format("1 A"));
        assertEquals("1\\mbox{-}{mA}", formatter.format("1-mA"));
    }

    @Test
    public void formatExample() {
        assertEquals("1~{Hz}", formatter.format(formatter.getExampleInput()));
    }

}
