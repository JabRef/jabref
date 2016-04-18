package net.sf.jabref.logic.formatter;


import net.sf.jabref.logic.formatter.bibtexfields.OrdinalsToSuperscriptFormatter;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
 */
public class OrdinalsToSuperscriptFormatterTest {

    private final OrdinalsToSuperscriptFormatter formatter = new OrdinalsToSuperscriptFormatter();

    @Test
    public void replacesSuperscript() {
        expectCorrect("1st", "1\\textsuperscript{st}");
        expectCorrect("2nd", "2\\textsuperscript{nd}");
        expectCorrect("3rd", "3\\textsuperscript{rd}");
        expectCorrect("4th", "4\\textsuperscript{th}");
        expectCorrect("21th", "21\\textsuperscript{th}");
    }

    @Test
    public void replaceSuperscriptsIgnoresCase() {
        expectCorrect("1st", "1\\textsuperscript{st}");
        expectCorrect("1ST", "1\\textsuperscript{ST}");
        expectCorrect("1sT", "1\\textsuperscript{sT}");
    }

    @Test
    public void replaceSuperscriptsInMultilineStrings() {
        expectCorrect(
                "replace on 1st line\nand on 2nd line.",
                "replace on 1\\textsuperscript{st} line\nand on 2\\textsuperscript{nd} line."
        );
    }

    @Test
    public void replaceAllSuperscripts() {
        expectCorrect(
                "1st 2nd 3rd 4th",
                "1\\textsuperscript{st} 2\\textsuperscript{nd} 3\\textsuperscript{rd} 4\\textsuperscript{th}"
        );
    }

    @Test
    public void ignoreSuperscriptsInsideWords() {
        expectCorrect("1st 1stword words1st inside1stwords", "1\\textsuperscript{st} 1stword words1st inside1stwords");
    }

    @Test
    public void formatExample() {
        assertEquals("11\\textsuperscript{th}", formatter.format(formatter.getExampleInput()));
    }

    private void expectCorrect(String input, String expected) {
        Assert.assertEquals(expected, formatter.format(input));
    }
}