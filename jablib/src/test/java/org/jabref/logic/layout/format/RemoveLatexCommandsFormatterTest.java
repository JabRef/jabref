package org.jabref.logic.layout.format;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoveLatexCommandsFormatterTest {

    private RemoveLatexCommandsFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new RemoveLatexCommandsFormatter();
    }

    @Test
    void withoutLatexCommandsUnmodified() {
        assertEquals("some text", formatter.format("some text"));
    }

    @Test
    void singleCommandWiped() {
        assertEquals("", formatter.format("\\sometext"));
    }

    @Test
    void singleSpaceAfterCommandRemoved() {
        assertEquals("text", formatter.format("\\some text"));
    }

    @Test
    void multipleSpacesAfterCommandRemoved() {
        assertEquals("text", formatter.format("\\some     text"));
    }

    @Test
    void escapedBackslashBecomesBackslash() {
        assertEquals("\\", formatter.format("\\\\"));
    }

    @Test
    void escapedBackslashFollowedByTextBecomesBackslashFollowedByText() {
        assertEquals("\\some text", formatter.format("\\\\some text"));
    }

    @Test
    void escapedBackslashKept() {
        assertEquals("\\some text\\", formatter.format("\\\\some text\\\\"));
    }

    @Test
    void escapedUnderscoreReplaces() {
        assertEquals("some_text", formatter.format("some\\_text"));
    }

    @Test
    void exampleUrlCorrectlyCleaned() {
        assertEquals("http://pi.informatik.uni-siegen.de/stt/36_2/./03_Technische_Beitraege/ZEUS2016/beitrag_2.pdf", formatter.format("http://pi.informatik.uni-siegen.de/stt/36\\_2/./03\\_Technische\\_Beitraege/ZEUS2016/beitrag\\_2.pdf"));
    }
}
