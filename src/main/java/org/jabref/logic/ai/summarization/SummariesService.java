package org.jabref.logic.ai.summarization;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.processingstatus.ProcessingInfo;
import org.jabref.logic.ai.processingstatus.ProcessingState;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SummariesService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SummariesService.class);

    private final Map<BibEntry, ProcessingInfo<BibEntry, SummariesStorage.SummarizationRecord>> summariesStatusMap = new HashMap<>();

    private final AiService aiService;
    private final FilePreferences filePreferences;
    private final TaskExecutor taskExecutor;

    public SummariesService(AiService aiService, PreferencesService preferencesService, TaskExecutor taskExecutor) {
        this.aiService = aiService;
        this.filePreferences = preferencesService.getFilePreferences();
        this.taskExecutor = taskExecutor;
    }

    public ProcessingInfo<BibEntry, SummariesStorage.SummarizationRecord> summarize(BibEntry bibEntry, BibDatabaseContext bibDatabaseContext) {
        return summariesStatusMap.computeIfAbsent(bibEntry, file -> {
            ProcessingInfo<BibEntry, SummariesStorage.SummarizationRecord> processingInfo = new ProcessingInfo<>(bibEntry, new SimpleObjectProperty<>(ProcessingState.PROCESSING), new SimpleStringProperty(""), null);

            if (bibDatabaseContext.getDatabasePath().isEmpty()) {
                startGeneratingSummary(processingInfo, bibEntry, bibDatabaseContext);
            } else if (bibEntry.getCitationKey().isEmpty()) {
                startGeneratingSummary(processingInfo, bibEntry, bibDatabaseContext);
            } else {
                Optional<SummariesStorage.SummarizationRecord> record = aiService.getSummariesStorage().get(bibDatabaseContext.getDatabasePath().get(), bibEntry.getCitationKey().get());

                if (record.isEmpty()) {
                    startGeneratingSummary(processingInfo, bibEntry, bibDatabaseContext);
                } else {
                    processingInfo.state().set(ProcessingState.SUCCESS);
                    processingInfo.data().set(record.get());
                }
            }

            return processingInfo;
        });
    }

    private void startGeneratingSummary(ProcessingInfo<BibEntry, SummariesStorage.SummarizationRecord> processingInfo, BibEntry bibEntry, BibDatabaseContext bibDatabaseContext) {
        new GenerateSummaryTask(bibDatabaseContext, bibEntry.getCitationKey().orElse("<no citation key>"), bibEntry.getFiles(), aiService, filePreferences)
                .onSuccess(summary -> {
                    SummariesStorage.SummarizationRecord record = new SummariesStorage.SummarizationRecord(
                            LocalDateTime.now(),
                            aiService.getPreferences().getAiProvider(),
                            aiService.getPreferences().getSelectedChatModel(),
                            summary
                    );

                    processingInfo.setSuccess(record);

                    if (bibDatabaseContext.getDatabasePath().isEmpty()) {
                        LOGGER.info("No database path is present. Summary will not be stored in the next sessions");
                    } else if (bibEntry.getCitationKey().isEmpty()) {
                        LOGGER.info("No citation key is present. Summary will not be stored in the next sessions");
                    } else {
                        aiService.getSummariesStorage().set(bibDatabaseContext.getDatabasePath().get(), bibEntry.getCitationKey().get(), record);
                    }
                })
                .onFailure(processingInfo::setError)
                .executeWith(taskExecutor);
    }
}
