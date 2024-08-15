package org.jabref.logic.ai.embeddings;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.eventbus.EventBus;
import org.h2.mvstore.MVStore;

/**
 * This class is responsible for recording the information about which documents (or documents) have been fully ingested.
 * <p>
 * It will also post an {@link DocumentIngestedEvent} to its event bus when a document is fully ingested.
 * <p>
 * The class also records the document modification time.
 */
public class FullyIngestedDocumentsTracker {
    // This map stores the ingested documents. The key is LinkedDocument.getLink(), and the value is the modification time in seconds.
    // If an entry is present, then it means the document was ingested. Otherwise, document was not ingested.
    // The reason why we need to track ingested documents is because we cannot use AiEmbeddingsManager and see if there are
    // any embeddings because when we ingest a document embeddings are generated in series, so if 1 embedding is present
    // it doesn't mean the document is fully ingested.
    private final Map<String, Long> ingestedMap;

    // Used to update the tab content after the data is available
    private final EventBus eventBus = new EventBus();

    public FullyIngestedDocumentsTracker(MVStore mvStore) {
        this.ingestedMap = mvStore.openMap("ingestedMap");
    }

    public boolean hasIngestedDocument(String link) {
        return ingestedMap.containsKey(link);
    }

    public static class DocumentIngestedEvent { }

    public void markDocumentAsFullyIngested(String link, long modificationTimeInSeconds) {
        ingestedMap.put(link, modificationTimeInSeconds);
        eventBus.post(new DocumentIngestedEvent());
    }

    public Optional<Long> getIngestedDocumentModificationTimeInSeconds(String link) {
        return Optional.ofNullable(ingestedMap.get(link));
    }

    public void registerListener(Object listener) {
        eventBus.register(listener);
    }

    public void unmarkDocumentAsFullyIngested(String link) {
        ingestedMap.remove(link);
    }

    public Set<String> getFullyIngestedDocuments() {
        return new HashSet<>(ingestedMap.keySet());
    }
}
