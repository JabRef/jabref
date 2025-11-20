package org.jabref.logic.util;

import java.net.MalformedURLException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        // By default, assume no custom templates are set, forcing use of default URLs
        when(mockPreferences.getSearchEngineUrlTemplates()).thenReturn(Map.of());
        linkCreator = new ExternalLinkCreator(mockPreferences);
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

    // --- ShortScience Search Tests ---
    @Nested
    class ShortScienceTests {

        @Test
        void getShortScienceSearchURLReturnsEmptyOnMissingTitle() {
            BibEntry entry = new BibEntry();
            assertEquals(Optional.empty(), linkCreator.getShortScienceSearchURL(entry));
        }

        @Test
        void getShortScienceSearchURLHandlesNullTitle() {
            BibEntry entry = new BibEntry().withField(StandardField.TITLE, null);
            assertEquals(Optional.empty(), linkCreator.getShortScienceSearchURL(entry));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   "}) // includes non-breaking space (U+00A0)
        void getShortScienceSearchURLHandlesEmptyOrWhitespaceTitles(String title) {
            BibEntry entry = createEntryWithTitle(title);
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);
            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));
        }

        @ParameterizedTest
        @CsvSource({
                "'JabRef bibliography management', 'https://www.shortscience.org/internalsearch?q=JabRef+bibliography+management'",
                "'Machine learning', 'https://www.shortscience.org/internalsearch?q=Machine+learning'",
        })
        void getShortScienceSearchURLLinksToSearchResults(String title, String expectedUrl) {
            BibEntry entry = createEntryWithTitle(title);
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);
            assertEquals(Optional.of(expectedUrl), url); // Use hard assertion if the underlying component works correctly
            assertTrue(url.get().startsWith(DEFAULT_SHORTSCIENCE_URL));
        }

        @ParameterizedTest
        @CsvSource({
                "'歷史書 📖 📚', 'q=%E6%AD%B7%E5%8F%B2%E6%9B%B8+%F0%9F%93%96+%F0%9F%93%9A'",
                "'    History Textbook   ', 'q=History+Textbook'",
                "'History%20Textbook', 'q=History%2520Textbook'", // Already encoded % is double-encoded
                "'A&B Research', 'q=A%26B+Research'"
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
            assertTrue(url.get().contains("author=John+Doe"));
        }

        @ParameterizedTest
        @CsvSource({
                "'Machine Learning', 'Smith & Jones', 'author=Smith+%26+Jones'",
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

    // --- Google Scholar Search Tests ---
    @Nested
    class GoogleScholarTests {

        @Test
        void getGoogleScholarSearchURLReturnsEmptyOnMissingTitle() {
            BibEntry entry = new BibEntry();
            assertEquals(Optional.empty(), linkCreator.getGoogleScholarSearchURL(entry));
        }

        @Test
        void getGoogleScholarSearchURLHandlesNullTitle() {
            BibEntry entry = new BibEntry().withField(StandardField.TITLE, null);
            assertEquals(Optional.empty(), linkCreator.getGoogleScholarSearchURL(entry));
        }

        @ParameterizedTest
        @CsvSource({
                "'JabRef bibliography management', 'https://scholar.google.com/scholar?q=JabRef+bibliography+management'",
                "'Machine learning', 'https://scholar.google.com/scholar?q=Machine+learning'"
        })
        void getGoogleScholarSearchURLLinksToSearchResults(String title, String expectedUrl) {
            BibEntry entry = createEntryWithTitle(title);
            Optional<String> url = linkCreator.getGoogleScholarSearchURL(entry);
            assertEquals(Optional.of(expectedUrl), url);
            assertTrue(url.get().startsWith(DEFAULT_GOOGLE_SCHOLAR_URL));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "!*'();:@&=+$,/?#[]",
                "100% Complete",
                "Question?"
        })
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
            assertTrue(url.get().contains("author=Alice+Smith"));
        }
    }

    // --- Custom Template and Fallback Tests ---
    @Nested
    class CustomTemplateTests {

        @Test
        void usesCustomTemplateWithTitlePlaceholder() {
            when(mockPreferences.getSearchEngineUrlTemplates())
                    .thenReturn(Map.of("Short Science", "https://custom.com/search?title={title}"));

            BibEntry entry = createEntryWithTitle("Test Title");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertEquals("https://custom.com/search?title=Test+Title", url.get());
            assertTrue(urlIsValid(url.get()));
        }

        @Test
        void usesCustomTemplateWithTitleAndAuthorPlaceholders() {
            when(mockPreferences.getSearchEngineUrlTemplates())
                    .thenReturn(Map.of("Short Science", "https://custom.com/search?t={title}&a={author}"));

            BibEntry entry = createEntryWithTitleAndAuthor("Test Title", "Test Author");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(url.get().contains("t=Test+Title"));
            assertTrue(url.get().contains("a=Test+Author"));
            assertTrue(urlIsValid(url.get()));
        }

        @Test
        void removesAuthorPlaceholderWhenAuthorAbsent() {
            when(mockPreferences.getSearchEngineUrlTemplates())
                    .thenReturn(Map.of("Short Science", "https://custom.com/search?t={title}&a={author}"));

            BibEntry entry = createEntryWithTitle("Test Title");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertFalse(url.get().contains("{author}"), "URL should not contain unresolved {author} placeholder");
            assertTrue(urlIsValid(url.get()));
        }

        @Test
        void fallsBackWhenTemplateMissingTitlePlaceholder() {
            // Template doesn't contain {title}, so it should fall back to default query param construction
            when(mockPreferences.getSearchEngineUrlTemplates())
                    .thenReturn(Map.of("Short Science", "https://custom.com/search"));

            BibEntry entry = createEntryWithTitle("Test Title");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            // Should fall back to the default URL, constructed with query params
            assertTrue(url.get().startsWith(DEFAULT_SHORTSCIENCE_URL));
            assertTrue(urlIsValid(url.get()));
        }
    }

    // --- Security and Injection Tests ---
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
            // Should fall back to the default secure HTTPS URL
            assertTrue(url.get().startsWith("https://"));
            assertTrue(urlIsValid(url.get()));
        }

        @Test
        void encodesPathTraversalInjectionAttempts() {
            BibEntry entry = createEntryWithTitle("../../etc/passwd");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));
            // The path traversal sequence should be URL-encoded (%2E%2E%2F)
            assertFalse(url.get().contains("../"));
        }

        @Test
        void handlesSqlInjectionAttempts() {
            BibEntry entry = createEntryWithTitle("'; DROP TABLE entries; --");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));
            // Should be properly encoded, preventing injection
            assertTrue(url.get().contains("q=%27%3B+DROP+TABLE+entries%3B+--"));
        }
    }

    // --- Edge Case Tests ---
    @Nested
    class EdgeCaseTests {

        @Test
        void handlesVeryLongTitles() {
            String longTitle = "A".repeat(1000);
            BibEntry entry = createEntryWithTitle(longTitle);
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));
        }

        @Test
        void handlesWhitespaceInTitles() {
            // Tests trimming and correct space encoding
            BibEntry entry = createEntryWithTitle("  Title with spaces  "); // Includes non-breaking spaces
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));
            // Should contain a single space encoding between words, not excessive whitespace or double encoding
            assertTrue(url.get().contains("q=Title+with+spaces"));
        }

        @Test
        void handlesNewlinesInTitle() {
            BibEntry entry = createEntryWithTitle("Title\nWith\nNewlines");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));
            // Newlines should be encoded, typically as %0A or +.
            assertFalse(url.get().contains("\n"));
        }
    }
}
