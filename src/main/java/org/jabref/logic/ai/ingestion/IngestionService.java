package org.jabref.logic.ai.ingestion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.ReadOnlyBooleanProperty;

import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.processingstatus.ProcessingInfo;
import org.jabref.logic.ai.processingstatus.ProcessingState;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.ai.AiPreferences;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * Main class for generating embedding for files.
 * Use this class in the logic and UI.
 */
public class IngestionService {
    private final Map<LinkedFile, ProcessingInfo<LinkedFile, Void>> ingestionStatusMap = new HashMap<>();

    private final FilePreferences filePreferences;
    private final TaskExecutor taskExecutor;

    private final FileEmbeddingsManager fileEmbeddingsManager;

    public IngestionService(AiPreferences aiPreferences,
                            ReadOnlyBooleanProperty shutdownProperty,
                            EmbeddingModel embeddingModel,
                            EmbeddingStore<TextSegment> embeddingStore,
                            FullyIngestedDocumentsTracker fullyIngestedDocumentsTracker,
                            FilePreferences filePreferences,
                            TaskExecutor taskExecutor
    ) {
        this.filePreferences = filePreferences;
        this.taskExecutor = taskExecutor;

        this.fileEmbeddingsManager = new FileEmbeddingsManager(
                aiPreferences,
                shutdownProperty,
                embeddingModel,
                embeddingStore,
                fullyIngestedDocumentsTracker
        );
    }

    /**
     * Start ingesting of a {@link LinkedFile}, if it was not ingested.
     * This method returns a {@link ProcessingInfo} that can be used for tracking state of the ingestion.
     * Returned {@link ProcessingInfo} is related to the passed {@link LinkedFile}, so if you call this method twice
     * on the same {@link LinkedFile}, the method will return the same {@link ProcessingInfo}.
     */
    public ProcessingInfo<LinkedFile, Void> ingest(LinkedFile linkedFile, BibDatabaseContext bibDatabaseContext) {
        ProcessingInfo<LinkedFile, Void> processingInfo = ingestionStatusMap.computeIfAbsent(linkedFile, file -> {
            ProcessingInfo<LinkedFile, Void> newProcessingInfo = new ProcessingInfo<>(linkedFile, ProcessingState.PROCESSING);
            startEmbeddingsGenerationTask(linkedFile, bibDatabaseContext, newProcessingInfo);
            return newProcessingInfo;
        });

        if (processingInfo.getState() == ProcessingState.STOPPED) {
            startEmbeddingsGenerationTask(linkedFile, bibDatabaseContext, processingInfo);
        }

        return processingInfo;
    }

    private void startEmbeddingsGenerationTask(LinkedFile linkedFile, BibDatabaseContext bibDatabaseContext, ProcessingInfo<LinkedFile, Void> processingInfo) {
        new GenerateEmbeddingsTask(linkedFile, fileEmbeddingsManager, bibDatabaseContext, filePreferences)
                .onSuccess(v -> processingInfo.setState(ProcessingState.SUCCESS))
                .onFailure(processingInfo::setException)
                .executeWith(taskExecutor);
    }

    public void clearEmbeddingsFor(List<LinkedFile> linkedFiles) {
        fileEmbeddingsManager.clearEmbeddingsFor(linkedFiles);
        ingestionStatusMap.values().forEach(processingInfo -> processingInfo.setState(ProcessingState.STOPPED));
    }
}
