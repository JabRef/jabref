package org.jabref.system;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BasicModelStringIntegrationTest {

    @Test
    void testAuthorFieldAndStringUtilMethods() {
        BibEntry entry = new BibEntry();
        // use a value that exercise shaveString (removes surrounding braces/quotes)
        entry.setField(StandardField.AUTHOR, "{john doe}");

        String author = entry.getField(StandardField.AUTHOR).orElse("");
        assertEquals("{john doe}", author);

        // shaveString should remove one pair of surrounding braces
        String cleaned = StringUtil.shaveString(author);
        assertEquals("john doe", cleaned);

        // capitalizeFirst exists in StringUtil
        String capitalized = StringUtil.capitalizeFirst("john");
        assertEquals("John", capitalized);

        // repeatSpaces exists
        String threeSpaces = StringUtil.repeatSpaces(3);
        assertEquals("   ", threeSpaces);

        // quoteForHTML exists and encodes characters as numeric entities
        String quoted = StringUtil.quoteForHTML("!");
        assertEquals("&#33;", quoted);
    }

    @Test
    void testTitleFieldAndCaseInsensitiveSuffix() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "a sample title.TXT");

        String title = entry.getField(StandardField.TITLE).orElse("");
        assertEquals("a sample title.TXT", title);

        // endsWithIgnoreCase exists in StringUtil
        boolean endsWithTxt = StringUtil.endsWithIgnoreCase(title, "txt");
        assertTrue(endsWithTxt);

        // removeBracesAroundCapitals: demonstrate it doesn't alter normal title
        String noChange = StringUtil.removeBracesAroundCapitals(title);
        assertEquals(title, noChange);
    }
}
