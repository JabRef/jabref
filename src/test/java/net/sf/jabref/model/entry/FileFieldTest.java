package net.sf.jabref.model.entry;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class FileFieldTest {

    @Test
    public void emptyListForEmptyInput() {
        String emptyInput = "";
        String nullInput = null;

        assertEquals(Collections.emptyList(), FileField.parse(emptyInput));
        assertEquals(Collections.emptyList(), FileField.parse(nullInput));
    }

    @Test
    public void parseCorrectInput() {
        String input = "Desc:File.PDF:PDF";

        assertEquals(Collections.singletonList(new ParsedFileField("Desc", "File.PDF", "PDF")), FileField.parse(input));
    }

    @Test
    public void ingoreMissingDescription() {
        String input = ":wei2005ahp.pdf:PDF";

        assertEquals(Collections.singletonList(new ParsedFileField("", "wei2005ahp.pdf", "PDF")), FileField.parse(input));
    }

    @Test
    public void interpreteLinkAsOnlyMandatoryField() {
        String single = "wei2005ahp.pdf";
        String multiple = "wei2005ahp.pdf;other.pdf";

        assertEquals(Collections.singletonList(new ParsedFileField("", "wei2005ahp.pdf", "")), FileField.parse(single));
        assertEquals(Arrays.asList(new ParsedFileField("", "wei2005ahp.pdf", ""), new ParsedFileField("", "other.pdf", "")), FileField.parse(multiple));
    }

    @Test
    public void escapedCharactersInDescription() {
        String input = "test\\:\\;:wei2005ahp.pdf:PDF";

        assertEquals(Collections.singletonList(new ParsedFileField("test:;", "wei2005ahp.pdf", "PDF")), FileField.parse(input));
    }

    @Test
    public void handleXmlCharacters() {
        String input = "test&#44\\;st\\:\\;:wei2005ahp.pdf:PDF";

        assertEquals(Collections.singletonList(new ParsedFileField("test&#44;st:;", "wei2005ahp.pdf", "PDF")), FileField.parse(input));
    }

    @Test
    public void handleEscapedFilePath() {
        String input = "desc:C\\:\\\\test.pdf:PDF";

        assertEquals(Collections.singletonList(new ParsedFileField("desc", "C:\\test.pdf", "PDF")), FileField.parse(input));
    }

    @Test
    public void subsetOfFieldsResultsInFileLink() {
        String descOnly = "file.pdf::";
        String fileOnly = ":file.pdf";
        String typeOnly = "::file.pdf";

        assertEquals(Collections.singletonList(new ParsedFileField("", "file.pdf", "")), FileField.parse(descOnly));
        assertEquals(Collections.singletonList(new ParsedFileField("", "file.pdf", "")), FileField.parse(fileOnly));
        assertEquals(Collections.singletonList(new ParsedFileField("", "file.pdf", "")), FileField.parse(typeOnly));
    }

    @Test
    public void tooManySeparators() {
        String input = "desc:file.pdf:PDF:asdf";

        assertEquals(Collections.singletonList(new ParsedFileField("desc", "file.pdf", "PDF")), FileField.parse(input));
    }

    @Test
    public void testQuoteStandard() {
        assertEquals("a", FileField.quote("a"));
    }

    @Test
    public void testQuoteAllCharacters() {
        assertEquals("a\\:\\;\\\\", FileField.quote("a:;\\"));
    }

    @Test
    public void testQuoteEmpty() {
        assertEquals("", FileField.quote(""));
    }

    @Test
    public void testQuoteNull() {
        assertNull(FileField.quote(null));
    }

}