package org.jabref.logic.util;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalLinkCreatorTest {

    // Create stub for ImporterPreferences to test
    private static class StubImporterPreferences extends ImporterPreferences {
        public StubImporterPreferences() {
            super(
                    true, // importerEnabled
                    true, // generateNewKeyOnImport
                    null, // importWorkingDirectory
                    true, // warnAboutDuplicatesOnImport
                    Collections.emptySet(), // customImporters
                    Collections.emptySet(), // apiKeys
                    Collections.emptyMap(), // defaultApiKeys
                    true, // persistCustomKeys
                    List.of(), // catalogs
                    null, // defaultPlainCitationParser
                    Collections.emptyMap() // searchEngineUrlTemplates
            );
        }
    }

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
        ImporterPreferences stubPreferences = new StubImporterPreferences();
        ExternalLinkCreator linkCreator = new ExternalLinkCreator(stubPreferences);

        BibEntry entry = new BibEntry();
        String rfc3986ReservedCharacters = "!*'();:@&=+$,/?#[]";
        entry.setField(StandardField.TITLE, rfc3986ReservedCharacters);
        Optional<String> url = linkCreator.getShortScienceSearchURL(entry);
        assertTrue(url.isPresent());
        assertTrue(urlIsValid(url.get()));
    }

    @Test
    void getShortScienceSearchURLReturnsEmptyOnMissingTitle() {
        ImporterPreferences stubPreferences = new StubImporterPreferences();
        ExternalLinkCreator linkCreator = new ExternalLinkCreator(stubPreferences);

        BibEntry entry = new BibEntry();
        assertEquals(Optional.empty(), linkCreator.getShortScienceSearchURL(entry));
    }

    @Test
    void getShortScienceSearchURLLinksToSearchResults() {
        ImporterPreferences stubPreferences = new StubImporterPreferences();
        ExternalLinkCreator linkCreator = new ExternalLinkCreator(stubPreferences);

        // Take an arbitrary article name
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "JabRef bibliography management");
        Optional<String> url = linkCreator.getShortScienceSearchURL(entry);
        // Expected behaviour is to link to the search results page, /internalsearch
        assertEquals(Optional.of("https://www.shortscience.org/internalsearch?q=JabRef%20bibliography%20management"), url);
    }
}
