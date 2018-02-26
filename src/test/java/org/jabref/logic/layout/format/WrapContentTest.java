package org.jabref.logic.layout.format;

import org.jabref.logic.layout.ParamLayoutFormatter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class WrapContentTest {

    @Test
    public void testSimpleText() {
        ParamLayoutFormatter a = new WrapContent();
        a.setArgument("<,>");
        assertEquals("<Bob>", a.format("Bob"));
    }

    @Test
    public void testEmptyStart() {
        ParamLayoutFormatter a = new WrapContent();
        a.setArgument(",:");
        assertEquals("Bob:", a.format("Bob"));
    }

    @Test
    public void testEmptyEnd() {
        ParamLayoutFormatter a = new WrapContent();
        a.setArgument("Content: ,");
        assertEquals("Content: Bob", a.format("Bob"));
    }

    @Test
    public void testEscaping() {
        ParamLayoutFormatter a = new WrapContent();
        a.setArgument("Name\\,Field\\,,\\,Author");
        assertEquals("Name,Field,Bob,Author", a.format("Bob"));
    }

    @Test
    public void testFormatNullExpectNothingAdded() {
        ParamLayoutFormatter a = new WrapContent();
        a.setArgument("Eds.,Ed.");
        assertEquals(null, a.format(null));
    }

    @Test
    public void testFormatEmptyExpectNothingAdded() {
        ParamLayoutFormatter a = new WrapContent();
        a.setArgument("Eds.,Ed.");
        assertEquals("", a.format(""));
    }

    @Test
    public void testNoArgumentSetExpectNothingAdded() {
        ParamLayoutFormatter a = new WrapContent();
        assertEquals("Bob Bruce and Jolly Jumper", a.format("Bob Bruce and Jolly Jumper"));
    }

    @Test
    public void testNoProperArgumentExpectNothingAdded() {
        ParamLayoutFormatter a = new WrapContent();
        a.setArgument("Eds.");
        assertEquals("Bob Bruce and Jolly Jumper", a.format("Bob Bruce and Jolly Jumper"));
    }
}
