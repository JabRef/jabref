package net.sf.jabref.model.entry;

import org.junit.Test;

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
    public void tooLessSeparators() throws Exception {
        String input = "desc:";

        assertEquals(Collections.singletonList(new FileField.ParsedFileField("desc", "", "")), FileField.parse(input));
    }

    @Test
    public void tooManySeparators() throws Exception {
        String input = "desc:file.pdf:PDF:asdf";

        assertEquals(Collections.singletonList(new FileField.ParsedFileField("desc", "file.pdf", "PDF")), FileField.parse(input));
    }
}