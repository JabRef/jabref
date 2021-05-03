package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FirstPageTest {

    private LayoutFormatter firstPageLayoutFormatter = new FirstPage();

    @Test
    public void formatEmpty() {
        assertEquals("", firstPageLayoutFormatter.format(""));
    }

    @Test
    public void formatNull() {
        assertEquals("", firstPageLayoutFormatter.format(null));
    }

    @Test
    public void formatSinglePage() {
        assertEquals("345", firstPageLayoutFormatter.format("345"));
    }

    @Test
    public void formatSingleDash() {
        assertEquals("345", firstPageLayoutFormatter.format("345-350"));
    }

    @Test
    public void formatDoubleDash() {
        assertEquals("345", firstPageLayoutFormatter.format("345--350"));
    }
}
