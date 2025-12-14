package org.jabref.model.pdf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PdfSectionTest {

    @Test
    void testValidPdfSection() {
        PdfSection section = new PdfSection("Introduction", "Content here", 1, 3);
        assertEquals("Introduction", section.name());
        assertEquals("Content here", section.content());
        assertEquals(1, section.startPage());
        assertEquals(3, section.endPage());
    }

    @Test
    void testSameStartAndEndPage() {
        PdfSection section = new PdfSection("Abstract", "Short content", 1, 1);
        assertEquals(1, section.startPage());
        assertEquals(1, section.endPage());
    }

    @Test
    void testBlankNameThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new PdfSection("   ", "content", 1, 1));
    }

    @Test
    void testNullNameThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new PdfSection(null, "content", 1, 1));
    }

    @Test
    void testNullContentThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new PdfSection("Name", null, 1, 1));
    }

    @Test
    void testEmptyContentAllowed() {
        PdfSection section = new PdfSection("Empty Section", "", 1, 1);
        assertEquals("", section.content());
    }

    @Test
    void testZeroStartPageThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new PdfSection("Name", "content", 0, 1));
    }

    @Test
    void testNegativeStartPageThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new PdfSection("Name", "content", -1, 1));
    }

    @Test
    void testEndPageBeforeStartPageThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new PdfSection("Name", "content", 5, 3));
    }
}
