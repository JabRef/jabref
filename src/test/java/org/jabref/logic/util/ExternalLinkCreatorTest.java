package org.jabref.logic.util;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
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
            new URL(url).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
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
        assertEquals(Optional.of("https://www.shortscience.org/internalsearch?q=JabRef+bibliography+management"), url);
    }
}
