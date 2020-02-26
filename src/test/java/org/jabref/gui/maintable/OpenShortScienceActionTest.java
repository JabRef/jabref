package org.jabref.gui.maintable;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import static org.jabref.gui.maintable.OpenShortScienceAction.getShortScienceSearchURL;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenShortScienceActionTest {

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
        assertTrue(url.isPresent() && urlIsValid(url.get()));
    }

    @Test
    void getShortScienceSearchURLReturnsEmptyOnMissingTitle() {
        BibEntry entry = new BibEntry();
        assertTrue(getShortScienceSearchURL(entry).isEmpty());
    }

    @Test
    void getShortScienceSearchURLLinksToSearchResults() {
        BibEntry entry = new BibEntry();
        // Take an arbitrary article name
        String title = "JabRef bibliography management";
        entry.setField(StandardField.TITLE, title);
        Optional<String> url = getShortScienceSearchURL(entry);
        // Expected behaviour is to link to the search results page, /internalsearch
        assertTrue(url.isPresent() && url.get().equals("https://www.shortscience.org/internalsearch?q=JabRef+bibliography+management"));
    }
}
