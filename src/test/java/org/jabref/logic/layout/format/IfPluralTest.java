package org.jabref.logic.layout.format;

import org.jabref.logic.layout.ParamLayoutFormatter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IfPluralTest {

    @Test
    public void standardUsageOneEditor() {
        ParamLayoutFormatter a = new IfPlural();
        a.setArgument("Eds.,Ed.");
        assertEquals("Ed.", a.format("Bob Bruce"));
    }

    @Test
    public void standardUsageTwoEditors() {
        ParamLayoutFormatter a = new IfPlural();
        a.setArgument("Eds.,Ed.");
        assertEquals("Eds.", a.format("Bob Bruce and Jolly Jumper"));
    }

    @Test
    public void formatNull() {
        ParamLayoutFormatter a = new IfPlural();
        a.setArgument("Eds.,Ed.");
        assertEquals("", a.format(null));
    }

    @Test
    public void formatEmpty() {
        ParamLayoutFormatter a = new IfPlural();
        a.setArgument("Eds.,Ed.");
        assertEquals("", a.format(""));
    }

    @Test
    public void noArgumentSet() {
        ParamLayoutFormatter a = new IfPlural();
        assertEquals("", a.format("Bob Bruce and Jolly Jumper"));
    }

    @Test
    public void noProperArgument() {
        ParamLayoutFormatter a = new IfPlural();
        a.setArgument("Eds.");
        assertEquals("", a.format("Bob Bruce and Jolly Jumper"));
    }
}
