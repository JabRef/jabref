package org.jabref.logic.ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javafx.beans.property.BooleanProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.logic.ai.impl.embeddings.FullyIngestedDocumentsTracker;
import org.jabref.logic.ai.impl.embeddings.LowLevelIngestor;
import org.jabref.logic.ai.impl.embeddings.MVStoreEmbeddingStore;
import org.jabref.logic.ai.impl.models.EmbeddingModel;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.AiPreferences;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;

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
public class AiEmbeddingsManager implements AutoCloseable {
    public static final String LINK_METADATA_KEY = "link";

    private static final String EMBEDDING_STORE_FILE_NAME = "embeddings.mv";
    private static final String INGESTED_FILES_FILE_NAME = "fullyIngested.mv";

    private final AiPreferences aiPreferences;

    private final MVStoreEmbeddingStore embeddingStore;
    private final FullyIngestedDocumentsTracker fullyIngestedDocumentsTracker;
    private final LowLevelIngestor lowLevelIngestor;

    public AiEmbeddingsManager(AiPreferences aiPreferences, EmbeddingModel embeddingModel, DialogService dialogService) {
        this.aiPreferences = aiPreferences;

        FullyIngestedDocumentsTracker fullyIngestedDocumentsTrackerTemp;
        MVStoreEmbeddingStore mvStoreEmbeddingStoreTemp;
        try {
            Path embeddingStorePath = JabRefDesktop.getEmbeddingsCacheDirectory().resolve(EMBEDDING_STORE_FILE_NAME);
            Path ingestedFilesTrackerPath = JabRefDesktop.getEmbeddingsCacheDirectory().resolve(INGESTED_FILES_FILE_NAME);

            Files.createDirectories(JabRefDesktop.getEmbeddingsCacheDirectory());
            mvStoreEmbeddingStoreTemp = new MVStoreEmbeddingStore(embeddingStorePath, dialogService);
            fullyIngestedDocumentsTrackerTemp = new FullyIngestedDocumentsTracker(ingestedFilesTrackerPath, dialogService);
        } catch (IOException e) {
            dialogService.showErrorDialogAndWait("An error occurred while creating directories for embeddings cache. Will store cache in RAM", e);
            mvStoreEmbeddingStoreTemp = new MVStoreEmbeddingStore(null, dialogService);
            fullyIngestedDocumentsTrackerTemp = new FullyIngestedDocumentsTracker(null, dialogService);
        }

        this.fullyIngestedDocumentsTracker = fullyIngestedDocumentsTrackerTemp;
        this.embeddingStore = mvStoreEmbeddingStoreTemp;
        this.lowLevelIngestor = new LowLevelIngestor(aiPreferences, embeddingStore, embeddingModel);

        setupListeningToPreferencesChanges();
    }

    private void setupListeningToPreferencesChanges() {
        // When these properties change, EmbeddingsGenerationTaskManager's should add the entries again.

        aiPreferences.onEmbeddingsParametersChange(embeddingStore::removeAll);
    }

    /**
     * Add document to embedding store. At the end of addition, mark document as ingested.
     * This method does not check if document was already ingested.
     *
     * @param link - path of the Document or any string that uniquely identifies the document.
     * @param document - document to add.
     * @param modificationTimeInSeconds - modification time in seconds of the document.
     * @param stopProperty - in case you want to stop ingestion process, set this property to true.
     */
    public void addDocument(String link, Document document, long modificationTimeInSeconds, BooleanProperty stopProperty) {
        document.metadata().put(LINK_METADATA_KEY, link);
        lowLevelIngestor.ingestDocument(document, stopProperty);

        if (!stopProperty.get()) {
            fullyIngestedDocumentsTracker.markDocumentAsFullyIngested(link, modificationTimeInSeconds);
        }
    }

    public void removeDocument(String link) {
        embeddingStore.removeAll(MetadataFilterBuilder.metadataKey(LINK_METADATA_KEY).isEqualTo(link));
        fullyIngestedDocumentsTracker.unmarkDocumentAsFullyIngested(link);
    }

    public void close() {
        embeddingStore.close();
        fullyIngestedDocumentsTracker.close();
    }

    public EmbeddingStore<TextSegment> getEmbeddingsStore() {
        return embeddingStore;
    }

    public Set<String> getIngestedDocuments() {
        return fullyIngestedDocumentsTracker.getFullyIngestedDocuments();
    }

    public Optional<Long> getIngestedDocumentModificationTimeInSeconds(String link) {
        return fullyIngestedDocumentsTracker.getIngestedDocumentModificationTimeInSeconds(link);
    }

    public void registerListener(Object object) {
        fullyIngestedDocumentsTracker.registerListener(object);
    }

    public boolean hasIngestedDocument(String link) {
        return fullyIngestedDocumentsTracker.hasIngestedDocument(link);
    }

    public boolean hasIngestedDocuments(List<String> links) {
        return links.stream().allMatch(this::hasIngestedDocument);
    }

    public boolean hasIngestedLinkedFiles(List<LinkedFile> linkedFiles) {
        return hasIngestedDocuments(linkedFiles.stream().map(LinkedFile::getLink).toList());
    }
}
