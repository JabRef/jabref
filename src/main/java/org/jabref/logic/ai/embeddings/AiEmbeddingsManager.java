package org.jabref.logic.ai.embeddings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.preferences.AiPreferences;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for managing the embeddings cache. The cache is saved in a local user directory.
 * <p>
 * MVStore is used as an embedded database. It stores the embeddings and what files have been fully ingested.
 * {@link org.jabref.model.entry.LinkedFile} and embeddings are connected with LinkedFile.getLink().
 * <p>
 * In case an error occurs while opening an MVStore, the class will notify the user of this error and continue
 * with in-memory store (meaning all embeddings will be thrown away on exit).
 * <p>
 * This class also listens for changes of embeddings parameters (in AI "Expert settings" section). In case any of them
 * changes, the embeddings should be invalidated (cleared).
 */
public class AiEmbeddingsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiEmbeddingsManager.class);

    private static final String EMBEDDINGS_STORE_FILE_NAME = "embeddings.mv";

    private final AiPreferences aiPreferences;

    private final MVStore mvStore;

    private final MVStoreEmbeddingStore embeddingStore;
    private final AiIngestedFilesTracker ingestedFilesTracker;

    public AiEmbeddingsManager(AiPreferences aiPreferences, DialogService dialogService) {
        this.aiPreferences = aiPreferences;

        Path mvStorePath = JabRefDesktop.getEmbeddingsCacheDirectory().resolve(EMBEDDINGS_STORE_FILE_NAME);

        try {
            Files.createDirectories(JabRefDesktop.getEmbeddingsCacheDirectory());
        } catch (IOException e) {
            LOGGER.error("An error occurred while creating directories for embedding store", e);
            dialogService.showErrorDialogAndWait("An error occurred while creating directories for embedding store. Will use an in-memory store", e);
            mvStorePath = null;
        }

        MVStore mvStoreTemp;

        try {
            mvStoreTemp = MVStore.open(mvStorePath == null ? null : mvStorePath.toString());
        } catch (Exception e) {
            dialogService.showErrorDialogAndWait("An error occurred while opening embedding store. Will use an in-memory store", e);

            mvStoreTemp = MVStore.open(null);
        }
        LOGGER.trace("Created MVStore for embeddings");

        this.mvStore = mvStoreTemp;
        this.embeddingStore = new MVStoreEmbeddingStore(this.mvStore);
        this.ingestedFilesTracker = new AiIngestedFilesTracker(this.mvStore);

        setupListeningToPreferencesChanges();
    }

    public void removeIngestedFile(String link) {
        embeddingStore.removeAll(MetadataFilterBuilder.metadataKey("linkedFile").isEqualTo(link));
        ingestedFilesTracker.removeIngestedFileTrack(link);
    }

    private void setupListeningToPreferencesChanges() {
        // When these properties change, EmbeddingsGenerationTaskManager's should add the entries again.
        // Unfortunately, they would also remove embeddings. So embeddings will be removed twice:
        // once there, and the other in EmbeddingsGenerationTaskManager.
        // It's not an error, but it's a double work.

        aiPreferences.onEmbeddingsParametersChange(embeddingStore::removeAll);
    }

    public AiIngestedFilesTracker getIngestedFilesTracker() {
        return ingestedFilesTracker;
    }

    public void close() {
        LOGGER.trace("Closing embeddings manager");
        mvStore.close();
    }

    public EmbeddingStore<TextSegment> getEmbeddingsStore() {
        return embeddingStore;
    }
}
