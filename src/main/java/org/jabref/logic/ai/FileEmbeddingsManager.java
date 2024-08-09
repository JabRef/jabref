package org.jabref.logic.ai;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;

import org.jabref.logic.ai.embeddings.FullyIngestedDocumentsTracker;
import org.jabref.logic.ai.embeddings.LowLevelIngestor;
import org.jabref.logic.ai.embeddings.MVStoreEmbeddingStore;
import org.jabref.logic.ai.models.JabRefEmbeddingModel;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.ai.AiPreferences;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.h2.mvstore.MVStore;

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
public class FileEmbeddingsManager {
    public static final String LINK_METADATA_KEY = "link";

    private final AiPreferences aiPreferences;
    private final ReadOnlyBooleanProperty shutdownProperty;

    private final MVStoreEmbeddingStore embeddingStore;
    private final FullyIngestedDocumentsTracker fullyIngestedDocumentsTracker;
    private final LowLevelIngestor lowLevelIngestor;

    public FileEmbeddingsManager(AiPreferences aiPreferences, ReadOnlyBooleanProperty shutdownProperty, JabRefEmbeddingModel jabRefEmbeddingModel, MVStore mvStore) {
        this.aiPreferences = aiPreferences;
        this.shutdownProperty = shutdownProperty;

        this.embeddingStore = new MVStoreEmbeddingStore(mvStore);
        this.fullyIngestedDocumentsTracker = new FullyIngestedDocumentsTracker(mvStore);
        this.lowLevelIngestor = new LowLevelIngestor(aiPreferences, embeddingStore, jabRefEmbeddingModel);

        setupListeningToPreferencesChanges();
    }

    private void setupListeningToPreferencesChanges() {
        aiPreferences.onEmbeddingsParametersChange(embeddingStore::removeAll);
    }

    public void addDocument(String link, Document document, long modificationTimeInSeconds, IntegerProperty workDone, IntegerProperty workMax) throws InterruptedException {
        document.metadata().put(LINK_METADATA_KEY, link);
        lowLevelIngestor.ingestDocument(document, shutdownProperty, workDone, workMax);

        if (!shutdownProperty.get()) {
            fullyIngestedDocumentsTracker.markDocumentAsFullyIngested(link, modificationTimeInSeconds);
        }
    }

    public void removeDocument(String link) {
        embeddingStore.removeAll(MetadataFilterBuilder.metadataKey(LINK_METADATA_KEY).isEqualTo(link));
        fullyIngestedDocumentsTracker.unmarkDocumentAsFullyIngested(link);
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

    public void clearEmbeddingsFor(List<LinkedFile> linkedFiles) {
        linkedFiles.stream().map(LinkedFile::getLink).forEach(this::removeDocument);
    }
}
