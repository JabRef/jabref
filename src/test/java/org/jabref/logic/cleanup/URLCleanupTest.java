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
        /*
         * Expected entries with various types of URLs(e.g, not secure protocol, password included for
         * authentication, IP address, port etc.)
         */
        BibEntry[] expectedwithURL = {
                new BibEntry().withField(StandardField.URL, "https://hdl.handle.net/10442/hedi/6089"),
                new BibEntry().withField(StandardField.URL, "http://hdl.handle.net/10442/hedi/6089"),
                new BibEntry().withField(StandardField.URL, "http://userid:password@example.com:8080"),
                new BibEntry().withField(StandardField.URL, "http://142.42.1.1:8080"),
                new BibEntry().withField(StandardField.URL, "http://☺.damowmow.com"),
                new BibEntry().withField(StandardField.URL, "http://-.~_!$&'()*+,;=:%40:80%2f::::::@example.com"),
                new BibEntry().withField(StandardField.URL, "https://www.example.com/foo/?bar=baz&inga=42&quux"),
        };

        // Expected entry in case note field holds more than two values
        BibEntry expectedWithTwoNoteValues = new BibEntry()
                .withField(StandardField.URL, "https://hdl.handle.net/10442/hedi/6089")
                .withField(StandardField.NOTE, "this is a note");

        // Expected entry in case note field holds more than one URLs
        BibEntry expectedWithMultipleURLs = new BibEntry()
                .withField(StandardField.URL, "https://hdl.handle.net/10442/hedi/6089")
                .withField(StandardField.NOTE, "\\url{http://142.42.1.1:8080}");

        return Stream.of(

                // Input Note field has two arguments stored , with the latter being a url
                Arguments.of(expectedWithTwoNoteValues, new BibEntry().withField(
                        StandardField.NOTE, "this is a note, \\url{https://hdl.handle.net/10442/hedi/6089}")),

                // Input Note field has two arguments stored, with the former being a url
                Arguments.of(expectedWithTwoNoteValues, new BibEntry().withField(
                        StandardField.NOTE, "\\url{https://hdl.handle.net/10442/hedi/6089}, this is a note")),

                // Input Note field has more than one URLs stored
                Arguments.of(expectedWithMultipleURLs, new BibEntry().withField(
                        StandardField.NOTE, "\\url{https://hdl.handle.net/10442/hedi/6089}, \\url{http://142.42.1.1:8080}")),

                // Several input URL types to be correctly identified
                Arguments.of(expectedwithURL[0], new BibEntry().withField(
                        StandardField.NOTE, "\\url{https://hdl.handle.net/10442/hedi/6089}")),

                Arguments.of(expectedwithURL[1], new BibEntry().withField(
                        StandardField.NOTE, "\\url{http://hdl.handle.net/10442/hedi/6089}")),

                Arguments.of(expectedwithURL[2], new BibEntry().withField(
                        StandardField.NOTE, "\\url{http://userid:password@example.com:8080}")),

                Arguments.of(expectedwithURL[3], new BibEntry().withField(
                        StandardField.NOTE, "\\url{http://142.42.1.1:8080}")),

                Arguments.of(expectedwithURL[4], new BibEntry().withField(
                        StandardField.NOTE, "\\url{http://☺.damowmow.com}")),

                Arguments.of(expectedwithURL[5], new BibEntry().withField(
                        StandardField.NOTE, "\\url{http://-.~_!$&'()*+,;=:%40:80%2f::::::@example.com}")),

                Arguments.of(expectedwithURL[6], new BibEntry().withField(
                        StandardField.NOTE, "\\url{https://www.example.com/foo/?bar=baz&inga=42&quux}")),

                Arguments.of(expectedwithURL[6], new BibEntry().withField(
                        StandardField.NOTE, "https://www.example.com/foo/?bar=baz&inga=42&quux"))
        );
    }
}
