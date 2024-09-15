package org.jabref.logic.ai.summarization;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.processingstatus.ProcessingInfo;
import org.jabref.logic.ai.processingstatus.ProcessingState;
import org.jabref.logic.ai.util.CitationKeyCheck;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for generating summaries of {@link BibEntry}ies.
 * Use this class in the logic and UI.
 * <p>
 * In order for summary to be stored and loaded, the {@link BibEntry} must satisfy the following requirements:
 * 1. There should exist an associated {@link BibDatabaseContext} for the {@link BibEntry}.
 * 2. The database path of the associated {@link BibDatabaseContext} must be set.
 * 3. The citation key of the {@link BibEntry} must be set and unique.
 */
public class SummariesService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SummariesService.class);

    private final Map<BibEntry, ProcessingInfo<BibEntry, Summary>> summariesStatusMap = new HashMap<>();

    private final AiPreferences aiPreferences;
    private final SummariesStorage summariesStorage;
    private final ChatLanguageModel chatLanguageModel;
    private final BooleanProperty shutdownSignal;
    private final FilePreferences filePreferences;
    private final TaskExecutor taskExecutor;

    public SummariesService(AiPreferences aiPreferences,
                            SummariesStorage summariesStorage,
                            ChatLanguageModel chatLanguageModel,
                            BooleanProperty shutdownSignal,
                            FilePreferences filePreferences,
                            TaskExecutor taskExecutor
    ) {
        this.aiPreferences = aiPreferences;
        this.summariesStorage = summariesStorage;
        this.chatLanguageModel = chatLanguageModel;
        this.shutdownSignal = shutdownSignal;
        this.filePreferences = filePreferences;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Start generating summary of a {@link BibEntry}, if it was already generated.
     * This method returns a {@link ProcessingInfo} that can be used for tracking state of the summarization.
     * Returned {@link ProcessingInfo} is related to the passed {@link BibEntry}, so if you call this method twice
     * on the same {@link BibEntry}, the method will return the same {@link ProcessingInfo}.
     */
    public ProcessingInfo<BibEntry, Summary> summarize(BibEntry bibEntry, BibDatabaseContext bibDatabaseContext) {
        return summariesStatusMap.computeIfAbsent(bibEntry, file -> {
            ProcessingInfo<BibEntry, Summary> processingInfo = new ProcessingInfo<>(bibEntry, ProcessingState.PROCESSING);
            generateSummary(bibEntry, bibDatabaseContext, processingInfo);
            return processingInfo;
        });
    }

    private void generateSummary(BibEntry bibEntry, BibDatabaseContext bibDatabaseContext, ProcessingInfo<BibEntry, Summary> processingInfo) {
        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            runGenerateSummaryTask(processingInfo, bibEntry, bibDatabaseContext);
        } else if (bibEntry.getCitationKey().isEmpty() || CitationKeyCheck.citationKeyIsPresentAndUnique(bibDatabaseContext, bibEntry)) {
            runGenerateSummaryTask(processingInfo, bibEntry, bibDatabaseContext);
        } else {
            Optional<Summary> summary = summariesStorage.get(bibDatabaseContext.getDatabasePath().get(), bibEntry.getCitationKey().get());

            if (summary.isEmpty()) {
                runGenerateSummaryTask(processingInfo, bibEntry, bibDatabaseContext);
            } else {
                processingInfo.setSuccess(summary.get());
            }
        }
    }

    /**
     * Method, similar to {@link #summarize(BibEntry, BibDatabaseContext)}, but it allows you to regenerate summary.
     */
    public void regenerateSummary(BibEntry bibEntry, BibDatabaseContext bibDatabaseContext) {
        ProcessingInfo<BibEntry, Summary> processingInfo = summarize(bibEntry, bibDatabaseContext);
        processingInfo.setState(ProcessingState.PROCESSING);

        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.info("No database path is present. Could not clear stored summary for regeneration");
        } else if (bibEntry.getCitationKey().isEmpty() || CitationKeyCheck.citationKeyIsPresentAndUnique(bibDatabaseContext, bibEntry)) {
            LOGGER.info("No valid citation key is present. Could not clear stored summary for regeneration");
        } else {
            summariesStorage.clear(bibDatabaseContext.getDatabasePath().get(), bibEntry.getCitationKey().get());
        }

        generateSummary(bibEntry, bibDatabaseContext, processingInfo);
    }

    private void runGenerateSummaryTask(ProcessingInfo<BibEntry, Summary> processingInfo, BibEntry bibEntry, BibDatabaseContext bibDatabaseContext) {
        new GenerateSummaryTask(
                bibDatabaseContext,
                bibEntry.getCitationKey().orElse("<no citation key>"),
                bibEntry.getFiles(),
                chatLanguageModel,
                shutdownSignal,
                aiPreferences,
                filePreferences)
                .onSuccess(summary -> {
                    Summary Summary = new Summary(
                            LocalDateTime.now(),
                            aiPreferences.getAiProvider(),
                            aiPreferences.getSelectedChatModel(),
                            summary
                    );

                    processingInfo.setSuccess(Summary);

                    if (bibDatabaseContext.getDatabasePath().isEmpty()) {
                        LOGGER.info("No database path is present. Summary will not be stored in the next sessions");
                    } else if (CitationKeyCheck.citationKeyIsPresentAndUnique(bibDatabaseContext, bibEntry)) {
                        LOGGER.info("No valid citation key is present. Summary will not be stored in the next sessions");
                    } else {
                        summariesStorage.set(bibDatabaseContext.getDatabasePath().get(), bibEntry.getCitationKey().get(), Summary);
                    }
                })
                .onFailure(processingInfo::setException)
                .executeWith(taskExecutor);
    }
}
