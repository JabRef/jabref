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
        when(mockPreferences.getSearchEngineUrlTemplates()).thenReturn(Map.of());
        linkCreator = new ExternalLinkCreator(mockPreferences);
    }

    /**
     * Validates URL conformance to RFC 2396 using standard Java URI parsing.
     */
    private boolean urlIsValid(String url) {
        try {
            // URI.create throws IllegalArgumentException if the string violates RFC 2396
            URI.create(url).toURL();
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

    // --- ShortScience Search Tests ---
    @Nested
    class ShortScienceTests {

        @Test
        void getShortScienceSearchURLReturnsEmptyOnMissingTitle() {
            BibEntry entry = new BibEntry();
            assertEquals(Optional.empty(), linkCreator.getShortScienceSearchURL(entry));
        }

        @ParameterizedTest
        @CsvSource({
                "'', 'https://www.shortscience.org/internalsearch?q='",
                "' ', 'https://www.shortscience.org/internalsearch?q='",
                "'   ', 'https://www.shortscience.org/internalsearch?q=%C2%A0%20%C2%A0'"
        })
        void getShortScienceSearchURLHandlesEmptyOrWhitespaceTitles(String title, String expectedUrl) {
            BibEntry entry = createEntryWithTitle(title);
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertEquals(expectedUrl, url.get());
        }

        @ParameterizedTest
        @CsvSource({
                // URIBuilder uses Percent-Encoding (%20), not Form-Encoding (+)
                "'JabRef bibliography management', 'https://www.shortscience.org/internalsearch?q=JabRef%20bibliography%20management'",
                "'Machine learning', 'https://www.shortscience.org/internalsearch?q=Machine%20learning'",
        })
        void getShortScienceSearchURLLinksToSearchResults(String title, String expectedUrl) {
            BibEntry entry = createEntryWithTitle(title);
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertEquals(Optional.of(expectedUrl), url);
            assertTrue(url.get().startsWith(DEFAULT_SHORTSCIENCE_URL));
        }

        @ParameterizedTest
        @CsvSource({
                // Unicode characters are percent-encoded
                "'歷史書 📖 📚', 'q=%E6%AD%B7%E5%8F%B2%E6%9B%B8%20%F0%9F%93%96%20%F0%9F%93%9A'",

                // Non-Breaking Spaces (NBSP, \u00A0) are NOT trimmed by Java's trim() and are encoded as %C2%A0
                "'    History Textbook   ', 'q=%C2%A0%20%C2%A0%20History%20Textbook%C2%A0%20%C2%A0'",

                // Literal % must be encoded as %25
                "'History%20Textbook', 'q=History%2520Textbook'",

                // Literal & must be encoded as %26
                "'A&B Research', 'q=A%26B%20Research'"
        })
        void getShortScienceSearchURLEncodesCharacters(String title, String expectedQueryPart) {
            BibEntry entry = createEntryWithTitle(title);
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()), "Generated URL " + url.get() + " is not RFC 2396 compliant");
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

    // --- Google Scholar Search Tests ---
    @Nested
    class GoogleScholarTests {

        @Test
        void getGoogleScholarSearchURLReturnsEmptyOnMissingTitle() {
            BibEntry entry = new BibEntry();
            assertEquals(Optional.empty(), linkCreator.getGoogleScholarSearchURL(entry));
        }

        @ParameterizedTest
        @CsvSource({
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

    // --- Custom Template and Fallback Tests ---
    @Nested
    class CustomTemplateTests {

        @Test
        void usesCustomTemplateWithTitlePlaceholder() {
            // Here the code uses strict URLEncoder.encode (StandardCharsets.UTF_8), which produces "+"
            when(mockPreferences.getSearchEngineUrlTemplates())
                    .thenReturn(Map.of("Short Science", "https://custom.com/search?title={title}"));

            BibEntry entry = createEntryWithTitle("Test Title");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            // Template logic explicitly calls URLEncoder, so we expect "+" here
            assertEquals("https://custom.com/search?title=Test+Title", url.get());
            assertTrue(urlIsValid(url.get()));
        }

        @Test
        void fallsBackWhenTemplateMissingTitlePlaceholder() {
            // Template lacks {title}, triggers fallback to default URL logic (URIBuilder -> %20)
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
            // Must fall back to the default valid HTTPS URL
            assertTrue(url.get().startsWith("https://"));
            assertTrue(urlIsValid(url.get()));
        }

        @Test
        void handlesSqlInjectionAttempts() {
            BibEntry entry = createEntryWithTitle("'; DROP TABLE entries; --");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));

            // 1. LatexToUnicodeAdapter converts " --" (latex dash) to " –" (unicode en-dash)
            // 2. URIBuilder encodes:
            //    ' -> %27
            //    ; -> %3B
            //    Space -> %20
            //    En-dash (–) -> %E2%80%93
            assertTrue(url.get().contains("q=%27%3B%20DROP%20TABLE%20entries%3B%20%E2%80%93"));
        }
    }
}
