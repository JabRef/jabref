package org.jabref.logic.layout.format;

import org.jabref.logic.os.OS;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RisAuthorsTest {

    private RisAuthors formatter;

    @BeforeEach
    void setUp() {
        formatter = new RisAuthors();
        formatter.setArgument("AU");
    }

    @Test
    void nullReturnsEmptyString() {
        assertEquals("", formatter.format(null));
    }

    @Test
    void singlePerson() {
        assertEquals("AU  - Smith, John", formatter.format("John Smith"));
    }

    @Test
    void multiplePeople() {
        assertEquals("AU  - Smith, John" + OS.NEWLINE + "AU  - Doe, Jane",
                formatter.format("John Smith and Jane Doe"));
    }

    @Test
    void organizationContainingAndIsSingleAuthor() {
        assertEquals("AU  - Research and Development Institute",
                formatter.format("{Research and Development Institute}"));
    }

    @Test
    void organizationAndPeopleMixed() {
        assertEquals("AU  - Food and Agriculture Organization" + OS.NEWLINE + "AU  - Smith, John",
                formatter.format("{Food and Agriculture Organization} and John Smith"));
    }

    @Test
    void latexInNameIsConvertedToUnicode() {
        assertEquals("AU  - Müller, Hans", formatter.format("Hans M{\\\"u}ller"));
    }

    @Test
    void editorTagIsUsed() {
        formatter.setArgument("A2");
        assertEquals("A2  - Smith, John", formatter.format("John Smith"));
    }
}
