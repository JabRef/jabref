package org.jabref.logic.layout.format;

import org.jabref.logic.layout.ParamLayoutFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WrapContentTest {

    private ParamLayoutFormatter wrapContentParamLayoutFormatter;

    @BeforeEach
    void setup() {
        wrapContentParamLayoutFormatter = new WrapContent();
    }

    @Test
    public void formatSimpleText() {
        wrapContentParamLayoutFormatter.setArgument("<,>");
        assertEquals("<Bob>", wrapContentParamLayoutFormatter.format("Bob"));
    }

    @Test
    public void formatEmptyStart() {
        wrapContentParamLayoutFormatter.setArgument(",:");
        assertEquals("Bob:", wrapContentParamLayoutFormatter.format("Bob"));
    }

    @Test
    public void formatEmptyEnd() {
        wrapContentParamLayoutFormatter.setArgument("Content: ,");
        assertEquals("Content: Bob", wrapContentParamLayoutFormatter.format("Bob"));
    }

    @Test
    public void formatEscaping() {
        wrapContentParamLayoutFormatter.setArgument("Name\\,Field\\,,\\,Author");
        assertEquals("Name,Field,Bob,Author", wrapContentParamLayoutFormatter.format("Bob"));
    }

    @Test
    public void formatNull() {
        wrapContentParamLayoutFormatter.setArgument("Eds.,Ed.");
        assertEquals(null, wrapContentParamLayoutFormatter.format(null));
    }

    @Test
    public void formatEmptyString() {
        wrapContentParamLayoutFormatter.setArgument("Eds.,Ed.");
        assertEquals("", wrapContentParamLayoutFormatter.format(""));
    }

    @Test
    public void noArgumentSetInFormatter() {
        assertEquals("Bob Bruce and Jolly Jumper", wrapContentParamLayoutFormatter.format("Bob Bruce and Jolly Jumper"));
    }

    @Test
    public void formatNoProperArgumentSet() {
        wrapContentParamLayoutFormatter.setArgument("Eds.");
        assertEquals("Bob Bruce and Jolly Jumper", wrapContentParamLayoutFormatter.format("Bob Bruce and Jolly Jumper"));
    }
}
