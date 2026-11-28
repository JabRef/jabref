package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Iso690NamesAuthorsTest {

    private LayoutFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new Iso690NamesAuthors();
    }

    @Test
    void formatEmptyOrNull() {
        assertEquals("", formatter.format(""));
        assertEquals("", formatter.format("   "));
        assertEquals("", formatter.format(null));
    }

    @Test
    void formatSingleAuthor() {
        assertEquals("SMITH, Sandra", formatter.format("Sandra Smith"));
    }

    @Test
    void formatSingleAuthorLastNameFirst() {
        assertEquals("SMITH, Sandra", formatter.format("Smith, Sandra"));
    }

    @Test
    void formatTwoAuthors() {
        assertEquals("SMITH, Jane y DOE, John", formatter.format("Jane Smith and John Doe"));
    }

    @Test
    void formatThreeAuthors() {
        assertEquals("ONE, Author, TWO, Author y THREE, Author",
                formatter.format("Author One and Author Two and Author Three"));
    }

    @Test
    void formatBracedOrganization() {
        assertEquals("{RESEARCH AND DEVELOPMENT INSTITUTE}",
                formatter.format("{Research and Development Institute}"));
    }

    @Test
    void formatOrganizationAndAuthor() {
        assertEquals("{RESEARCH AND DEVELOPMENT INSTITUTE} y DOE, Jane",
                formatter.format("{Research and Development Institute} and Jane Doe"));
    }

    @Test
    void formatAuthorWithVonPrefix() {
        assertEquals("VON NEUMANN, John", formatter.format("John von Neumann"));
    }

    @Test
    void formatAuthorWithSuffix() {
        assertEquals("SMITH, Jr., John", formatter.format("Smith, Jr., John"));
    }

    @Test
    void formatSingleName() {
        assertEquals("ARISTOTLE", formatter.format("Aristotle"));
    }
}
