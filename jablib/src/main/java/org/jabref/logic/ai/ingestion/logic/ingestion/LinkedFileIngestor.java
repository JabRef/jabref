package org.jabref.logic.ai.ingestion.logic.ingestion;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.ingestion.logic.EmbeddingsCleaner;
import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.ingestion.util.FileHasher;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkedFileIngestor {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkedFileIngestor.class);

    private final FilePreferences filePreferences;
    private final PersistedFileIngestor persistedFileIngestor;

    public LinkedFileIngestor(
            FilePreferences filePreferences,
            IngestedDocumentsRepository ingestedDocumentsRepository,
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel,
            DocumentSplitter documentSplitter
    ) {
        this.filePreferences = filePreferences;
        this.persistedFileIngestor = new PersistedFileIngestor(
                ingestedDocumentsRepository,
                embeddingStore,
                embeddingModel,
                documentSplitter
        );
    }

    public void ingest(BibDatabaseContext bibDatabaseContext, LinkedFile linkedFile) throws InterruptedException {
        LOGGER.debug("Generating embeddings for file \"{}\"", linkedFile.getLink());

        Optional<Path> path = linkedFile.findIn(bibDatabaseContext, filePreferences);

        if (path.isEmpty()) {
            LOGGER.error("Could not find path for a linked file \"{}\", while generating embeddings", linkedFile.getLink());
            LOGGER.debug("Unable to generate embeddings for file \"{}\", because it was not found while generating embeddings", linkedFile.getLink());
            return;
        }

        Optional<String> fileHash = FileHasher.computeHash(path.get());
        if (fileHash.isEmpty()) {
            LOGGER.error("Could not compute hash for file \"{}\" while generating embeddings", linkedFile.getLink());
            return;
        }

        Metadata metadata = new Metadata();
        metadata.put(EmbeddingsCleaner.FILE_HASH_METADATA_KEY, fileHash.get());

        persistedFileIngestor.ingest(metadata, path.get());
    }
}
