package org.jabref.logic.ai.ingestion.repositories;

/// This class is responsible for recording the information about which documents (or documents) have been fully ingested.
///
/// The class tracks files by their SHA-256 hash to detect if a file has been modified since ingestion.
public interface IngestedDocumentsRepository extends AutoCloseable {
    /// Marks a file as fully ingested using its hash.
    ///
    /// @param fileHash the SHA-256 hash of the file
    void markDocumentAsFullyIngested(String fileHash);

    /// Checks if a file has been fully ingested.
    ///
    /// @param fileHash the SHA-256 hash of the file
    /// @return true if the file has been ingested, false otherwise
    boolean isDocumentIngested(String fileHash);

    /// Removes a file from the ingested documents record.
    ///
    /// @param fileHash the SHA-256 hash of the file
    void unmarkDocumentAsFullyIngested(String fileHash);

    void removeAll();
}
