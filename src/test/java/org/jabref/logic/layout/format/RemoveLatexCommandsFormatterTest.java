package org.jabref.logic.layout.format;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoveLatexCommandsFormatterTest {

    private RemoveLatexCommandsFormatter formatter = new RemoveLatexCommandsFormatter();

    @Test
    public void exampleUrlCorrectlyCleaned() {
        assertEquals("http://pi.informatik.uni-siegen.de/stt/36_2/./03_Technische_Beitraege/ZEUS2016/beitrag_2.pdf", formatter.format("http://pi.informatik.uni-siegen.de/stt/36\\_2/./03\\_Technische\\_Beitraege/ZEUS2016/beitrag\\_2.pdf"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "withoutLatexCommandsUnmodified, some text, some text",
            "singleCommandWiped, '', \\sometext",
            "singleSpaceAfterCommandRemoved, text, \\some text",
            "multipleSpacesAfterCommandRemoved, text, \\some     text",
            "escapedBackslashBecomesBackslash, \\, \\\\",
            "escapedBackslashFollowedByTextBecomesBackslashFollowedByText, \\some text, \\\\some text",
            "escapedBackslashKept, \\some text\\, \\\\some text\\\\",
            "escapedUnderscoreReplaces, some_text, some\\_text"
    })
    public void formatterTest(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }
}
