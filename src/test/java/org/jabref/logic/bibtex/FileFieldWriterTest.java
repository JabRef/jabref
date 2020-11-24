package org.jabref.logic.bibtex;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.util.FileFieldParser;
import org.jabref.model.entry.LinkedFile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class FileFieldWriterTest {

    @Test
    public void emptyListForEmptyInput() {
        String emptyInput = "";

        assertEquals(Collections.emptyList(), FileFieldParser.parse(emptyInput));
        assertEquals(Collections.emptyList(), FileFieldParser.parse(null));
    }

    @Test
    public void parseCorrectInput() {
        String input = "Desc:File.PDF:PDF";

        assertEquals(
                Collections.singletonList(new LinkedFile("Desc", Path.of("File.PDF"), "PDF")),
                FileFieldParser.parse(input));
    }

    @Test
    public void parseCorrectOnlineInput() throws MalformedURLException {
        String input = ":http\\://arxiv.org/pdf/2010.08497v1:PDF";
        String inputURL = "http://arxiv.org/pdf/2010.08497v1";
        List<LinkedFile> expected = Collections.singletonList(new LinkedFile(new URL(inputURL), "PDF"));

        assertEquals(expected, FileFieldParser.parse(input));
    }

    @Test
    public void parseFaultyOnlineInput() {
        String input = ":htt\\://arxiv.org/pdf/2010.08497v1:PDF";
        String inputURL = "htt://arxiv.org/pdf/2010.08497v1";
        List<LinkedFile> expected = Collections.singletonList(new LinkedFile("", Path.of(inputURL), "PDF"));

        assertEquals(expected, FileFieldParser.parse(input));
    }

    @Test
    public void ingoreMissingDescription() {
        String input = ":wei2005ahp.pdf:PDF";

        assertEquals(
                Collections.singletonList(new LinkedFile("", Path.of("wei2005ahp.pdf"), "PDF")),
                FileFieldParser.parse(input));
    }

    @Test
    public void interpreteLinkAsOnlyMandatoryField() {
        String single = "wei2005ahp.pdf";
        String multiple = "wei2005ahp.pdf;other.pdf";

        assertEquals(
                Collections.singletonList(new LinkedFile("", Path.of("wei2005ahp.pdf"), "")),
                FileFieldParser.parse(single));

        assertEquals(
                Arrays.asList(
                        new LinkedFile("", Path.of("wei2005ahp.pdf"), ""),
                        new LinkedFile("", Path.of("other.pdf"), "")),
                FileFieldParser.parse(multiple));
    }

    @Test
    public void escapedCharactersInDescription() {
        String input = "test\\:\\;:wei2005ahp.pdf:PDF";

        assertEquals(
                Collections.singletonList(new LinkedFile("test:;", Path.of("wei2005ahp.pdf"), "PDF")),
                FileFieldParser.parse(input));
    }

    @Test
    public void handleXmlCharacters() {
        String input = "test&#44\\;st\\:\\;:wei2005ahp.pdf:PDF";

        assertEquals(
                Collections.singletonList(new LinkedFile("test&#44;st:;", Path.of("wei2005ahp.pdf"), "PDF")),
                FileFieldParser.parse(input));
    }

    @Test
    public void handleEscapedFilePath() {
        String input = "desc:C\\:\\\\test.pdf:PDF";

        assertEquals(
                Collections.singletonList(new LinkedFile("desc", Path.of("C:\\test.pdf"), "PDF")),
                FileFieldParser.parse(input));
    }

    @Test
    public void subsetOfFieldsResultsInFileLink() {
        String descOnly = "file.pdf::";
        String fileOnly = ":file.pdf";
        String typeOnly = "::file.pdf";

        assertEquals(
                Collections.singletonList(new LinkedFile("", Path.of("file.pdf"), "")),
                FileFieldParser.parse(descOnly));

        assertEquals(
                Collections.singletonList(new LinkedFile("", Path.of("file.pdf"), "")),
                FileFieldParser.parse(fileOnly));

        assertEquals(
                Collections.singletonList(new LinkedFile("", Path.of("file.pdf"), "")),
                FileFieldParser.parse(typeOnly));
    }

    @Test
    public void tooManySeparators() {
        String input = "desc:file.pdf:PDF:asdf";

        assertEquals(
                Collections.singletonList(new LinkedFile("desc", Path.of("file.pdf"), "PDF")),
                FileFieldParser.parse(input));
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
        assertEquals("a:b;c:d", FileFieldWriter.encodeStringArray(new String[][]{{"a", "b"}, {"c", "d"}}));
        assertEquals("a:;c:d", FileFieldWriter.encodeStringArray(new String[][]{{"a", ""}, {"c", "d"}}));
        assertEquals("a:" + null + ";c:d", FileFieldWriter.encodeStringArray(new String[][]{{"a", null}, {"c", "d"}}));
        assertEquals("a:\\:b;c\\;:d", FileFieldWriter.encodeStringArray(new String[][]{{"a", ":b"}, {"c;", "d"}}));
    }

    @Test
    public void testFileFieldWriterGetStringRepresentation() {
        LinkedFile file = new LinkedFile("test", Path.of("X:\\Users\\abc.pdf"), "PDF");
        assertEquals("test:X\\:/Users/abc.pdf:PDF", FileFieldWriter.getStringRepresentation(file));
    }
}
