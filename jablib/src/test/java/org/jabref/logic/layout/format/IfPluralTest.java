package org.jabref.logic.layout.format;

import org.jabref.logic.layout.ParamLayoutFormatter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IfPluralTest {

    @Test
    void standardUsageOneEditor() {
        ParamLayoutFormatter a = new IfPlural();
        a.setArgument("Eds.,Ed.");
        assertEquals("Ed.", a.format("Bob Bruce"));
    }

    @Test
    void standardUsageTwoEditors() {
        ParamLayoutFormatter a = new IfPlural();
        a.setArgument("Eds.,Ed.");
        assertEquals("Eds.", a.format("Bob Bruce and Jolly Jumper"));
    }

    @Test
    void formatNull() {
        ParamLayoutFormatter a = new IfPlural();
        a.setArgument("Eds.,Ed.");
        assertEquals("", a.format(null));
    }

    @Test
    void formatEmpty() {
        ParamLayoutFormatter a = new IfPlural();
        a.setArgument("Eds.,Ed.");
        assertEquals("", a.format(""));
    }

    @Test
    void noArgumentSet() {
        ParamLayoutFormatter a = new IfPlural();
        assertEquals("", a.format("Bob Bruce and Jolly Jumper"));
    }

    @Test
    void noProperArgument() {
        ParamLayoutFormatter a = new IfPlural();
        a.setArgument("Eds.");
        assertEquals("", a.format("Bob Bruce and Jolly Jumper"));
    }
}
