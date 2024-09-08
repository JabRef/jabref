package org.jabref.logic.ai.ingestion.storages;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.ai.ingestion.FullyIngestedDocumentsTracker;
import org.jabref.logic.ai.util.MVStoreBase;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.NotificationService;

/**
 * This class is responsible for recording the information about which documents (or documents) have been fully ingested.
 * <p>
 * The class also records the document modification time.
 */
public class MVStoreFullyIngestedDocumentsTracker extends MVStoreBase implements FullyIngestedDocumentsTracker {
    private static final String INGESTED_MAP_NAME = "ingested";

    // This map stores the ingested documents. The key is LinkedDocument.getLink(), and the value is the modification time in seconds.
    // If an entry is present, then it means the document was ingested. Otherwise, document was not ingested.
    // The reason why we need to track ingested documents is because we cannot use AiEmbeddingsManager and see if there are
    // any embeddings because when we ingest a document embeddings are generated in series, so if 1 embedding is present
    // it doesn't mean the document is fully ingested.
    private final Map<String, Long> ingestedMap;

    public MVStoreFullyIngestedDocumentsTracker(Path path, NotificationService dialogService) {
        super(path, dialogService);

        this.ingestedMap = this.mvStore.openMap(INGESTED_MAP_NAME);
    }

    public void markDocumentAsFullyIngested(String link, long modificationTimeInSeconds) {
        ingestedMap.put(link, modificationTimeInSeconds);
    }

    public Optional<Long> getIngestedDocumentModificationTimeInSeconds(String link) {
        return Optional.ofNullable(ingestedMap.get(link));
    }

    public void unmarkDocumentAsFullyIngested(String link) {
        ingestedMap.remove(link);
    }

    @Override
    protected String errorMessageForOpening() {
        return "An error occurred while opening the fully ingested documents cache file. Fully ingested documents will not be stored in the next session.";
    }

    @Override
    protected String errorMessageForOpeningLocalized() {
        return Localization.lang("An error occurred while opening the fully ingested documents cache file. Fully ingested documents will not be stored in the next session.");
    }
}
