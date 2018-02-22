package org.jabref.model.entry;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class FileFieldWriterTest {

    @Test
    public void emptyListForEmptyInput() {
        String emptyInput = "";
        String nullInput = null;

        assertEquals(Collections.emptyList(), FileFieldParser.parse(emptyInput));
        assertEquals(Collections.emptyList(), FileFieldParser.parse(nullInput));
    }

    @Test
    public void parseCorrectInput() {
        String input = "Desc:File.PDF:PDF";

        assertEquals(Collections.singletonList(new LinkedFile("Desc", "File.PDF", "PDF")), FileFieldParser.parse(input));
    }

    @Test
    public void ingoreMissingDescription() {
        String input = ":wei2005ahp.pdf:PDF";

        assertEquals(Collections.singletonList(new LinkedFile("", "wei2005ahp.pdf", "PDF")), FileFieldParser.parse(input));
    }

    @Test
    public void interpreteLinkAsOnlyMandatoryField() {
        String single = "wei2005ahp.pdf";
        String multiple = "wei2005ahp.pdf;other.pdf";

        assertEquals(Collections.singletonList(new LinkedFile("", "wei2005ahp.pdf", "")), FileFieldParser.parse(single));
        assertEquals(Arrays.asList(new LinkedFile("", "wei2005ahp.pdf", ""), new LinkedFile("", "other.pdf", "")), FileFieldParser.parse(multiple));
    }

    @Test
    public void escapedCharactersInDescription() {
        String input = "test\\:\\;:wei2005ahp.pdf:PDF";

        assertEquals(Collections.singletonList(new LinkedFile("test:;", "wei2005ahp.pdf", "PDF")), FileFieldParser.parse(input));
    }

    @Test
    public void handleXmlCharacters() {
        String input = "test&#44\\;st\\:\\;:wei2005ahp.pdf:PDF";

        assertEquals(Collections.singletonList(new LinkedFile("test&#44;st:;", "wei2005ahp.pdf", "PDF")), FileFieldParser.parse(input));
    }

    @Test
    public void handleEscapedFilePath() {
        String input = "desc:C\\:\\\\test.pdf:PDF";

        assertEquals(Collections.singletonList(new LinkedFile("desc", "C:\\test.pdf", "PDF")), FileFieldParser.parse(input));
    }

    @Test
    public void subsetOfFieldsResultsInFileLink() {
        String descOnly = "file.pdf::";
        String fileOnly = ":file.pdf";
        String typeOnly = "::file.pdf";

        assertEquals(Collections.singletonList(new LinkedFile("", "file.pdf", "")), FileFieldParser.parse(descOnly));
        assertEquals(Collections.singletonList(new LinkedFile("", "file.pdf", "")), FileFieldParser.parse(fileOnly));
        assertEquals(Collections.singletonList(new LinkedFile("", "file.pdf", "")), FileFieldParser.parse(typeOnly));
    }

    @Test
    public void tooManySeparators() {
        String input = "desc:file.pdf:PDF:asdf";

        assertEquals(Collections.singletonList(new LinkedFile("desc", "file.pdf", "PDF")), FileFieldParser.parse(input));
    }

    @Test
    public void testQuoteStandard() {
        assertEquals("a", FileFieldWriter.quote("a"));
    }

    @Test
    public void testQuoteAllCharacters() {
        assertEquals("a\\:\\;\\\\", FileFieldWriter.quote("a:;\\"));
    }

    @Test
    public void testQuoteEmpty() {
        assertEquals("", FileFieldWriter.quote(""));
    }

    @Test
    public void testQuoteNull() {
        assertNull(FileFieldWriter.quote(null));
    }

    @Test
    public void testEncodeStringArray() {
        assertEquals("a:b;c:d", FileFieldWriter.encodeStringArray(new String[][] {{"a", "b"}, {"c", "d"}}));
        assertEquals("a:;c:d", FileFieldWriter.encodeStringArray(new String[][] {{"a", ""}, {"c", "d"}}));
        assertEquals("a:" + null + ";c:d", FileFieldWriter.encodeStringArray(new String[][] {{"a", null}, {"c", "d"}}));
        assertEquals("a:\\:b;c\\;:d", FileFieldWriter.encodeStringArray(new String[][] {{"a", ":b"}, {"c;", "d"}}));
    }

    @Test
    public void testFileFieldWriterGetStringRepresentation() {
        LinkedFile file = new LinkedFile("test", "X:\\Users\\abc.pdf", "PDF");
        assertEquals("test:X\\:/Users/abc.pdf:PDF", FileFieldWriter.getStringRepresentation(file));
    }


}
