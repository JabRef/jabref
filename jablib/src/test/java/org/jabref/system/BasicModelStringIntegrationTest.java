package org.jabref.system;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.database.BibDatabase;
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

    @Test
    void testYearFieldWithTransformation() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.YEAR, " 2024 ");

        String year = entry.getField(StandardField.YEAR).orElse("");
        assertEquals(" 2024 ", year);

        // Test isBlank on non-blank string
        assertFalse(StringUtil.isBlank(year));
        String trimmed = year.trim();
        assertEquals("2024", trimmed);

        // Test isInCurlyBrackets with a non-bracketed string
        assertFalse(StringUtil.isInCurlyBrackets(trimmed));

        // Test boldHTML transformation
        String boldYear = StringUtil.boldHTML(trimmed);
        assertEquals("<b>2024</b>", boldYear);

        // Test with blank/
        assertTrue(StringUtil.isBlank(""));
        assertTrue(StringUtil.isBlank("   "));

        // Test isInCurlyBrackets with bracketed content
        assertTrue(StringUtil.isInCurlyBrackets("{2024}"));
    }

    @Test
    void testOptionalFieldVolume() {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setField(StandardField.VOLUME, "VOl. 1");
        BibDatabase database = new BibDatabase(List.of(bibEntry));
        assertEquals(1, database.getEntryCount()); // verify entry in the database
        Set <String> expected = bibEntry.getFieldAsWords(StandardField.VOLUME);
        Optional <String> actual = bibEntry.getField(StandardField.VOLUME);
        assertNotEquals(expected, actual, "They are not equal."); // expected output : [1, VOl.]
    }
}
