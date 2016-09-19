package net.sf.jabref.model.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ModelStringUtilTest {

    @Test
    public void testBooleanToBinaryString() {
        assertEquals("0", ModelStringUtil.booleanToBinaryString(false));
        assertEquals("1", ModelStringUtil.booleanToBinaryString(true));
    }

    @Test
    public void testQuoteSimple() {
        assertEquals("a::", ModelStringUtil.quote("a:", "", ':'));
    }

    @Test
    public void testQuoteNullQuotation() {
        assertEquals("a::", ModelStringUtil.quote("a:", null, ':'));
    }

    @Test
    public void testQuoteNullString() {
        assertEquals("", ModelStringUtil.quote(null, ";", ':'));
    }

    @Test
    public void testQuoteQuotationCharacter() {
        assertEquals("a:::;", ModelStringUtil.quote("a:;", ";", ':'));
    }

    @Test
    public void testQuoteMoreComplicated() {
        assertEquals("a::b:%c:;", ModelStringUtil.quote("a:b%c;", "%;", ':'));
    }

}
