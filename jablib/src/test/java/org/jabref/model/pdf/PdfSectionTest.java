package org.jabref.model.pdf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PdfSectionTest {

    @Test
    void validPdfSection() {
        PdfSection section = new PdfSection("Introduction", "Content here", 1, 3);
        assertEquals("Introduction", section.name());
        assertEquals("Content here", section.content());
        assertEquals(1, section.startPage());
        assertEquals(3, section.endPage());
    }

    @Test
    void sameStartAndEndPage() {
        PdfSection section = new PdfSection("Abstract", "Short content", 1, 1);
        assertEquals(1, section.startPage());
        assertEquals(1, section.endPage());
    }

    @Test
    void blankNameThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new PdfSection("   ", "content", 1, 1));
    }

    @Test
    void nullNameThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new PdfSection(null, "content", 1, 1));
    }

    @Test
    void nullContentThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new PdfSection("Name", null, 1, 1));
    }

    @Test
    void emptyContentAllowed() {
        PdfSection section = new PdfSection("Empty Section", "", 1, 1);
        assertEquals("", section.content());
    }

    @Test
    void zeroStartPageThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new PdfSection("Name", "content", 0, 1));
    }

    @Test
    void negativeStartPageThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new PdfSection("Name", "content", -1, 1));
    }

    @Test
    void endPageBeforeStartPageThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new PdfSection("Name", "content", 5, 3));
    }
}
