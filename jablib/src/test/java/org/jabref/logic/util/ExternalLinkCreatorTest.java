package org.jabref.logic.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExternalLinkCreatorTest {
    private static final String DEFAULT_SHORTSCIENCE_URL = "https://www.shortscience.org/internalsearch";
    private static final String DEFAULT_GOOGLE_SCHOLAR_URL = "https://scholar.google.com/scholar";

    private ImporterPreferences mockPreferences;
    private ExternalLinkCreator linkCreator;

    @BeforeEach
    void setUp() {
        mockPreferences = mock(ImporterPreferences.class);
        when(mockPreferences.getSearchEngineUrlTemplates()).thenReturn(Map.of());
        linkCreator = new ExternalLinkCreator(mockPreferences);
    }

    /**
     * Validates using java.net.URL.
     * This mimics strict URLUtil behavior but accepts standard form encoding (+).
     */
    private boolean urlIsValid(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private BibEntry createEntryWithTitle(String title) {
        return new BibEntry().withField(StandardField.TITLE, title);
    }

    private BibEntry createEntryWithTitleAndAuthor(String title, String author) {
        return new BibEntry()
                .withField(StandardField.TITLE, title)
                .withField(StandardField.AUTHOR, author);
    }

    static Stream<Arguments> specialCharactersProvider() {
        return Stream.of(
                Arguments.of("!*'();:@&=+$,/?#[]"),
                Arguments.of("100% Complete Research"),
                Arguments.of("C++ Programming"),
                Arguments.of("Research & Development")
        );
    }

    @Nested
    class ShortScienceTests {

        @Test
        void getShortScienceSearchURLReturnsEmptyOnMissingTitle() {
            BibEntry entry = new BibEntry();
            assertEquals(Optional.empty(), linkCreator.getShortScienceSearchURL(entry));
        }

        /**
         * FIX: BibEntry removes empty/blank strings, so these should return Empty, not a URL.
         */
        @ParameterizedTest
        @ValueSource(strings = {"", " "})
        void getShortScienceSearchURLReturnsEmptyForStandardWhitespace(String title) {
            BibEntry entry = createEntryWithTitle(title);
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);
            assertEquals(Optional.empty(), url, "BibEntry should treat '" + title + "' as empty, resulting in no URL");
        }

        /**
         * FIX: NBSP (\u00A0) usually survives BibEntry trimming, so THIS one produces a URL.
         */
        @ParameterizedTest
        @CsvSource({
                // Input contains NBSP. Output contains encoded NBSP (%C2%A0) and space (%20)
                "'   ', 'https://www.shortscience.org/internalsearch?q=%C2%A0%20%C2%A0'"
        })
        void getShortScienceSearchURLHandlesNBSP(String title, String expectedUrl) {
            BibEntry entry = createEntryWithTitle(title);
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertEquals(expectedUrl, url.get());
        }

        @ParameterizedTest
        @CsvSource({
                // Expecting %20 because we are using URIBuilder
                "'JabRef bibliography management', 'https://www.shortscience.org/internalsearch?q=JabRef%20bibliography%20management'",
                "'Machine learning', 'https://www.shortscience.org/internalsearch?q=Machine%20learning'",
        })
        void getShortScienceSearchURLLinksToSearchResults(String title, String expectedUrl) {
            BibEntry entry = createEntryWithTitle(title);
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertEquals(Optional.of(expectedUrl), url);
        }

        @ParameterizedTest
        @CsvSource({
                // Unicode: URIBuilder percent-encodes utf-8 bytes.
                "'歷史書 📖 📚', 'q=%E6%AD%B7%E5%8F%B2%E6%9B%B8%20%F0%9F%93%96%20%F0%9F%93%9A'",

                // NBSP (\u00A0) is encoded as %C2%A0. Regular spaces become %20.
                "'    History Textbook   ', 'q=%C2%A0%20%C2%A0%20History%20Textbook%C2%A0%20%C2%A0'",

                // Literal % becomes %25
                "'History%20Textbook', 'q=History%2520Textbook'",

                // Literal & becomes %26
                "'A&B Research', 'q=A%26B%20Research'"
        })
        void getShortScienceSearchURLEncodesCharacters(String title, String expectedQueryPart) {
            BibEntry entry = createEntryWithTitle(title);
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));
            assertTrue(url.get().contains(expectedQueryPart));
        }

        @ParameterizedTest
        @MethodSource("org.jabref.logic.util.ExternalLinkCreatorTest#specialCharactersProvider")
        void getShortScienceSearchURLEncodesSpecialCharacters(String title) {
            BibEntry entry = createEntryWithTitle(title);
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);
            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));
        }

        @Test
        void getShortScienceSearchURLIncludesAuthor() {
            BibEntry entry = createEntryWithTitleAndAuthor("Neural Networks", "John Doe");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));
            // Expect %20 for space
            assertTrue(url.get().contains("author=John%20Doe"));
        }
    }

    @Nested
    class GoogleScholarTests {

        @Test
        void getGoogleScholarSearchURLReturnsEmptyOnMissingTitle() {
            BibEntry entry = new BibEntry();
            assertEquals(Optional.empty(), linkCreator.getGoogleScholarSearchURL(entry));
        }

        @ParameterizedTest
        @CsvSource({
                // Expect %20
                "'JabRef bibliography management', 'https://scholar.google.com/scholar?q=JabRef%20bibliography%20management'",
                "'Machine learning', 'https://scholar.google.com/scholar?q=Machine%20learning'"
        })
        void getGoogleScholarSearchURLLinksToSearchResults(String title, String expectedUrl) {
            BibEntry entry = createEntryWithTitle(title);
            Optional<String> url = linkCreator.getGoogleScholarSearchURL(entry);
            assertEquals(Optional.of(expectedUrl), url);
        }
    }

    @Nested
    class CustomTemplateTests {

        @Test
        void usesCustomTemplateWithTitlePlaceholder() {
            // Manual template logic (URI Encoded) produces +
            when(mockPreferences.getSearchEngineUrlTemplates())
                    .thenReturn(Map.of("Short Science", "https://custom.com/search?title={title}"));

            BibEntry entry = createEntryWithTitle("Test Title");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertEquals("https://custom.com/search?title=Test+Title", url.get());
            assertTrue(urlIsValid(url.get()));
        }

        @Test
        void fallsBackWhenTemplateMissingTitlePlaceholder() {
            // Fallback logic (URIBuilder) produces %20
            when(mockPreferences.getSearchEngineUrlTemplates())
                    .thenReturn(Map.of("Short Science", "https://custom.com/search"));

            BibEntry entry = createEntryWithTitle("Test Title");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(url.get().startsWith(DEFAULT_SHORTSCIENCE_URL));
            assertTrue(url.get().contains("q=Test%20Title"));
            assertTrue(urlIsValid(url.get()));
        }
    }

    @Nested
    class SecurityTests {
        @Test
        void handlesSqlInjectionAttempts() {
            BibEntry entry = createEntryWithTitle("'; DROP TABLE entries; --");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));

            // URIBuilder encodes safely (%20)
            assertTrue(url.get().contains("q=%27%3B%20DROP%20TABLE%20entries%3B%20%E2%80%93"));
        }
    }
}
