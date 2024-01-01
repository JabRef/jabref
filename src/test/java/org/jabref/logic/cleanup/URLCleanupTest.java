package org.jabref.logic.cleanup;

import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class URLCleanupTest {

    @ParameterizedTest
    @MethodSource("provideURL")
    public void testChangeURL(BibEntry expected, BibEntry urlInputField) {
        URLCleanup cleanUp = new URLCleanup();
        cleanUp.cleanup(urlInputField);

        assertEquals(expected, urlInputField);
    }

    private static Stream<Arguments> provideURL() {
        return Stream.of(

            // Input Note field has two arguments stored , with the latter being a url.
            Arguments.of(
                new BibEntry().withField(StandardField.URL,
                                    "https://hdl.handle.net/10442/hedi/6089")
                              .withField(StandardField.NOTE,
                                    "this is a note"),
                new BibEntry().withField(StandardField.NOTE,
                                    "this is a note, \\url{https://hdl.handle.net/10442/hedi/6089}")),

            // Input Note field has two arguments stored, with the former being a url.
            Arguments.of(
                new BibEntry().withField(StandardField.URL,
                                    "https://hdl.handle.net/10442/hedi/6089")
                              .withField(StandardField.NOTE,
                                    "this is a note"),
                new BibEntry().withField(StandardField.NOTE,
                                    "\\url{https://hdl.handle.net/10442/hedi/6089}, this is a note")),

            // Input Note field has more than one URLs stored.
            Arguments.of(
                new BibEntry().withField(StandardField.URL,
                                    "https://hdl.handle.net/10442/hedi/6089")
                              .withField(StandardField.NOTE,
                                    "\\url{http://142.42.1.1:8080}"),
                new BibEntry().withField(StandardField.NOTE,
                                    "\\url{https://hdl.handle.net/10442/hedi/6089}, "
                                        + "\\url{http://142.42.1.1:8080}")),

            // Input entry holds the same URL both in Note and Url field.
            Arguments.of(
                new BibEntry().withField(StandardField.URL,
                                     "https://hdl.handle.net/10442/hedi/6089"),
                new BibEntry().withField(StandardField.NOTE,
                                     "\\url{https://hdl.handle.net/10442/hedi/6089}")
                              .withField(StandardField.URL,
                                     "https://hdl.handle.net/10442/hedi/6089")),

            // Input Note field has several values stored.
            Arguments.of(
                new BibEntry().withField(StandardField.URL,
                                    "https://example.org")
                              .withField(StandardField.NOTE,
                                    "cited by Kramer, 2002."),
                new BibEntry().withField(StandardField.NOTE,
                                    "\\url{https://example.org}, cited by Kramer, 2002.")),

            /*
             * Several input URL types (e.g, not secure protocol, password included for
             * authentication, IP address, port etc.) to be correctly identified.
             */
            Arguments.of(
                new BibEntry().withField(StandardField.URL,
                                    "https://hdl.handle.net/10442/hedi/6089"),
                new BibEntry().withField(StandardField.NOTE,
                                    "\\url{https://hdl.handle.net/10442/hedi/6089}")),

            Arguments.of(
                new BibEntry().withField(StandardField.URL,
                                    "http://hdl.handle.net/10442/hedi/6089"),
                new BibEntry().withField(StandardField.NOTE,
                                    "\\url{http://hdl.handle.net/10442/hedi/6089}")),

            Arguments.of(
                new BibEntry().withField(StandardField.URL,
                                    "http://userid:password@example.com:8080"),
                new BibEntry().withField(StandardField.NOTE,
                                    "\\url{http://userid:password@example.com:8080}")),

            Arguments.of(
                new BibEntry().withField(StandardField.URL,
                                    "http://142.42.1.1:8080"),
                new BibEntry().withField(StandardField.NOTE,
                                    "\\url{http://142.42.1.1:8080}")),

            Arguments.of(
                new BibEntry().withField(StandardField.URL,
                                    "http://☺.damowmow.com"),
                new BibEntry().withField(StandardField.NOTE,
                                    "\\url{http://☺.damowmow.com}")),

            Arguments.of(
                new BibEntry().withField(StandardField.URL,
                                    "http://-.~_!$&'()*+,;=:%40:80%2f::::::@example.com"),
                new BibEntry().withField(StandardField.NOTE,
                                    "\\url{http://-.~_!$&'()*+,;=:%40:80%2f::::::@example.com}")),

            Arguments.of(
                new BibEntry().withField(StandardField.URL,
                                    "https://www.example.com/foo/?bar=baz&inga=42&quux"),
                new BibEntry().withField(StandardField.NOTE,
                                    "\\url{https://www.example.com/foo/?bar=baz&inga=42&quux}")),

            Arguments.of(
                new BibEntry().withField(StandardField.URL,
                                    "https://www.example.com/foo/?bar=baz&inga=42&quux"),
                new BibEntry().withField(StandardField.NOTE,
                                    "https://www.example.com/foo/?bar=baz&inga=42&quux")),

            // Expected entry returns formatted the url-date in the Urldate field.
            Arguments.of(
                new BibEntry().withField(StandardField.URL,
                                    "http://142.42.1.1:8080")
                              .withField(StandardField.URLDATE,
                                    "2021-01-15"),
                new BibEntry().withField(StandardField.NOTE,
                                    "\\url{http://142.42.1.1:8080}, accessed on January 15, 2021")),

            // Input entry doesn't hold any URL in the Note field.
            Arguments.of(
                new BibEntry().withField(StandardField.NOTE,
                                    "Accessed on 2015-01-15"),
                new BibEntry().withField(StandardField.NOTE,
                                    "Accessed on 2015-01-15")),

            // Input entry has multiple url-dates stored in the Note field.
            Arguments.of(
                new BibEntry().withField(StandardField.URL,
                                    "http://142.42.1.1:8080")
                              .withField(StandardField.URLDATE,
                                    "2021-01-15")
                              .withField(StandardField.NOTE,
                                    "visited on February 12, 2017"),
                new BibEntry().withField(StandardField.NOTE,
                                    "\\url{http://142.42.1.1:8080}, accessed on January 15, 2021, visited on February 12, 2017")),

            // Input entry holds the same url-date in both Note and Urldate field.
            Arguments.of(
                new BibEntry().withField(StandardField.URL,
                                    "http://142.42.1.1:8080")
                              .withField(StandardField.URLDATE,
                                    "2015-01-15"),
                new BibEntry().withField(StandardField.NOTE,
                                    "\\url{http://142.42.1.1:8080}, visited on 2015.01.15")
                              .withField(StandardField.URLDATE,
                                    "2015-01-15")),

            // Input Note field has several values stored.
            Arguments.of(
                new BibEntry().withField(StandardField.URL,
                                    "https://example.org")
                              .withField(StandardField.URLDATE,
                                    "2023-04-11")
                              .withField(StandardField.NOTE,
                                    "cited by Kramer"),
                new BibEntry().withField(StandardField.NOTE,
                                    "\\url{https://example.org}, cited by Kramer, accessed on 2023-04-11"),
            // test with no fields present
            Arguments.of(
                    new BibEntry(),
                    new BibEntry())
            )
        );
    }
}
