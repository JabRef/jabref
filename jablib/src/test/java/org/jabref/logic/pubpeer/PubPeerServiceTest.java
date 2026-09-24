package org.jabref.logic.pubpeer;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.util.PubPeerClient;
import org.jabref.logic.importer.util.PubPeerService;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.pubpeer.PubPeerFeedback;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@NullMarked
class PubPeerServiceTest {
    private static final DOI DOI_WITH_COMMENTS = new DOI("10.1234/example");
    private static final PubPeerFeedback FEEDBACK = new PubPeerFeedback(
            DOI_WITH_COMMENTS, "Example", URI.create("https://pubpeer.com/publications/EXAMPLE"),
            2, List.of("Peer 1"), Optional.empty());

    private final PubPeerClient client = mock(PubPeerClient.class);
    private final PubPeerService service = new PubPeerService(client);

    @Test
    void missingDoiDoesNotSendRequest() throws FetcherException {
        assertEquals(Optional.empty(), service.fetchFeedback(new BibEntry()));
        verifyNoInteractions(client);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "not-a-doi"})
    void invalidDoiDoesNotSendRequest(String doi) throws FetcherException {
        assertEquals(Optional.empty(), service.fetchFeedback(new BibEntry().withField(StandardField.DOI, doi)));
        verifyNoInteractions(client);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.1234/example", "10.1234/EXAMPLE", "https://doi.org/10.1234/EXAMPLE", "doi:10.1234/example"})
    void resolvesDoiAndReturnsFeedback(String doi) throws FetcherException {
        when(client.fetchFeedback(List.of(DOI_WITH_COMMENTS))).thenReturn(List.of(FEEDBACK));

        assertEquals(Optional.of(FEEDBACK), service.fetchFeedback(new BibEntry().withField(StandardField.DOI, doi)));
        ArgumentCaptor<List<DOI>> requestedDois = ArgumentCaptor.captor();
        verify(client).fetchFeedback(requestedDois.capture());
        assertEquals(List.of("10.1234/example"), requestedDois.getValue().stream().map(DOI::asString).toList());
    }

    @Test
    void noCommentsReturnsEmpty() throws FetcherException {
        when(client.fetchFeedback(List.of(DOI_WITH_COMMENTS))).thenReturn(List.of());

        assertEquals(Optional.empty(), service.fetchFeedback(new BibEntry().withField(StandardField.DOI, DOI_WITH_COMMENTS.asString())));
    }

    @Test
    void emptyBatchDoesNotSendRequest() throws FetcherException {
        assertEquals(Map.of(), service.fetchFeedback(List.of()));
        verifyNoInteractions(client);
    }

    @Test
    void deduplicatesDoisAndSkipsEntriesWithoutDoi() throws FetcherException {
        when(client.fetchFeedback(List.of(DOI_WITH_COMMENTS))).thenReturn(List.of(FEEDBACK));

        assertEquals(Map.of(DOI_WITH_COMMENTS, FEEDBACK), service.fetchFeedback(List.of(
                new BibEntry().withField(StandardField.DOI, "10.1234/EXAMPLE"),
                new BibEntry().withField(StandardField.DOI, "https://doi.org/10.1234/example"),
                new BibEntry())));
        verify(client).fetchFeedback(List.of(DOI_WITH_COMMENTS));
    }

    @ParameterizedTest
    @ValueSource(ints = {40, 41, 81})
    void splitsRequestsIntoSequentialBatches(int count) throws FetcherException {
        List<DOI> dois = IntStream.range(0, count).mapToObj(index -> new DOI("10.1234/" + index)).toList();
        List<BibEntry> entries = dois.stream().map(doi -> new BibEntry().withField(StandardField.DOI, doi.asString())).toList();

        assertEquals(Map.of(), service.fetchFeedback(entries));

        InOrder requests = inOrder(client);
        for (int start = 0; start < count; start += 40) {
            requests.verify(client).fetchFeedback(dois.subList(start, Math.min(start + 40, count)));
        }
        requests.verifyNoMoreInteractions();
    }

    @Test
    void combinesFeedbackFromDifferentBatches() throws FetcherException {
        List<BibEntry> entries = IntStream.range(0, 41)
                                          .mapToObj(index -> new BibEntry().withField(StandardField.DOI, "10.1234/" + index))
                                          .toList();
        List<DOI> firstBatch = IntStream.range(0, 40).mapToObj(index -> new DOI("10.1234/" + index)).toList();
        PubPeerFeedback first = new PubPeerFeedback(new DOI("10.1234/0"), "First", FEEDBACK.url(), 1, List.of(), Optional.empty());
        PubPeerFeedback last = new PubPeerFeedback(new DOI("10.1234/40"), "Last", FEEDBACK.url(), 2, List.of(), Optional.empty());
        when(client.fetchFeedback(firstBatch)).thenReturn(List.of(first));
        when(client.fetchFeedback(List.of(last.doi()))).thenReturn(List.of(last));

        assertEquals(Map.of(first.doi(), first, last.doi(), last), service.fetchFeedback(entries));
    }

    @Test
    void requestFailureIsNotReportedAsNoComments() throws FetcherException {
        FetcherException failure = new FetcherException("Unavailable");
        when(client.fetchFeedback(List.of(DOI_WITH_COMMENTS))).thenThrow(failure);

        assertEquals(failure, assertThrows(FetcherException.class,
                () -> service.fetchFeedback(new BibEntry().withField(StandardField.DOI, DOI_WITH_COMMENTS.asString()))));
    }

    @Test
    void repeatedLookupFetchesFreshFeedback() throws FetcherException {
        when(client.fetchFeedback(List.of(DOI_WITH_COMMENTS))).thenReturn(List.of(FEEDBACK)).thenReturn(List.of());
        BibEntry entry = new BibEntry().withField(StandardField.DOI, DOI_WITH_COMMENTS.asString());

        assertEquals(Optional.of(FEEDBACK), service.fetchFeedback(entry));
        assertEquals(Optional.empty(), service.fetchFeedback(entry));
    }

    @Test
    void doesNotAssociateUnrequestedPublicationWithEntry() throws FetcherException {
        when(client.fetchFeedback(List.of(new DOI("10.1234/other")))).thenReturn(List.of(FEEDBACK));

        assertEquals(Optional.empty(), service.fetchFeedback(new BibEntry().withField(StandardField.DOI, "10.1234/other")));
    }

    @Test
    void lookupDoesNotModifyEntry() throws FetcherException {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "https://doi.org/10.1234/EXAMPLE");
        Map<Field, String> fields = Map.copyOf(entry.getFieldMap());
        when(client.fetchFeedback(List.of(DOI_WITH_COMMENTS))).thenReturn(List.of(FEEDBACK));

        service.fetchFeedback(entry);

        assertEquals(fields, entry.getFieldMap());
    }
}
