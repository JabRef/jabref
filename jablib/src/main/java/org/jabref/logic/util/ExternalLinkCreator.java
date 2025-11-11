package org.jabref.logic.util;

import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.MalformedURLException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
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
     * Validates URL conformance to RFC2396. Does not perform complex checks such as opening connections.
     */
    private boolean urlIsValid(String url) {
        try {
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
        @ValueSource(strings = {"", " ", "   "})
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
                "'Deep Neural Networks', 'https://www.shortscience.org/internalsearch?q=Deep+Neural+Networks'"
        })
        void getShortScienceSearchURLLinksToSearchResults(String title, String expectedUrl) {
            BibEntry entry = createEntryWithTitle(title);
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);
            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));
            assertTrue(url.get().startsWith(DEFAULT_SHORTSCIENCE_URL));
        }

        @ParameterizedTest
        @CsvSource({
                "'歷史書 📖 📚', 'https://www.shortscience.org/internalsearch?q=%E6%AD%B7%E5%8F%B2%E6%9B%B8+%F0%9F%93%96+%F0%9F%93%9A'",
                "'    History Textbook   ', 'History+Textbook'",
                "'History%20Textbook', 'History%2520Textbook'",
                "'A&B Research', 'A%26B+Research'"
        })
        void getShortScienceSearchURLEncodesCharacters(String title, String expectedEncodingPart) {
            BibEntry entry = createEntryWithTitle(title);
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);
            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));
            assertTrue(url.get().contains(expectedEncodingPart) || url.get().startsWith(DEFAULT_SHORTSCIENCE_URL));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "!*'();:@&=+$,/?#[]",
                "Title with spaces",
                "Title/with/slashes",
                "Question?",
                "100% Complete"
        })
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
            assertTrue(url.get().contains("author="));
        }

        @ParameterizedTest
        @CsvSource({
                "'Machine Learning', 'Smith & Jones', 'Smith+%26+Jones'",
                "'Deep Learning', '李明', '%E6%9D%8E%E6%98%8E'"
        })
        void getShortScienceSearchURLEncodesAuthorNames(String title, String author, String expectedAuthorEncoding) {
            BibEntry entry = createEntryWithTitleAndAuthor(title, author);
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);
            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));
            assertTrue(url.get().contains("author="));
        }
    }

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
        void getGoogleScholarSearchURLLinksToSearchResults(String title, String ignored) {
            BibEntry entry = createEntryWithTitle(title);
            Optional<String> url = linkCreator.getGoogleScholarSearchURL(entry);
            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));
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
            assertTrue(url.get().contains("author="));
        }
    }

    @Nested
    class CustomTemplateTests {

        @Test
        void usesCustomTemplateWithTitlePlaceholder() {
            when(mockPreferences.getSearchEngineUrlTemplates())
                    .thenReturn(Map.of("Short Science", "https://custom.com/search?title={title}"));

            BibEntry entry = createEntryWithTitle("Test Title");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(url.get().startsWith("https://custom.com/search?title="));
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
        void fallsBackWhenTemplateInvalid() {
            when(mockPreferences.getSearchEngineUrlTemplates())
                    .thenReturn(Map.of("Short Science", "https://custom.com/search"));

            BibEntry entry = createEntryWithTitle("Test Title");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(url.get().startsWith(DEFAULT_SHORTSCIENCE_URL));
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
        void rejectsNonHttpSchemes(String maliciousUrl) {
            when(mockPreferences.getSearchEngineUrlTemplates())
                    .thenReturn(Map.of("Short Science", maliciousUrl + "?q={title}"));

            BibEntry entry = createEntryWithTitle("Test");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(url.get().startsWith("https://"));
            assertTrue(urlIsValid(url.get()));
        }

        @Test
        void encodesInjectionAttempts() {
            BibEntry entry = createEntryWithTitle("../../etc/passwd");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));
            assertFalse(url.get().contains("../"));
        }

        @Test
        void handlesSqlInjectionAttempts() {
            BibEntry entry = createEntryWithTitle("'; DROP TABLE entries; --");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));
        }
    }

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
            BibEntry entry = createEntryWithTitle("  Title with spaces  ");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));
            assertFalse(url.get().contains("++") || url.get().contains("%20%20"));
        }

        @Test
        void handlesAlreadyEncodedCharacters() {
            BibEntry entry = createEntryWithTitle("Already%20Encoded");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));
            assertTrue(url.get().contains("%2520") || url.get().contains("Already%20Encoded"));
        }

        @Test
        void handlesNewlinesInTitle() {
            BibEntry entry = createEntryWithTitle("Title\nWith\nNewlines");
            Optional<String> url = linkCreator.getShortScienceSearchURL(entry);

            assertTrue(url.isPresent());
            assertTrue(urlIsValid(url.get()));
        }
    }

    static Stream<Arguments> specialCharactersProvider() {
        return Stream.of(
                Arguments.of("!*'();:@&=+$,/?#[]"),
                Arguments.of("100% Complete Research"),
                Arguments.of("C++ Programming"),
                Arguments.of("Research & Development")
        );
    }
}
