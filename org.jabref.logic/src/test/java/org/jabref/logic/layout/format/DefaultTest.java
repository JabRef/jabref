package org.jabref.logic.layout.format;

import org.jabref.logic.layout.ParamLayoutFormatter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class DefaultTest {

    @Test
    public void testSimpleText() {
        ParamLayoutFormatter a = new Default();
        a.setArgument("DEFAULT TEXT");
        assertEquals("Bob Bruce", a.format("Bob Bruce"));
    }

    @Test
    public void testFormatNullExpectReplace() {
        ParamLayoutFormatter a = new Default();
        a.setArgument("DEFAULT TEXT");
        assertEquals("DEFAULT TEXT", a.format(null));
    }

    @Test
    public void testFormatEmpty() {
        ParamLayoutFormatter a = new Default();
        a.setArgument("DEFAULT TEXT");
        assertEquals("DEFAULT TEXT", a.format(""));
    }

    @Test
    public void testNoArgumentSet() {
        ParamLayoutFormatter a = new Default();
        assertEquals("Bob Bruce and Jolly Jumper", a.format("Bob Bruce and Jolly Jumper"));
    }

    @Test
    public void testNoArgumentSetNullInput() {
        ParamLayoutFormatter a = new Default();
        assertEquals("", a.format(null));
    }

    @Test
    public void testNoArgumentSetEmptyInput() {
        ParamLayoutFormatter a = new Default();
        assertEquals("", a.format(""));
    }

}
