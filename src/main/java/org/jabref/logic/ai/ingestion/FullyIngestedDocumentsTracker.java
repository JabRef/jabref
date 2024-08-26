package org.jabref.logic.ai.ingestion;

import java.util.Optional;

/**
 * This class is responsible for recording the information about which documents (or documents) have been fully ingested.
 * <p>
 * The class also records the document modification time.
 */
public interface FullyIngestedDocumentsTracker {
    void markDocumentAsFullyIngested(String link, long modificationTimeInSeconds);

    Optional<Long> getIngestedDocumentModificationTimeInSeconds(String link);

    void unmarkDocumentAsFullyIngested(String link);

    void commit();

    void close();
}
