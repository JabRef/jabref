package org.jabref.logic.layout.format;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoveLatexCommandsFormatterTest {

    private RemoveLatexCommandsFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new RemoveLatexCommandsFormatter();
    }

    @ParameterizedTest
    @CsvSource(
            delimiterString = "->",
            textBlock = """
                        some text -> some text
                        '' -> '\\sometext'
                        text -> '\\some text'
                        text -> '\\some     text'
                        '\\' -> '\\\\'
                        '\\some text' -> '\\\\some text'
                        '\\some text\\' -> '\\\\some text\\\\'
                        some_text -> 'some\\_text'
                        'http://pi.informatik.uni-siegen.de/stt/36_2/./03_Technische_Beitraege/ZEUS2016/beitrag_2.pdf' -> 'http://pi.informatik.uni-siegen.de/stt/36\\_2/./03\\_Technische\\_Beitraege/ZEUS2016/beitrag\\_2.pdf'
                    """
    )
    void format(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }
}
