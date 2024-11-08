package org.jabref.logic.ai.summarization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.processingstatus.ProcessingInfo;
import org.jabref.logic.ai.processingstatus.ProcessingState;
import org.jabref.logic.ai.templates.TemplatesService;
import org.jabref.logic.ai.util.CitationKeyCheck;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.EntriesAddedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;

import com.google.common.eventbus.Subscribe;
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

    private final TreeMap<BibEntry, ProcessingInfo<BibEntry, Summary>> summariesStatusMap = new TreeMap<>(Comparator.comparing(BibEntry::getId));

    private final List<List<BibEntry>> listsUnderSummarization = new ArrayList<>();

    private final AiPreferences aiPreferences;
    private final SummariesStorage summariesStorage;
    private final ChatLanguageModel chatLanguageModel;
    private final TemplatesService templatesService;
    private final BooleanProperty shutdownSignal;
    private final FilePreferences filePreferences;
    private final TaskExecutor taskExecutor;

    public SummariesService(AiPreferences aiPreferences,
                            SummariesStorage summariesStorage,
                            ChatLanguageModel chatLanguageModel,
                            TemplatesService templatesService,
                            BooleanProperty shutdownSignal,
                            FilePreferences filePreferences,
                            TaskExecutor taskExecutor
    ) {
        this.aiPreferences = aiPreferences;
        this.summariesStorage = summariesStorage;
        this.chatLanguageModel = chatLanguageModel;
        this.templatesService = templatesService;
        this.shutdownSignal = shutdownSignal;
        this.filePreferences = filePreferences;
        this.taskExecutor = taskExecutor;
    }

    public void setupDatabase(BibDatabaseContext bibDatabaseContext) {
        // GC was eating the listeners, so we have to fall back to the event bus.
        bibDatabaseContext.getDatabase().registerListener(new EntriesChangedListener(bibDatabaseContext));
    }

    private class EntriesChangedListener {
        private final BibDatabaseContext bibDatabaseContext;

        public EntriesChangedListener(BibDatabaseContext bibDatabaseContext) {
            this.bibDatabaseContext = bibDatabaseContext;
        }

        @Subscribe
        public void listen(EntriesAddedEvent e) {
            e.getBibEntries().forEach(entry -> {
                if (aiPreferences.getAutoGenerateSummaries()) {
                    summarize(entry, bibDatabaseContext);
                }
            });
        }

        @Subscribe
        public void listen(FieldChangedEvent e) {
            if (e.getField() == StandardField.FILE && aiPreferences.getAutoGenerateSummaries()) {
                summarize(e.getBibEntry(), bibDatabaseContext);
            }
        }
    }

    /**
     * Start generating summary of a {@link BibEntry}, if it was already generated.
     * This method returns a {@link ProcessingInfo} that can be used for tracking state of the summarization.
     * Returned {@link ProcessingInfo} is related to the passed {@link BibEntry}, so if you call this method twice
     * on the same {@link BibEntry}, the method will return the same {@link ProcessingInfo}.
     */
    public ProcessingInfo<BibEntry, Summary> summarize(BibEntry bibEntry, BibDatabaseContext bibDatabaseContext) {
        ProcessingInfo<BibEntry, Summary> processingInfo = getProcessingInfo(bibEntry);

        if (processingInfo.getState() == ProcessingState.STOPPED) {
            startSummarizationTask(bibEntry, bibDatabaseContext, processingInfo);
        }

        return processingInfo;
    }

    public ProcessingInfo<BibEntry, Summary> getProcessingInfo(BibEntry entry) {
        return summariesStatusMap.computeIfAbsent(entry, file -> new ProcessingInfo<>(entry, ProcessingState.STOPPED));
    }

    public List<ProcessingInfo<BibEntry, Summary>> getProcessingInfo(List<BibEntry> entries) {
        return entries.stream().map(this::getProcessingInfo).toList();
    }

    public List<ProcessingInfo<BibEntry, Summary>> summarize(StringProperty groupName, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext) {
        List<ProcessingInfo<BibEntry, Summary>> result = getProcessingInfo(entries);

        if (listsUnderSummarization.contains(entries)) {
            return result;
        }

        listsUnderSummarization.add(entries);

        List<ProcessingInfo<BibEntry, Summary>> needToProcess = result.stream().filter(processingInfo -> processingInfo.getState() == ProcessingState.STOPPED).toList();
        startSummarizationTask(groupName, needToProcess, bibDatabaseContext);

        return result;
    }

    private void startSummarizationTask(BibEntry entry, BibDatabaseContext bibDatabaseContext, ProcessingInfo<BibEntry, Summary> processingInfo) {
        processingInfo.setState(ProcessingState.PROCESSING);

        new GenerateSummaryTask(entry, bibDatabaseContext, summariesStorage, chatLanguageModel, templatesService, shutdownSignal, aiPreferences, filePreferences)
                .onSuccess(processingInfo::setSuccess)
                .onFailure(processingInfo::setException)
                .executeWith(taskExecutor);
    }

    private void startSummarizationTask(StringProperty groupName, List<ProcessingInfo<BibEntry, Summary>> entries, BibDatabaseContext bibDatabaseContext) {
        entries.forEach(processingInfo -> processingInfo.setState(ProcessingState.PROCESSING));

        new GenerateSummaryForSeveralTask(groupName, entries, bibDatabaseContext, summariesStorage, chatLanguageModel, templatesService, shutdownSignal, aiPreferences, filePreferences, taskExecutor)
                .executeWith(taskExecutor);
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

        startSummarizationTask(bibEntry, bibDatabaseContext, processingInfo);
    }
}
