package org.jabref.logic.ai.summarization;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Pair;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.processingstatus.ProcessingInfo;
import org.jabref.logic.ai.processingstatus.ProcessingState;
import org.jabref.logic.ai.templates.TemplatesService;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.ProgressCounter;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This task generates summaries for several {@link BibEntry}ies (typically used for groups).
 * It will check if summaries were already generated.
 * And it also will store the summaries.
 */
public class GenerateSummaryForSeveralTask extends BackgroundTask<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateSummaryForSeveralTask.class);

    private final StringProperty groupName;
    private final List<ProcessingInfo<BibEntry, Summary>> entries;
    private final BibDatabaseContext bibDatabaseContext;
    private final SummariesStorage summariesStorage;
    private final ChatLanguageModel chatLanguageModel;
    private final TemplatesService templatesService;
    private final ReadOnlyBooleanProperty shutdownSignal;
    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;
    private final TaskExecutor taskExecutor;

    private final ProgressCounter progressCounter = new ProgressCounter();

    private String currentFile = "";

    public GenerateSummaryForSeveralTask(
            StringProperty groupName,
            List<ProcessingInfo<BibEntry, Summary>> entries,
            BibDatabaseContext bibDatabaseContext,
            SummariesStorage summariesStorage,
            ChatLanguageModel chatLanguageModel,
            TemplatesService templatesService,
            ReadOnlyBooleanProperty shutdownSignal,
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            TaskExecutor taskExecutor
    ) {
        this.groupName = groupName;
        this.entries = entries;
        this.bibDatabaseContext = bibDatabaseContext;
        this.summariesStorage = summariesStorage;
        this.chatLanguageModel = chatLanguageModel;
        this.templatesService = templatesService;
        this.shutdownSignal = shutdownSignal;
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;
        this.taskExecutor = taskExecutor;

        configure();
    }

    private void configure() {
        showToUser(true);
        titleProperty().set(Localization.lang("Generating summaries for %0", groupName.get()));
        groupName.addListener((o, oldValue, newValue) -> titleProperty().set(Localization.lang("Generating summaries for %0", newValue)));

        progressCounter.increaseWorkMax(entries.size());
        progressCounter.listenToAllProperties(this::updateProgress);
        updateProgress();
    }

    @Override
    public Void call() throws Exception {
        LOGGER.debug("Starting summaries generation of several files for {}", groupName.get());

        List<Pair<? extends Future<?>, BibEntry>> futures = new ArrayList<>();

        entries
                .stream()
                .map(processingInfo -> {
                    processingInfo.setState(ProcessingState.PROCESSING);
                    return new Pair<>(
                            new GenerateSummaryTask(
                                    processingInfo.getObject(),
                                    bibDatabaseContext,
                                    summariesStorage,
                                    chatLanguageModel,
                                    templatesService,
                                    shutdownSignal,
                                    aiPreferences,
                                    filePreferences
                            )
                                    .showToUser(false)
                                    .onSuccess(processingInfo::setSuccess)
                                    .onFailure(processingInfo::setException)
                                    .onFinished(() -> progressCounter.increaseWorkDone(1))
                                    .executeWith(taskExecutor),
                            processingInfo.getObject());
                })
                .forEach(futures::add);

        for (Pair<? extends Future<?>, BibEntry> pair : futures) {
            currentFile = pair.getValue().getCitationKey().orElse("<no citation key>");
            pair.getKey().get();
        }

        LOGGER.debug("Finished embeddings generation task of several files for {}", groupName.get());
        progressCounter.stop();
        return null;
    }

    private void updateProgress() {
        updateProgress(progressCounter.getWorkDone(), progressCounter.getWorkMax());
        updateMessage(progressCounter.getMessage() + " - " + currentFile + ", ...");
    }
}
