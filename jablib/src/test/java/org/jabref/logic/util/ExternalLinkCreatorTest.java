package org.jabref.logic.util;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

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
                    5, // citationsRelationsStoreTTL
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

    static Stream<Arguments> specialCharactersProvider() {
        return Stream.of(
            Arguments.of("!*'();:@&=+$,/?#[]")
        );
    }

    @ParameterizedTest
    @MethodSource("specialCharactersProvider")
    void getShortScienceSearchURLEncodesSpecialCharacters(String title) {
        ImporterPreferences stubPreferences = new StubImporterPreferences();
        ExternalLinkCreator linkCreator = new ExternalLinkCreator(stubPreferences);

        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, title);
        Optional<String> url = linkCreator.getShortScienceSearchURL(entry);
        assertTrue(url.isPresent());
        assertTrue(urlIsValid(url.get()));
    }

    @ParameterizedTest
    @CsvSource({
        "'歷史書 📖 📚', 'https://www.shortscience.org/internalsearch?q=%E6%AD%B7%E5%8F%B2%E6%9B%B8%20%F0%9F%93%96%20%F0%9F%93%9A'",
        "'    History Textbook   ', 'https://www.shortscience.org/internalsearch?q=History%20Textbook'",
        "'History%20Textbook', 'https://www.shortscience.org/internalsearch?q=History%2520Textbook'",
        "'JabRef bibliography management', 'https://www.shortscience.org/internalsearch?q=JabRef%20bibliography%20management'"
    })
    void getShortScienceSearchURLEncodesCharacters(String title, String expectedUrl) {
        ImporterPreferences stubPreferences = new StubImporterPreferences();
        ExternalLinkCreator linkCreator = new ExternalLinkCreator(stubPreferences);

        BibEntry entry = new BibEntry().withField(StandardField.TITLE, title);
        Optional<String> url = linkCreator.getShortScienceSearchURL(entry);
        assertEquals(Optional.of(expectedUrl), url);
    }

    @Test
    void getShortScienceSearchURLReturnsEmptyOnMissingTitle() {
        ImporterPreferences stubPreferences = new StubImporterPreferences();
        ExternalLinkCreator linkCreator = new ExternalLinkCreator(stubPreferences);

        BibEntry entry = new BibEntry();
        assertEquals(Optional.empty(), linkCreator.getShortScienceSearchURL(entry));
    }

    @ParameterizedTest
    @CsvSource({
            "JabRef bibliography management, https://www.shortscience.org/internalsearch?q=JabRef%20bibliography%20management",
            "Machine learning, https://www.shortscience.org/internalsearch?q=Machine%20learning",
    })
    void getShortScienceSearchURLLinksToSearchResults(String title, String expectedUrl) {
        ImporterPreferences stubPreferences = new StubImporterPreferences();
        ExternalLinkCreator linkCreator = new ExternalLinkCreator(stubPreferences);

        BibEntry entry = new BibEntry().withField(StandardField.TITLE, title);
        Optional<String> url = linkCreator.getShortScienceSearchURL(entry);
        assertEquals(Optional.of(expectedUrl), url);
    }
}
