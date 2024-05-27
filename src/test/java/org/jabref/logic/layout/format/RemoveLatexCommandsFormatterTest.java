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

    @ParameterizedTest(name = "expected={0}, input={1}")
    @CsvSource({
            "some text, some text",                 // withoutLatexCommandsUnmodified
            "'', \\sometext",                       // singleCommandWiped
            "text, \\some text",                    // singleSpaceAfterCommandRemoved
            "text, \\some     text",                // multipleSpacesAfterCommandRemoved
            "\\, \\\\",                             // escapedBackslashBecomesBackslash
            "\\some text, \\\\some text",           // escapedBackslashFollowedByTextBecomesBackslashFollowedByText
            "\\some text\\, \\\\some text\\\\",     // escapedBackslashKept
            "some_text, some\\_text"                // escapedUnderscoreReplaces
    })
    public void formatterTest(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }
}
