package org.jabref.logic.ai.ingestion.repositories;

import java.nio.file.Path;
import java.util.Map;

import org.jabref.logic.ai.util.MVStoreBase;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.NotificationService;

/// This class is responsible for recording the information about which documents (or documents) have been fully ingested.
///
/// The class tracks files by their SHA-256 hash.
public class MVStoreIngestedDocumentsRepository extends MVStoreBase implements IngestedDocumentsRepository {
    private static final String INGESTED_MAP_NAME = "ingested";

    // This map stores the ingested documents. The key is the SHA-256 hash of the file, and the value is a dummy boolean (always true).
    // If an entry is present, then it means the document was ingested. Otherwise, document was not ingested.
    // The reason why we need to track ingested documents is because we cannot use AiEmbeddingsManager and see if there are
    // any embeddings because when we ingest a document embeddings are generated in series, so if 1 embedding is present
    // it doesn't mean the document is fully ingested.
    private final Map<String, Boolean> ingestedMap;

    public MVStoreIngestedDocumentsRepository(
            NotificationService dialogService,
            Path path
    ) {
        super(path, dialogService);

        this.ingestedMap = this.mvStore.openMap(INGESTED_MAP_NAME);
    }

    public void markDocumentAsFullyIngested(String fileHash) {
        ingestedMap.put(fileHash, true);
    }

    public boolean isDocumentIngested(String fileHash) {
        return ingestedMap.containsKey(fileHash);
    }

    public void unmarkDocumentAsFullyIngested(String fileHash) {
        ingestedMap.remove(fileHash);
    }

    public void removeAll() {
        ingestedMap.clear();
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
