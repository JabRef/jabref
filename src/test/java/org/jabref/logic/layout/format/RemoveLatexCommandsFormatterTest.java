package org.jabref.logic.layout.format;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoveLatexCommandsFormatterTest {

    private RemoveLatexCommandsFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new RemoveLatexCommandsFormatter();
    }

    @Test
    public void withoutLatexCommandsUnmodified() {
        assertEquals("some text", formatter.format("some text"));
    }

    @Test
    public void singleCommandWiped() {
        assertEquals("", formatter.format("\\sometext"));
    }

    @Test
    public void singleSpaceAfterCommandRemoved() {
        assertEquals("text", formatter.format("\\some text"));
    }

    @Test
    public void multipleSpacesAfterCommandRemoved() {
        assertEquals("text", formatter.format("\\some     text"));
    }

    @Test
    public void escapedBackslashBecomesBackslash() {
        assertEquals("\\", formatter.format("\\\\"));
    }

    @Test
    public void escapedBackslashFollowedByTextBecomesBackslashFollowedByText() {
        assertEquals("\\some text", formatter.format("\\\\some text"));
    }

    @Test
    public void escapedBackslashKept() {
        assertEquals("\\some text\\", formatter.format("\\\\some text\\\\"));
    }

    @Test
    public void escapedUnderscoreReplaces() {
        assertEquals("some_text", formatter.format("some\\_text"));
    }

    @Test
    public void exampleUrlCorrectlyCleaned() {
        assertEquals("http://pi.informatik.uni-siegen.de/stt/36_2/./03_Technische_Beitraege/ZEUS2016/beitrag_2.pdf", formatter.format("http://pi.informatik.uni-siegen.de/stt/36\\_2/./03\\_Technische\\_Beitraege/ZEUS2016/beitrag\\_2.pdf"));
    }
}
