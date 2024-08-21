package org.jabref.logic.layout.format;

import org.jabref.logic.layout.ParamLayoutFormatter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReplaceTest {

    @Test
    void simpleText() {
        ParamLayoutFormatter a = new Replace();
        a.setArgument("Bob,Ben");
        assertEquals("Ben Bruce", a.format("Bob Bruce"));
    }

    @Test
    void simpleTextNoHit() {
        ParamLayoutFormatter a = new Replace();
        a.setArgument("Bob,Ben");
        assertEquals("Jolly Jumper", a.format("Jolly Jumper"));
    }

    @Test
    void formatNull() {
        ParamLayoutFormatter a = new Replace();
        a.setArgument("Eds.,Ed.");
        assertNull(a.format(null));
    }

    @Test
    void formatEmpty() {
        ParamLayoutFormatter a = new Replace();
        a.setArgument("Eds.,Ed.");
        assertEquals("", a.format(""));
    }

    @Test
    void noArgumentSet() {
        ParamLayoutFormatter a = new Replace();
        assertEquals("Bob Bruce and Jolly Jumper", a.format("Bob Bruce and Jolly Jumper"));
    }

    @Test
    void noProperArgument() {
        ParamLayoutFormatter a = new Replace();
        a.setArgument("Eds.");
        assertEquals("Bob Bruce and Jolly Jumper", a.format("Bob Bruce and Jolly Jumper"));
    }
}
