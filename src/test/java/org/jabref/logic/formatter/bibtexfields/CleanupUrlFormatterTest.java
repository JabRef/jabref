package org.jabref.logic.formatter.bibtexfields;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *  Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class CleanupUrlFormatterTest {

    private CleanupURLFormatter formatter;

    @Before
    public void setUp() {
        formatter = new CleanupURLFormatter();
    }

    @Test
    public void removeSpecialSymbolsFromURLLink() {
        assertEquals("http://wikipedia.org",
                formatter.format("http%3A%2F%2Fwikipedia.org"));
    }

    @Test
    public void extractURLFormLink() {
        assertEquals("http://wikipedia.org",
                formatter.format("away.php?to=http%3A%2F%2Fwikipedia.org&a=snippet"));
    }

    @Test
    public void formatExample() {
        assertEquals("http://www.focus.de/" +
                "gesundheit/ratgeber/herz/test/lebenserwartung-werden-sie-100-jahre-alt_aid_363828.html",
                formatter.format(formatter.getExampleInput()));
    }
}
