package org.jabref.logic.ai.ingestion.logic;

import java.util.List;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.ingestion.util.FileHasher;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.ObservablesHelper;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;

public class EmbeddingsCleaner {
    public static final String FILE_HASH_METADATA_KEY = "fileHash";

    private final AiPreferences aiPreferences;

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final IngestedDocumentsRepository ingestedDocumentsRepository;

    public EmbeddingsCleaner(
            AiPreferences aiPreferences,
            EmbeddingStore<TextSegment> embeddingStore,
            IngestedDocumentsRepository ingestedDocumentsRepository
    ) {
        this.aiPreferences = aiPreferences;
        this.embeddingStore = embeddingStore;
        this.ingestedDocumentsRepository = ingestedDocumentsRepository;

        setupListeners();
    }

    private void setupListeners() {
        ObservablesHelper.onChange(aiPreferences.getEmbeddingsProperties(), this::removeAll);
    }

    public void removeAll() {
        embeddingStore.removeAll();
        ingestedDocumentsRepository.removeAll();
    }

    public void removeDocument(String fileHash) {
        embeddingStore.removeAll(MetadataFilterBuilder.metadataKey(FILE_HASH_METADATA_KEY).isEqualTo(fileHash));
        ingestedDocumentsRepository.unmarkDocumentAsFullyIngested(fileHash);
    }

    public void clearEmbeddingsFor(List<LinkedFile> linkedFiles, BibDatabaseContext bibDatabaseContext, FilePreferences filePreferences) {
        linkedFiles.stream()
                   .flatMap(linkedFile -> linkedFile.findIn(bibDatabaseContext, filePreferences).stream())
                   .forEach(path ->
                           FileHasher.computeHash(path).ifPresent(this::removeDocument)
                   );
    }
}
