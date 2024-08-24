package org.jabref.logic.ai.ingestion;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.SimpleObjectProperty;

import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.processingstatus.ProcessingInfo;
import org.jabref.logic.ai.processingstatus.ProcessingState;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

public class IngestionService {
    private final Map<LinkedFile, ProcessingInfo<LinkedFile, Void>> ingestionStatusMap = new HashMap<>();

    private final FilePreferences filePreferences;
    private final FileEmbeddingsManager fileEmbeddingsManager;
    private final TaskExecutor taskExecutor;

    public IngestionService(PreferencesService preferencesService, AiService aiService, TaskExecutor taskExecutor) {
        this.filePreferences = preferencesService.getFilePreferences();
        this.fileEmbeddingsManager = aiService.getEmbeddingsManager();
        this.taskExecutor = taskExecutor;
    }

    public ProcessingInfo<LinkedFile, Void> ingest(LinkedFile linkedFile, BibDatabaseContext bibDatabaseContext) {
        return ingestionStatusMap.computeIfAbsent(linkedFile, file -> {
            ProcessingInfo<LinkedFile, Void> processingInfo = new ProcessingInfo<>(linkedFile, new SimpleObjectProperty<>(ProcessingState.PROCESSING), new SimpleObjectProperty<>(null), null);

            new GenerateEmbeddingsTask(linkedFile, fileEmbeddingsManager, bibDatabaseContext, filePreferences)
                    .onSuccess((v) -> processingInfo.state().set(ProcessingState.SUCCESS))
                    .onFailure(processingInfo::setException)
                    .executeWith(taskExecutor);

            return processingInfo;
        });
    }
}
