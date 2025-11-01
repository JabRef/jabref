package org.jabref.logic.util;

import java.net.MalformedURLException;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.jabref.logic.util.ExternalLinkCreator.getGoogleScholarSearchURL;
import static org.jabref.logic.util.ExternalLinkCreator.getSemanticScholarSearchURL;
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

    static Stream<Arguments> specialCharactersProvider() {
        return Stream.of(
                Arguments.of("!*'();:@&=+$,/?#[]")
        );
    }

    @ParameterizedTest
    @MethodSource("specialCharactersProvider")
    void getShortScienceSearchURLEncodesSpecialCharacters(String title) {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, title);
        Optional<String> url = getShortScienceSearchURL(entry);
        assertTrue(url.isPresent());
        assertTrue(urlIsValid(url.get()));
    }

    @ParameterizedTest
    @MethodSource("specialCharactersProvider")
    void getGoogleScholarSearchURLEncodesSpecialCharacters(String title) {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, title);
        Optional<String> url = getGoogleScholarSearchURL(entry);
        assertTrue(url.isPresent());
        assertTrue(urlIsValid(url.get()));
    }

    @ParameterizedTest
    @MethodSource("specialCharactersProvider")
    void getSemanticScholarSearchURLEncodesSpecialCharacters(String title) {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, title);
        Optional<String> url = getSemanticScholarSearchURL(entry);
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
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, title);
        Optional<String> url = getShortScienceSearchURL(entry);
        assertEquals(Optional.of(expectedUrl), url);
    }

    @ParameterizedTest
    @CsvSource({
            "'歷史書 📖 📚', 'https://scholar.google.com/scholar?q=%E6%AD%B7%E5%8F%B2%E6%9B%B8%20%F0%9F%93%96%20%F0%9F%93%9A'",
            "'    History Textbook   ', 'https://scholar.google.com/scholar?q=History%20Textbook'",
            "'History%20Textbook', 'https://scholar.google.com/scholar?q=History%2520Textbook'",
            "'JabRef bibliography management', 'https://scholar.google.com/scholar?q=JabRef%20bibliography%20management'"
    })
    void getGoogleScholarSearchURLEncodesCharacters(String title, String expectedUrl) {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, title);
        Optional<String> url = getGoogleScholarSearchURL(entry);
        assertEquals(Optional.of(expectedUrl), url);
    }

    @ParameterizedTest
    @CsvSource({
            "'歷史書 📖 📚', 'https://www.semanticscholar.org/search?q=%E6%AD%B7%E5%8F%B2%E6%9B%B8%20%F0%9F%93%96%20%F0%9F%93%9A'",
            "'    History Textbook   ', 'https://www.semanticscholar.org/search?q=History%20Textbook'",
            "'History%20Textbook', 'https://www.semanticscholar.org/search?q=History%2520Textbook'",
            "'JabRef bibliography management', 'https://www.semanticscholar.org/search?q=JabRef%20bibliography%20management'"
    })
    void getSemanticScholarSearchURLEncodesCharacters(String title, String expectedUrl) {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, title);
        Optional<String> url = getSemanticScholarSearchURL(entry);
        assertEquals(Optional.of(expectedUrl), url);
    }

    @Test
    void getShortScienceSearchURLReturnsEmptyOnMissingTitle() {
        BibEntry entry = new BibEntry();
        assertEquals(Optional.empty(), getShortScienceSearchURL(entry));
    }

    @Test
    void getGoogleScholarSearchURLReturnsEmptyOnMissingTitle() {
        BibEntry entry = new BibEntry();
        assertEquals(Optional.empty(), getGoogleScholarSearchURL(entry));
    }

    @Test
    void getSemanticScholarSearchURLReturnsEmptyOnMissingTitle() {
        BibEntry entry = new BibEntry();
        assertEquals(Optional.empty(), getSemanticScholarSearchURL(entry));
    }

    @Test
    void getShortScienceSearchURLWithoutLaTeX() {
        BibEntry entry = new BibEntry();
        entry.withField(StandardField.TITLE, "{The Difference Between Graph-Based and Block-Structured Business Process Modelling Languages}");

        Optional<String> url = getShortScienceSearchURL(entry);

        String expectedUrl = "https://www.shortscience.org/internalsearch?q=The%20Difference%20Between%20Graph-Based%20and%20Block-Structured%20Business%20Process%20Modelling%20Languages";
        assertEquals(Optional.of(expectedUrl), url);
    }

    @Test
    void getGoogleScholarSearchURLWithoutLaTeX() {
        BibEntry entry = new BibEntry();
        entry.withField(StandardField.TITLE, "{The Difference Between Graph-Based and Block-Structured Business Process Modelling Languages}");

        Optional<String> url = getGoogleScholarSearchURL(entry);

        String expectedUrl = "https://scholar.google.com/scholar?q=The%20Difference%20Between%20Graph-Based%20and%20Block-Structured%20Business%20Process%20Modelling%20Languages";
        assertEquals(Optional.of(expectedUrl), url);
    }

    @Test
    void getSemanticScholarSearchURLWithoutLaTeX() {
        BibEntry entry = new BibEntry();
        entry.withField(StandardField.TITLE, "{The Difference Between Graph-Based and Block-Structured Business Process Modelling Languages}");

        Optional<String> url = getSemanticScholarSearchURL(entry);

        String expectedUrl = "https://www.semanticscholar.org/search?q=The%20Difference%20Between%20Graph-Based%20and%20Block-Structured%20Business%20Process%20Modelling%20Languages";
        assertEquals(Optional.of(expectedUrl), url);
    }
}
