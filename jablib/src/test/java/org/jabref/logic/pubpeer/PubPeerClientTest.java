package org.jabref.logic.pubpeer;

import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.util.PubPeerClient;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.pubpeer.PubPeerFeedback;
import org.jabref.support.ExternalServicesTest;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExternalServicesTest
@NullMarked
class PubPeerClientTest {
    private String apiKey = "PubPeerZotero";

    @BeforeEach
    void setUp() {
        apiKey = System.getenv().getOrDefault("PUBPEER_API_KEY", "PubPeerZotero");
        assumeTrue(!apiKey.isBlank(), "Set PUBPEER_API_KEY to run live PubPeer tests");
    }

    @Test
    void retrievesFeedbackForKnownPublication() throws FetcherException {
        DOI doi = new DOI("10.1080/15548627.2015.1100356");
        List<PubPeerFeedback> result = new PubPeerClient(apiKey).fetchFeedback(List.of(doi));

        assertEquals(1, result.size());
        PubPeerFeedback feedback = result.getFirst();
        assertEquals(doi, feedback.doi());
        assertEquals("pubpeer.com", feedback.url().getHost());
        assertTrue(feedback.totalComments() > 0);
        assertFalse(feedback.users().isEmpty());
        assertTrue(feedback.lastCommentedAt().isPresent());
    }

    @Test
    void unknownPublicationHasNoFeedback() throws FetcherException {
        assertEquals(List.of(), new PubPeerClient(apiKey).fetchFeedback(List.of(new DOI("10.0000/jabref-pubpeer-missing"))));
    }
}
