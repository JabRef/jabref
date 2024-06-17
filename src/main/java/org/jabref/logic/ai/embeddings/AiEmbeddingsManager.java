package org.jabref.logic.ai.embeddings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.preferences.AiPreferences;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiEmbeddingsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiEmbeddingsManager.class);

    private static final String EMBEDDINGS_STORE_FILE_NAME = "embeddings.mv";

    private final AiPreferences aiPreferences;

    private final MVStore mvStore;

    private final MVStoreEmbeddingStore embeddingStore;
    private final AiIngestedFilesTracker ingestedFilesTracker;

    public AiEmbeddingsManager(AiPreferences aiPreferences) {
        this.aiPreferences = aiPreferences;

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

        listenToPreferences();
    }

    private void listenToPreferences() {
        // When these properties change, EmbeddingsGenerationTaskManager's should add the entries again.
        // Unfortunately, they would also remove embeddings. So embeddings will be removed twice:
        // once there, and the other in EmbeddingsGenerationTaskManager.
        // It's not an error, but it's a double work.

        aiPreferences.embeddingModelProperty().addListener(obs -> embeddingStore.removeAll());
        aiPreferences.documentSplitterChunkSizeProperty().addListener(obs -> embeddingStore.removeAll());
        aiPreferences.documentSplitterOverlapSizeProperty().addListener(obs -> embeddingStore.removeAll());
    }

    public void removeIngestedFile(String link) {
        embeddingStore.removeAll(MetadataFilterBuilder.metadataKey("linkedFile").isEqualTo(link));
        ingestedFilesTracker.removeIngestedFileTrack(link);
    }

    public AiIngestedFilesTracker getIngestedFilesTracker() {
        return ingestedFilesTracker;
    }

    public void close() {
        mvStore.close();
    }

    public EmbeddingStore<TextSegment> getEmbeddingsStore() {
        return embeddingStore;
    }
}
