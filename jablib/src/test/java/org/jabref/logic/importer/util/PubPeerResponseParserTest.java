package org.jabref.logic.importer.util;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.pubpeer.PubPeerFeedback;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@NullMarked
class PubPeerResponseParserTest {
    private static final String RESPONSE = """
            {"status":"good","feedbacks":[{
              "id":"10.1234/EXAMPLE",
              "title":"Example publication",
              "url":"https://pubpeer.com/publications/EXAMPLE",
              "total_comments":2,
              "users":"Peer 1, Peer 2, ",
              "last_commented_at":%s,
              "updates":[]
            }]}
            """;

    private final PubPeerResponseParser parser = new PubPeerResponseParser();

    @ParameterizedTest
    @ValueSource(strings = {
            "\"2026-08-13 02:29:54\"",
            "\"2026-08-13 02:29:54.000000\"",
            "{\"date\":\"2026-08-13 02:29:54.000000\",\"timezone\":\"UTC\",\"timezone_type\":3}"
    })
    void parsesCurrentAndLegacyFeedback(String timestamp) throws FetcherException {
        PubPeerFeedback expected = new PubPeerFeedback(
                new DOI("10.1234/example"), "Example publication", URI.create("https://pubpeer.com/publications/EXAMPLE"),
                2, List.of("Peer 1", "Peer 2"), Optional.of(LocalDateTime.of(2026, 8, 13, 2, 29, 54)));

        assertEquals(List.of(expected), parser.parse(RESPONSE.formatted(timestamp)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"\""})
    void absentTimestampIsOptional(String timestamp) throws FetcherException {
        assertEquals(Optional.empty(), parser.parse(RESPONSE.formatted(timestamp)).getFirst().lastCommentedAt());
    }

    @Test
    void missingTimestampIsOptional() throws FetcherException {
        assertEquals(Optional.empty(), parser.parse(RESPONSE.formatted("null").replace("\"last_commented_at\":null,", "")).getFirst().lastCommentedAt());
    }

    @Test
    void noFeedbackReturnsEmptyList() throws FetcherException {
        assertEquals(List.of(), parser.parse("{\"status\":\"good\",\"feedbacks\":[]}"));
    }

    @Test
    void emptyUsersReturnsEmptyList() throws FetcherException {
        assertEquals(List.of(), parser.parse(RESPONSE.formatted("null").replace("Peer 1, Peer 2, ", "")).getFirst().users());
    }

    @Test
    void parsesMultiplePublications() throws FetcherException {
        String response = RESPONSE.formatted("null");
        String publication = response.substring(response.indexOf('[') + 1, response.lastIndexOf(']'));

        assertEquals(2, parser.parse("{\"status\":\"good\",\"feedbacks\":[" + publication + "," + publication.replace("10.1234/EXAMPLE", "10.1234/other") + "]}").size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "null", "[]", "42", "not json", "{}", "{\"status\":\"good\"}", "{\"status\":\"bad\",\"feedbacks\":[]}", "{\"status\":\"good\",\"feedbacks\":{}}"})
    void rejectsInvalidResponseInsteadOfReportingNoComments(String response) {
        assertThrows(FetcherException.class, () -> parser.parse(response));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"status\":null,\"feedbacks\":[]}", "{\"status\":{},\"feedbacks\":[]}", "{\"status\":\"good\",\"feedbacks\":null}"})
    void rejectsInvalidEnvelopeTypes(String response) {
        assertThrows(FetcherException.class, () -> parser.parse(response));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\"id\":\"10.1234/EXAMPLE\"",
            "\"title\":\"Example publication\"",
            "\"url\":\"https://pubpeer.com/publications/EXAMPLE\"",
            "\"total_comments\":2",
            "\"users\":\"Peer 1, Peer 2, \""
    })
    void rejectsNullRequiredFields(String field) {
        String response = RESPONSE.formatted("null").replace(field, field.substring(0, field.indexOf(':') + 1) + "null");

        assertThrows(FetcherException.class, () -> parser.parse(response));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"invalid date\"", "42", "{}"})
    void rejectsMalformedTimestamp(String timestamp) {
        assertThrows(FetcherException.class, () -> parser.parse(RESPONSE.formatted(timestamp)));
    }

    @Test
    void rejectsInvalidDoi() {
        assertThrows(FetcherException.class, () -> parser.parse(RESPONSE.formatted("null").replace("10.1234/EXAMPLE", "invalid")));
    }

    @Test
    void rejectsNegativeCommentCount() {
        assertThrows(FetcherException.class, () -> parser.parse(RESPONSE.formatted("null").replace("\"total_comments\":2", "\"total_comments\":-1")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "/publications/EXAMPLE", "https://example.org/publications/EXAMPLE", "https://pubpeer.com/invalid space"})
    void rejectsInvalidPublicationLink(String url) {
        assertThrows(FetcherException.class, () -> parser.parse(RESPONSE.formatted("null").replace("https://pubpeer.com/publications/EXAMPLE", url)));
    }
}
