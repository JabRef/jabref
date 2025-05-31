package org.jabref.logic.util;

import java.net.MalformedURLException;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.jabref.logic.util.ExternalLinkCreator.getShortScienceSearchURL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalLinkCreatorTest {

    /**
     * Validates URL conformance to RFC2396. Does not perform complex checks such as opening connections.
     */
    private boolean urlIsValid(String url) {
        try {
            // This will throw on non-compliance to RFC2396.
            URLUtil.create(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    @Test
    void getShortScienceSearchURLEncodesSpecialCharacters() {
        BibEntry entry = new BibEntry();
        String rfc3986ReservedCharacters = "!*'();:@&=+$,/?#[]";
        entry.setField(StandardField.TITLE, rfc3986ReservedCharacters);
        Optional<String> url = getShortScienceSearchURL(entry);
        assertTrue(url.isPresent());
        assertTrue(urlIsValid(url.get()));
    }

    @Test
    void getShortScienceSearchURLEncodesUnicodeCharacters() {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "歷史書 📖 📚");
        Optional<String> url = getShortScienceSearchURL(entry);

        // Expected behaviour is to link to the search results page, /internalsearch, and the unicode and emojis are percent-encoded
        assertEquals(
                Optional.of("https://www.shortscience.org/internalsearch?q=%E6%AD%B7%E5%8F%B2%E6%9B%B8%20%F0%9F%93%96%20%F0%9F%93%9A"),
                url
        );
    }

    @Test
    void getShortScienceSearchURLTrimsWhitespaceInTitle() {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "    History Textbook   ");
        Optional<String> url = getShortScienceSearchURL(entry);
        assertEquals(
                Optional.of("https://www.shortscience.org/internalsearch?q=History%20Textbook"),
                url
        );
    }

    @Test
    void getShortScienceSearchURLEncodesAlreadyEncodedCharacters() {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "History%20Textbook");
        Optional<String> url = getShortScienceSearchURL(entry);

        // Expected behaviour is that the already encoded %20 should be treated as a literal string
        // instead of a space and be re-encoded from % to %25, making %20 as %2520
        assertEquals(
                Optional.of("https://www.shortscience.org/internalsearch?q=History%2520Textbook"),
                url
        );
    }

    @Test
    void getShortScienceSearchURLReturnsEmptyOnMissingTitle() {
        BibEntry entry = new BibEntry();
        assertEquals(Optional.empty(), getShortScienceSearchURL(entry));
    }

    @Test
    void getShortScienceSearchURLLinksToSearchResults() {
        // Take an arbitrary article name
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "JabRef bibliography management");
        Optional<String> url = getShortScienceSearchURL(entry);
        // Expected behaviour is to link to the search results page, /internalsearch
        assertEquals(
                Optional.of("https://www.shortscience.org/internalsearch?q=JabRef%20bibliography%20management"),
                url
        );
    }
}
