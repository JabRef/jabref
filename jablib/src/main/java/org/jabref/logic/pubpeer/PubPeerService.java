package org.jabref.logic.pubpeer;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.pubpeer.PubPeerFeedback;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class PubPeerService {
    private static final int BATCH_SIZE = 40;

    private final PubPeerClient client;

    public PubPeerService(PubPeerClient client) {
        this.client = client;
    }

    public Optional<PubPeerFeedback> fetchFeedback(BibEntry entry) throws FetcherException {
        return fetchFeedback(List.of(entry)).values().stream().findFirst();
    }

    public Map<DOI, PubPeerFeedback> fetchFeedback(List<BibEntry> entries) throws FetcherException {
        List<DOI> dois = entries.stream()
                                .map(entry -> entry.getField(StandardField.DOI).flatMap(DOI::parse))
                                .flatMap(Optional::stream)
                                .map(doi -> new DOI(doi.asString().toLowerCase(Locale.ROOT)))
                                .distinct()
                                .toList();
        Map<DOI, PubPeerFeedback> feedback = new HashMap<>();
        for (int start = 0; start < dois.size(); start += BATCH_SIZE) {
            List<DOI> batch = dois.subList(start, Math.min(start + BATCH_SIZE, dois.size()));
            for (PubPeerFeedback publication : client.fetchFeedback(batch)) {
                if (batch.contains(publication.doi())) {
                    feedback.put(publication.doi(), publication);
                }
            }
        }
        return feedback;
    }
}
