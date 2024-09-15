package org.jabref.logic.ai.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Pair;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.processingstatus.ProcessingInfo;
import org.jabref.logic.ai.processingstatus.ProcessingState;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.ProgressCounter;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This task generates embeddings for several {@link LinkedFile} (typically used for groups).
 * It will check if embeddings were already generated.
 * And it also will store the embeddings.
 */
public class GenerateEmbeddingsForSeveralTask extends BackgroundTask<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateEmbeddingsForSeveralTask.class);

    private final StringProperty name;
    private final List<ProcessingInfo<LinkedFile, Void>> linkedFiles;
    private final FileEmbeddingsManager fileEmbeddingsManager;
    private final BibDatabaseContext bibDatabaseContext;
    private final FilePreferences filePreferences;
    private final TaskExecutor taskExecutor;
    private final ReadOnlyBooleanProperty shutdownSignal;

    private final ProgressCounter progressCounter = new ProgressCounter();

    private String currentFile = "";

    public GenerateEmbeddingsForSeveralTask(
            StringProperty name,
            List<ProcessingInfo<LinkedFile, Void>> linkedFiles,
            FileEmbeddingsManager fileEmbeddingsManager,
            BibDatabaseContext bibDatabaseContext,
            FilePreferences filePreferences,
            TaskExecutor taskExecutor,
            ReadOnlyBooleanProperty shutdownSignal
    ) {
        this.name = name;
        this.linkedFiles = linkedFiles;
        this.fileEmbeddingsManager = fileEmbeddingsManager;
        this.bibDatabaseContext = bibDatabaseContext;
        this.filePreferences = filePreferences;
        this.taskExecutor = taskExecutor;
        this.shutdownSignal = shutdownSignal;

        configure(name);
    }

    private void configure(StringProperty name) {
        showToUser(true);
        titleProperty().set(Localization.lang("Generating embeddings for %0", name.get()));
        name.addListener((o, oldValue, newValue) -> titleProperty().set(Localization.lang("Generating embeddings for %0", newValue)));

        progressCounter.increaseWorkMax(linkedFiles.size());
        progressCounter.listenToAllProperties(this::updateProgress);
        updateProgress();
    }

    @Override
    public Void call() throws Exception {
        LOGGER.debug("Starting embeddings generation of several files for {}", name.get());

        List<Pair<? extends Future<?>, String>> futures = new ArrayList<>();
        linkedFiles
                .stream()
                .map(processingInfo -> {
                    processingInfo.setState(ProcessingState.PROCESSING);
                    return new Pair<>(
                            new GenerateEmbeddingsTask(
                                    processingInfo.getObject(),
                                    fileEmbeddingsManager,
                                    bibDatabaseContext,
                                    filePreferences,
                                    shutdownSignal
                            )
                                    .onSuccess(v -> processingInfo.setState(ProcessingState.SUCCESS))
                                    .onFailure(processingInfo::setException)
                                    .onFinished(() -> progressCounter.increaseWorkDone(1))
                                    .executeWith(taskExecutor),
                            processingInfo.getObject().getLink());
                })
                .forEach(futures::add);

        for (Pair<? extends Future<?>, String> pair : futures) {
            currentFile = pair.getValue();
            pair.getKey().get();
        }

        LOGGER.debug("Finished embeddings generation task of several files for {}", name.get());
        progressCounter.stop();
        return null;
    }

    private void updateProgress() {
        updateProgress(progressCounter.getWorkDone(), progressCounter.getWorkMax());
        updateMessage(progressCounter.getMessage() + " - " + currentFile + ", ...");
    }
}
