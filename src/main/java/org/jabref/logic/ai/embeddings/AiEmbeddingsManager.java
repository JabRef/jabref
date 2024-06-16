package org.jabref.logic.ai.embeddings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.gui.desktop.JabRefDesktop;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiEmbeddingsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiEmbeddingsManager.class);

    private static final String EMBEDDINGS_STORE_FILE_NAME = "embeddings.mv";

    private final MVStore mvStore;

    private final MVStoreEmbeddingStore embeddingStore;
    private final AiIngestedFilesTracker ingestedFilesTracker;

    public AiEmbeddingsManager() {
        Path mvStorePath = JabRefDesktop.getEmbeddingsCacheDirectory().resolve(EMBEDDINGS_STORE_FILE_NAME);

        try {
            Files.createDirectories(JabRefDesktop.getEmbeddingsCacheDirectory());
        } catch (IOException e) {
            LOGGER.error("An error occurred while creating directories for embedding store. Will use an in-memory store", e);
            mvStorePath = null;
        }

        this.mvStore = MVStore.open(mvStorePath == null ? null : mvStorePath.toString());
        this.embeddingStore = new MVStoreEmbeddingStore(this.mvStore);
        this.ingestedFilesTracker = new AiIngestedFilesTracker(this.mvStore);
    }

    public void removeIngestedFile(String link) {
        embeddingStore.removeAll(MetadataFilterBuilder.metadataKey("linkedFile").isEqualTo(link));
        this.ingestedFilesTracker.removeIngestedFileTrack(link);
    }

    public AiIngestedFilesTracker getIngestedFilesTracker() {
        return ingestedFilesTracker;
    }

    public void close() {
        this.mvStore.close();
    }

    public EmbeddingStore<TextSegment> getEmbeddingsStore() {
        return embeddingStore;
    }
}
