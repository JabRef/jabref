package org.jabref.logic.ai.ingestion;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.FileEmbeddingsManager;
import org.jabref.logic.ai.GenerateEmbeddingsTask;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

public class IngestionService {
    private final Map<LinkedFile, IngestionStatus> ingestionStatusMap = new HashMap<>();

    private final FilePreferences filePreferences;
    private final FileEmbeddingsManager fileEmbeddingsManager;
    private final TaskExecutor taskExecutor;

    public IngestionService(PreferencesService preferencesService, AiService aiService, TaskExecutor taskExecutor) {
        this.filePreferences = preferencesService.getFilePreferences();
        this.fileEmbeddingsManager = aiService.getEmbeddingsManager();
        this.taskExecutor = taskExecutor;
    }

    public IngestionStatus ingest(LinkedFile linkedFile, BibDatabaseContext bibDatabaseContext) {
        return ingestionStatusMap.computeIfAbsent(linkedFile, file -> {
            IngestionStatus ingestionStatus = new IngestionStatus(linkedFile, new SimpleObjectProperty<>(IngestionState.INGESTING), new SimpleStringProperty(""));
            new GenerateEmbeddingsTask(ingestionStatus, fileEmbeddingsManager, bibDatabaseContext, filePreferences).executeWith(taskExecutor);
            return ingestionStatus;
        });
    }
}
