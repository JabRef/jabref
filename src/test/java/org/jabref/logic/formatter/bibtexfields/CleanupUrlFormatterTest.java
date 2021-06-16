package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
class CleanupUrlFormatterTest {

    private CleanupUrlFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new CleanupUrlFormatter();
    }

    @Test
    void removeSpecialSymbolsFromURLLink() {
        assertEquals("http://wikipedia.org",
                formatter.format("http%3A%2F%2Fwikipedia.org"));
    }

    @Test
    void extractURLFormLink() {
        assertEquals("http://wikipedia.org",
                formatter.format("away.php?to=http%3A%2F%2Fwikipedia.org&a=snippet"));
    }

    @Test
    void validUrlUnmodified() {
        assertEquals("http://wikipedia.org", formatter.format("http://wikipedia.org"));
    }

    @Test
    void latexCommandsNotRemoved() {
        assertEquals("http://pi.informatik.uni-siegen.de/stt/36\\_2/./03\\_Technische\\_Beitraege/ZEUS2016/beitrag\\_2.pdf", formatter.format("http://pi.informatik.uni-siegen.de/stt/36\\_2/./03\\_Technische\\_Beitraege/ZEUS2016/beitrag\\_2.pdf"));
    }

    @Test
    void urlencodedSlashesAreAlsoConverted() {
        // the caller has to pay attention that this does not happen
        assertEquals("jabref.org/test/test", formatter.format("jabref.org/test%2Ftest"));
    }

    @Test
    void formatExample() {
        assertEquals("http://www.focus.de/" +
                        "gesundheit/ratgeber/herz/test/lebenserwartung-werden-sie-100-jahre-alt_aid_363828.html",
                formatter.format(formatter.getExampleInput()));
    }
}
