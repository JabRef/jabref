package org.jabref.logic.ai.ingestion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.processingstatus.ProcessingInfo;
import org.jabref.logic.ai.processingstatus.ProcessingState;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * Main class for generating embedding for files.
 * Use this class in the logic and UI.
 */
public class IngestionService {
    private final Map<LinkedFile, ProcessingInfo<LinkedFile, Void>> ingestionStatusMap = new HashMap<>();

    private final List<List<LinkedFile>> listsUnderIngestion = new ArrayList<>();

    private final FilePreferences filePreferences;
    private final TaskExecutor taskExecutor;

    private final FileEmbeddingsManager fileEmbeddingsManager;

    private final ReadOnlyBooleanProperty shutdownSignal;

    public IngestionService(AiPreferences aiPreferences,
                            ReadOnlyBooleanProperty shutdownSignal,
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
                shutdownSignal,
                embeddingModel,
                embeddingStore,
                fullyIngestedDocumentsTracker
        );

        this.shutdownSignal = shutdownSignal;
    }

    /**
     * Start ingesting of a {@link LinkedFile}, if it was not ingested.
     * This method returns a {@link ProcessingInfo} that can be used for tracking state of the ingestion.
     * Returned {@link ProcessingInfo} is related to the passed {@link LinkedFile}, so if you call this method twice
     * on the same {@link LinkedFile}, the method will return the same {@link ProcessingInfo}.
     */
    public ProcessingInfo<LinkedFile, Void> ingest(LinkedFile linkedFile, BibDatabaseContext bibDatabaseContext) {
        ProcessingInfo<LinkedFile, Void> processingInfo = getProcessingInfo(linkedFile);

        if (processingInfo.getState() == ProcessingState.STOPPED) {
            startEmbeddingsGenerationTask(linkedFile, bibDatabaseContext, processingInfo);
        }

        return processingInfo;
    }

    /**
     * Get {@link ProcessingInfo} of a {@link LinkedFile}. Initially, it is in state {@link ProcessingState#STOPPED}.
     * This method will not start ingesting. If you need to start it, use {@link IngestionService#ingest(LinkedFile, BibDatabaseContext)}.
     */
    public ProcessingInfo<LinkedFile, Void> getProcessingInfo(LinkedFile linkedFile) {
        return ingestionStatusMap.computeIfAbsent(linkedFile, file -> new ProcessingInfo<>(linkedFile, ProcessingState.STOPPED));
    }

    public List<ProcessingInfo<LinkedFile, Void>> getProcessingInfo(List<LinkedFile> linkedFiles) {
        return linkedFiles.stream().map(this::getProcessingInfo).toList();
    }

    public List<ProcessingInfo<LinkedFile, Void>> ingest(StringProperty name, List<LinkedFile> linkedFiles, BibDatabaseContext bibDatabaseContext) {
        List<ProcessingInfo<LinkedFile, Void>> result = getProcessingInfo(linkedFiles);

        if (listsUnderIngestion.contains(linkedFiles)) {
            return result;
        }

        List<ProcessingInfo<LinkedFile, Void>> needToProcess = result.stream().filter(processingInfo -> processingInfo.getState() == ProcessingState.STOPPED).toList();
        startEmbeddingsGenerationTask(name, needToProcess, bibDatabaseContext);

        return result;
    }

    private void startEmbeddingsGenerationTask(LinkedFile linkedFile, BibDatabaseContext bibDatabaseContext, ProcessingInfo<LinkedFile, Void> processingInfo) {
        new GenerateEmbeddingsTask(linkedFile, fileEmbeddingsManager, bibDatabaseContext, filePreferences, shutdownSignal)
                .onSuccess(v -> processingInfo.setState(ProcessingState.SUCCESS))
                .onFailure(processingInfo::setException)
                .executeWith(taskExecutor);
    }

    private void startEmbeddingsGenerationTask(StringProperty name, List<ProcessingInfo<LinkedFile, Void>> linkedFiles, BibDatabaseContext bibDatabaseContext) {
        new GenerateEmbeddingsForSeveralTask(name, linkedFiles, fileEmbeddingsManager, bibDatabaseContext, filePreferences, taskExecutor, shutdownSignal)
                .executeWith(taskExecutor);
    }

    public void clearEmbeddingsFor(List<LinkedFile> linkedFiles) {
        fileEmbeddingsManager.clearEmbeddingsFor(linkedFiles);
        ingestionStatusMap.values().forEach(processingInfo -> processingInfo.setState(ProcessingState.STOPPED));
    }
}
