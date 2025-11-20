package org.jabref.logic.util;

import java.net.URI;
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
        // By default, assume no custom templates are set
        when(mockPreferences.getSearchEngineUrlTemplates()).thenReturn(Map.of());
        linkCreator = new ExternalLinkCreator(mockPreferences);
    }

    /**
     * Validates URL conformance to RFC 2396 using standard Java URI parsing.
     */
    private boolean urlIsValid(String url) {
        try {
            URI.create(url);
            return true;
        } catch (Exception e) {
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

        @Test
        void getShortScienceSearchURLReturnsEmptyOnEmptyString() {
            // BibEntry treats "" as a missing field, so we expect Empty
            BibEntry entry = createEntryWithTitle("");
            assertEquals(Optional.empty(), linkCreator.getShortScienceSearchURL(entry));
        }

        @ParameterizedTest
        @CsvSource({
                // Standard space: Java trim() removes it -> "q="
                "' ', 'https://www.shortscience.org/internalsearch?q='",

                // Non-Breaking Space (NBSP): Java trim() keeps it -> "q=%C2%A0%20%C2%A0"
                // (Note: URIBuilder encodes NBSP as %C2%A0 and the middle regular space as %20)
                "'   ', 'https://www.shortscience.org/internalsearch?q=%C2%A0%20%C2%A0'"
        })
        void getShortScienceSearchURLHandlesWhitespace(String title, String expectedUrl) {
            BibEntry entry = createEntryWithTitle(title);
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertEquals(expectedUrl, url.get());
        }

        @ParameterizedTest
        @CsvSource({
                // Standard cases using URIBuilder (%20 encoding)
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
                // Unicode characters
                "'歷史書 📖 📚', 'q=%E6%AD%B7%E5%8F%B2%E6%9B%B8%20%F0%9F%93%96%20%F0%9F%93%9A'",
                // Mixed NBSP and standard space
                "'    History Textbook   ', 'q=%C2%A0%20%C2%A0%20History%20Textbook%C2%A0%20%C2%A0'",
                // Literal symbols
                "'History%20Textbook', 'q=History%2520Textbook'",
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

        @ParameterizedTest
        @CsvSource({
                "'Machine Learning', 'Smith & Jones', 'author=Smith%20%26%20Jones'",
                "'Deep Learning', '李明', 'author=%E6%9D%8E%E6%98%8E'"
        })
        void getShortScienceSearchURLEncodesAuthorNames(String title, String author, String expectedAuthorEncoding) {
            BibEntry entry = createEntryWithTitleAndAuthor(title, author);
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));
            assertTrue(url.get().contains(expectedAuthorEncoding));
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

        @ParameterizedTest
        @ValueSource(strings = { "!*'();:@&=+$,/?#[]", "100% Complete", "Question?" })
        void getGoogleScholarSearchURLEncodesSpecialCharacters(String title) {
            BibEntry entry = createEntryWithTitle(title);
            Optional<String> url = linkCreator.getGoogleScholarSearchURL(entry);
            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));
        }

        @Test
        void getGoogleScholarSearchURLIncludesAuthor() {
            BibEntry entry = createEntryWithTitleAndAuthor("Quantum Computing", "Alice Smith");
            Optional<String> url = linkCreator.getGoogleScholarSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));
            assertTrue(url.get().contains("author=Alice%20Smith"));
        }
    }

    @Nested
    class CustomTemplateTests {

        @Test
        void usesCustomTemplateWithTitlePlaceholder() {
            // Manual template logic uses URLEncoder, so we expect '+'
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
            // Fallback logic uses URIBuilder, so we expect '%20'
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
        @ParameterizedTest
        @ValueSource(strings = {
                "mailto:test@example.com",
                "javascript:alert('xss')",
                "file:///etc/passwd",
                "ftp://malicious.com"
        })
        void rejectsNonHttpSchemesAndFallsBack(String maliciousUrl) {
            when(mockPreferences.getSearchEngineUrlTemplates())
                    .thenReturn(Map.of("Short Science", maliciousUrl + "?q={title}"));

            BibEntry entry = createEntryWithTitle("Test");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            // Must fall back to the default secure HTTPS URL
            assertTrue(url.get().startsWith("https://"));
            assertTrue(urlIsValid(url.get()));
        }

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
