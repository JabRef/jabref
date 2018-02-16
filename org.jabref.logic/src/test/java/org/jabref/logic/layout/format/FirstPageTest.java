package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class FirstPageTest {

    @Test
    public void testFormatEmpty() {
        LayoutFormatter a = new FirstPage();
        assertEquals("", a.format(""));
    }

    @Test
    public void testFormatNull() {
        LayoutFormatter a = new FirstPage();
        assertEquals("", a.format(null));
    }

    @Test
    public void testFormatSinglePage() {
        LayoutFormatter a = new FirstPage();
        assertEquals("345", a.format("345"));
    }

    @Test
    public void testFormatSingleDash() {
        LayoutFormatter a = new FirstPage();
        assertEquals("345", a.format("345-350"));
    }

    @Test
    public void testFormatDoubleDash() {
        LayoutFormatter a = new FirstPage();
        assertEquals("345", a.format("345--350"));
    }
}
