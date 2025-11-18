package org.jabref.logic.net;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.jabref.logic.util.URLUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class URLUtilTest {

    @ParameterizedTest
    @CsvSource(
            textBlock = """
                    # empty text
                    ''
                    ' '
                    # no URL
                    this is no url!
                    # no Google search URL
                    http://dl.acm.org/citation.cfm?id=321811
                    """
    )
    void cleanGoogleSearchUrlKeepsNonGoogleUrls(String input) {
        assertEquals(input, URLUtil.cleanGoogleSearchURL(input));
    }

    @ParameterizedTest
    @CsvSource(
            textBlock = """
                    # malformed Google URL
                    https://www.google.de/url♥
                    # no queries
                    https://www.google.de/url
                    https://www.google.de/url?
                    # no multiple queries
                    https://www.google.de/url?key=value
                    # no key values
                    https://www.google.de/url?key
                    https://www.google.de/url?url
                    https://www.google.de/url?key=
                    # no url param
                    https://www.google.de/url?key=value&key2=value2
                    # no url param value
                    https://www.google.de/url?url=
                    # url param value no URL
                    https://www.google.de/url?url=this+is+no+url
                    """
    )
    void cleanGoogleSearchUrlKeepsGoogleOnlyUrls(String input) {
        assertEquals(input, URLUtil.cleanGoogleSearchURL(input));
    }

    @ParameterizedTest
    @CsvSource(
            delimiterString = "->",
            textBlock = """
                    # Http
                    http://www.google.de/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjJurHd2sfHAhWBsxQKHSrEAaM&url=http%3A%2F%2Fmoz.com%2Fugc%2Fthe-ultimate-guide-to-the-google-search-parameters&ei=0THeVYmOJIHnUqqIh5gK&usg=AFQjCNHnid_r_d2LP8_MqvI7lQnTC3lB_g&sig2=ICzxDroG2ENTJSUGmdhI2w -> http://moz.com/ugc/the-ultimate-guide-to-the-google-search-parameters
                    # Https
                    https://www.google.de/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjJurHd2sfHAhWBsxQKHSrEAaM&url=https%3A%2F%2Fmoz.com%2Fugc%2Fthe-ultimate-guide-to-the-google-search-parameters&ei=0THeVYmOJIHnUqqIh5gK&usg=AFQjCNHnid_r_d2LP8_MqvI7lQnTC3lB_g&sig2=ICzxDroG2ENTJSUGmdhI2w -> https://moz.com/ugc/the-ultimate-guide-to-the-google-search-parameters
                    # root domain
                    https://google.de/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjJurHd2sfHAhWBsxQKHSrEAaM&url=https%3A%2F%2Fmoz.com%2Fugc%2Fthe-ultimate-guide-to-the-google-search-parameters&ei=0THeVYmOJIHnUqqIh5gK&usg=AFQjCNHnid_r_d2LP8_MqvI7lQnTC3lB_g&sig2=ICzxDroG2ENTJSUGmdhI2w -> https://moz.com/ugc/the-ultimate-guide-to-the-google-search-parameters
                    # foreign domain
                    https://www.google.fr/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjJurHd2sfHAhWBsxQKHSrEAaM&url=https%3A%2F%2Fmoz.com%2Fugc%2Fthe-ultimate-guide-to-the-google-search-parameters&ei=0THeVYmOJIHnUqqIh5gK&usg=AFQjCNHnid_r_d2LP8_MqvI7lQnTC3lB_g&sig2=ICzxDroG2ENTJSUGmdhI2w -> https://moz.com/ugc/the-ultimate-guide-to-the-google-search-parameters
                    # foreign domain co.uk
                    https://www.google.co.uk/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjJurHd2sfHAhWBsxQKHSrEAaM&url=https%3A%2F%2Fmoz.com%2Fugc%2Fthe-ultimate-guide-to-the-google-search-parameters&ei=0THeVYmOJIHnUqqIh5gK&usg=AFQjCNHnid_r_d2LP8_MqvI7lQnTC3lB_g&sig2=ICzxDroG2ENTJSUGmdhI2w -> https://moz.com/ugc/the-ultimate-guide-to-the-google-search-parameters
                    # accept ftp results
                    https://www.google.fr/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjJurHd2sfHAhWBsxQKHSrEAaM&url=ftp%3A%2F%2Fmoz.com%2Fugc%2Fthe-ultimate-guide-to-the-google-search-parameters&ei=0THeVYmOJIHnUqqIh5gK&usg=AFQjCNHnid_r_d2LP8_MqvI7lQnTC3lB_g&sig2=ICzxDroG2ENTJSUGmdhI2w -> ftp://moz.com/ugc/the-ultimate-guide-to-the-google-search-parameters
                    """
    )
    void cleanGoogleSearchURLShouldReturnTargetURL(String input, String expected) {
        assertEquals(expected, URLUtil.cleanGoogleSearchURL(input));
    }

    @ParameterizedTest
    @CsvSource(
            textBlock = """
                    http://www.google.com
                    https://www.google.com
                    """
    )
    void isURLshouldAcceptValidURL(String url) {
        assertTrue(URLUtil.isURL(url));
    }

    @ParameterizedTest
    @CsvSource(
            textBlock = """
                    www.google.com
                    google.com
                    """
    )
    void isURLshouldRejectInvalidURL(String url) {
        assertFalse(URLUtil.isURL(url));
    }

    @Test
    void isURLshouldRejectEmbeddedURL() {
        assertFalse(URLUtil.isURL("dblp computer science bibliography, http://dblp.org"));
    }

    @Test
    void createUriShouldHandlePipeCharacter() {
        String input = "http://example.com/test|file";
        URI uri = URLUtil.createUri(input);
        assertEquals("http://example.com/test%7Cfile", uri.toString());
    }

    @Test
    void validUrl() throws MalformedURLException {
        String input = "http://example.com";

        URL result = URLUtil.create(input);
        assertNotNull(result);
        assertNotEquals("", result.toString().trim());
        assertEquals(input, result.toString());
    }

    @Test
    void emptyUrl() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                URLUtil.create("   "));
        assertTrue(exception.getMessage().contains("URL must not be empty."));
    }

    @Test
    void urlStartingWithWww() throws MalformedURLException {
        // URLs starting with www. should be prefixed with https://
        URL result = URLUtil.create("www.example.com");
        assertNotNull(result);
        assertEquals("https://www.example.com", result.toString());
    }

    @Test
    void uriMissingSchemeAndNotStartingWithWww() {
        // URLs not starting with www. and without a scheme should still throw an exception
        MalformedURLException exception = assertThrows(MalformedURLException.class, () ->
                URLUtil.create("example.com"));
        assertTrue(exception.getMessage().contains("not absolute"));
    }

    @Test
    void uriMissingHost() {
        MalformedURLException exception = assertThrows(MalformedURLException.class, () ->
                URLUtil.create("mailto:someone@example.com"));
        assertTrue(exception.getMessage().contains("must include both scheme and host"));
    }

    @Test
    void malformedSyntax() {
        MalformedURLException exception = assertThrows(MalformedURLException.class, () ->
                URLUtil.create("http://[invalid-url]"));
        assertTrue(exception.getMessage().contains("Invalid URI"));
    }
}
