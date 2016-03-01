package net.sf.jabref.model.entry;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class FileFieldTest {

    @Test
    public void emptyListForEmptyInput() throws Exception {
        String emptyInput = "";
        String nullInput = null;

        assertEquals(Collections.emptyList(), FileField.parse(emptyInput));
        assertEquals(Collections.emptyList(), FileField.parse(nullInput));
    }

    @Test
    public void parseCorrectInput() throws Exception {
        String input = "Desc:File.PDF:PDF";

        assertEquals(Collections.singletonList(new FileField.ParsedFileField("Desc", "File.PDF", "PDF")), FileField.parse(input));
    }

    @Test
    public void ingoreMissingDescription() throws Exception {
        String input = ":wei2005ahp.pdf:PDF";

        assertEquals(Collections.singletonList(new FileField.ParsedFileField("", "wei2005ahp.pdf", "PDF")), FileField.parse(input));
    }

    @Test
    public void interpreteLinkAsOnlyMandatoryField() throws Exception {
        String single = "wei2005ahp.pdf";
        String multiple = "wei2005ahp.pdf;other.pdf";

        assertEquals(Collections.singletonList(new FileField.ParsedFileField("", "wei2005ahp.pdf", "")), FileField.parse(single));
        assertEquals(Arrays.asList(new FileField.ParsedFileField("", "wei2005ahp.pdf", ""), new FileField.ParsedFileField("", "other.pdf", "")), FileField.parse(multiple));
    }

    @Test
    public void escapedCharactersInDescription() throws Exception {
        String input = "test\\:\\;:wei2005ahp.pdf:PDF";

        assertEquals(Collections.singletonList(new FileField.ParsedFileField("test:;", "wei2005ahp.pdf", "PDF")), FileField.parse(input));
    }

    @Test
    public void handleXmlCharacters() throws Exception {
        String input = "test&#44\\;st\\:\\;:wei2005ahp.pdf:PDF";

        assertEquals(Collections.singletonList(new FileField.ParsedFileField("test&#44;st:;", "wei2005ahp.pdf", "PDF")), FileField.parse(input));
    }

    @Test
    public void handleEscapedFilePath() throws Exception {
        String input = "desc:C\\:\\\\test.pdf:PDF";

        assertEquals(Collections.singletonList(new FileField.ParsedFileField("desc", "C:\\test.pdf", "PDF")), FileField.parse(input));
    }

    @Test
    public void subsetOfFieldsResultsInFileLink() throws Exception {
        String descOnly = "file.pdf::";
        String fileOnly = ":file.pdf";
        String typeOnly = "::file.pdf";

        assertEquals(Collections.singletonList(new FileField.ParsedFileField("", "file.pdf", "")), FileField.parse(descOnly));
        assertEquals(Collections.singletonList(new FileField.ParsedFileField("", "file.pdf", "")), FileField.parse(fileOnly));
        assertEquals(Collections.singletonList(new FileField.ParsedFileField("", "file.pdf", "")), FileField.parse(typeOnly));
    }

    @Test
    public void tooManySeparators() throws Exception {
        String input = "desc:file.pdf:PDF:asdf";

        assertEquals(Collections.singletonList(new FileField.ParsedFileField("desc", "file.pdf", "PDF")), FileField.parse(input));
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