package org.jabref.gui.ai.summary;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.util.ChatModelFactory;
import org.jabref.logic.ai.ingestion.logic.parsing.UniversalContentParser;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.InMemorySummaryCache;
import org.jabref.logic.ai.summarization.SummarizationTaskAggregator;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.logic.ai.summarization.tasks.GenerateSummaryTask;
import org.jabref.logic.ai.summarization.tasks.GenerateSummaryTaskRequest;
import org.jabref.logic.ai.summarization.util.SummarizatorFactory;
import org.jabref.logic.ai.util.TrackedBackgroundTask;
import org.jabref.logic.util.ObservablesHelper;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;

import com.google.common.eventbus.Subscribe;
import org.jspecify.annotations.Nullable;

public class AiSummaryViewModel extends AbstractViewModel {
    public enum State {
        AI_TURNED_OFF,
        NO_FILES,
        NO_SUPPORTED_FILE_TYPES,
        PROCESSING,
        DONE,
        ERROR_WHILE_GENERATING,
        READY,
        CANCELLED
    }

    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.AI_TURNED_OFF);
    private final ObjectProperty<Exception> error = new SimpleObjectProperty<>(null);
    private final ObjectProperty<AiSummary> summary = new SimpleObjectProperty<>();

    private final ObjectProperty<FullBibEntry> entry = new SimpleObjectProperty<>();
    private final ObjectProperty<ChatModel> chatModel = new SimpleObjectProperty<>();
    private final ObjectProperty<Summarizator> summarizator = new SimpleObjectProperty<>();

    private final ObjectProperty<GenerateSummaryTask> currentTask = new SimpleObjectProperty<>();
    private final ChangeListener<TrackedBackgroundTask.Status> taskStateListener = (_, _, value) -> updateByTaskState(value);

    // These properties are used to control the NO_FILES and NO_SUPPORTED_FILE_TYPES states.
    // They aare made as properties and not as bindings, as we need to listen to the chnages of files in an entry,
    // which is made by listening to the even bus and manually updating the properties.
    private final BooleanProperty filesEmpty = new SimpleBooleanProperty(true);
    private final BooleanProperty noSupportedFileTypes = new SimpleBooleanProperty(true);

    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;
    private final SummariesRepository summariesRepository;
    private final InMemorySummaryCache inMemoryCache;
    private final SummarizationTaskAggregator summarizationTaskAggregator;
    private final DialogService dialogService;

    public AiSummaryViewModel(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            SummariesRepository summariesRepository,
            InMemorySummaryCache inMemoryCache,
            SummarizationTaskAggregator summarizationTaskAggregator,
            DialogService dialogService
    ) {
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;
        this.summariesRepository = summariesRepository;
        this.inMemoryCache = inMemoryCache;
        this.summarizationTaskAggregator = summarizationTaskAggregator;
        this.dialogService = dialogService;

        setupBindings();
        setupListeners();
    }

    private void setupBindings() {
        BindingsHelper.bindEnum(
                state,
                State.READY,

                Map.entry(State.AI_TURNED_OFF,
                        aiPreferences.enableAiProperty().not()
                ),

                Map.entry(State.NO_FILES,
                        filesEmpty
                ),

                Map.entry(State.NO_SUPPORTED_FILE_TYPES,
                        noSupportedFileTypes
                ),

                Map.entry(State.DONE,
                        summary.isNotNull()
                ),

                Map.entry(State.CANCELLED,
                        currentTask.map(TrackedBackgroundTask::getStatus)
                                   .map(s -> s == TrackedBackgroundTask.Status.CANCELLED)
                                   .orElse(false)
                ),

                Map.entry(State.ERROR_WHILE_GENERATING,
                        error.isNotNull()
                ),

                Map.entry(State.PROCESSING,
                        currentTask.isNotNull()
                )
        );

        Function<GenerateSummaryTask, ReadOnlyObjectProperty<TrackedBackgroundTask.Status>> propertyExtractor = GenerateSummaryTask::statusProperty;

        currentTask.addListener((_, oldVal, newVal) -> {
            if (oldVal != null) {
                propertyExtractor.apply(oldVal).removeListener(taskStateListener);
            }
            if (newVal != null) {
                propertyExtractor.apply(newVal).addListener(taskStateListener);
            }
        });

        this.chatModel.bind(ObservablesHelper.createClosableObjectBinding(
                () -> ChatModelFactory.create(aiPreferences),
                aiPreferences.getChatProperties()
        ));

        setupSummarizatorBinding();
    }

    private void setupSummarizatorBinding() {
        summarizator.bind(ObservablesHelper.createObjectBinding(
                () -> SummarizatorFactory.create(aiPreferences),
                aiPreferences.getSummarizatorProperties()
        ));
    }

    private void setupListeners() {
        entry.addListener((_, oldVal, newVal) -> {
            if (oldVal != null) {
                oldVal.entry().unregisterListener(this);
            }

            if (newVal == null) {
                filesEmpty.set(true);
                noSupportedFileTypes.set(true);
            } else {
                newVal.entry().registerListener(this);
                refreshFileState(newVal.entry().getFiles());
            }
        });

        BindingsHelper.listen(this::prepareForEntry, entry);
        BindingsHelper.listen(this::processEntry, entry, noSupportedFileTypes, filesEmpty);
    }

    @Subscribe
    public void onFileFieldChanged(FieldChangedEvent event) {
        if (!event.getField().equals(StandardField.FILE)) {
            return;
        }

        FullBibEntry fullEntry = entry.get();
        if (fullEntry == null) {
            return;
        }

        UiTaskExecutor.runInJavaFXThread(() -> refreshFileState(fullEntry.entry().getFiles()));
    }

    private void refreshFileState(List<LinkedFile> files) {
        filesEmpty.set(files.isEmpty());
        noSupportedFileTypes.set(files.stream()
                                      .map(f -> Path.of(f.getLink()))
                                      .noneMatch(UniversalContentParser::isSupportedFileType));
    }

    /// Resets the chat model and summarizator to the default values from AI preferences.
    /// Called before generating a summary to ensure default models are used
    /// (as opposed to a custom summarizator set by {@link #regenerateCustom()}).
    private void setDefaultModels() {
        summarizator.unbind();
        setupSummarizatorBinding();
    }

    private void clearTask() {
        if (currentTask.get() != null) {
            currentTask.set(null);
        }
    }

    public void regenerate() {
        regenerate(getEntry());
    }

    public void regenerateCustom() {
        regenerateCustom(getEntry());
    }

    public void generate() {
        generate(getEntry());
    }

    public void cancel() {
        if (currentTask.get() != null) {
            currentTask.get().cancel();
        }
    }

    private void prepareForEntry() {
        clearTask();
        summary.set(null);
        error.set(null);
    }

    private void processEntry() {
        if (entry.get() == null || state.get() != State.READY) {
            return;
        }

        // THe retrieval is done in 4 steps:
        // 1. Check the repository if there is a generated entry.
        // 2. Check the in-memory cache.
        // 3. Check if there is a running task.
        // 4. Otherwise, start a new task.

        Optional<AiSummary> persistedSummary = entry.get().toAiSummaryIdentifier()
                                                    .flatMap(summariesRepository::get);
        if (persistedSummary.isPresent()) {
            this.summary.set(persistedSummary.get());
            return;
        }

        Optional<AiSummary> cachedSummary = inMemoryCache.get(entry.get().entry());
        if (cachedSummary.isPresent()) {
            this.summary.set(cachedSummary.get());
            return;
        }

        Optional<GenerateSummaryTask> runningTask = summarizationTaskAggregator.getTask(entry.get().entry());
        if (runningTask.isPresent()) {
            GenerateSummaryTask task = runningTask.get();
            currentTask.set(task);
            summarizator.unbind();
            summarizator.set(task.getRequest().summarizator());
            chatModel.set(task.getRequest().chatModel());

            switch (task.getStatus()) {
                case SUCCESS -> {
                    summary.set(task.getResult());
                    clearTask();
                }
                case ERROR -> {
                    error.set(task.getException());
                    clearTask();
                }
                default -> {
                }
            }
            return;
        }

        generate();
    }

    private void regenerate(FullBibEntry identifier) {
        clearSummary(identifier);
        generate(identifier);
    }

    private void regenerateCustom(FullBibEntry identifier) {
        if (identifier == null) {
            return;
        }

        AiSummaryParametersDialog parametersDialog = new AiSummaryParametersDialog();
        Optional<Boolean> result = dialogService.showCustomDialogAndWait(parametersDialog);

        if (result.isEmpty() || !result.get()) {
            return;
        }

        @Nullable Summarizator customSummarizator = parametersDialog.summarizatorProperty().get();

        if (customSummarizator == null) {
            return;
        }

        summarizator.unbind();
        summarizator.set(customSummarizator);

        clearSummary(identifier);
        startSummarization(identifier);
    }

    private void generate(FullBibEntry identifier) {
        setDefaultModels();
        clearSummary(identifier);
        startSummarization(identifier);
    }

    public void clearSummary(FullBibEntry fullEntry) {
        if (fullEntry == null) {
            return;
        }

        inMemoryCache.remove(fullEntry.entry());

        fullEntry.toAiSummaryIdentifier()
                 .ifPresent(summariesRepository::clear);

        summary.set(null);
    }

    private void startSummarization(FullBibEntry fullEntry) {
        if (fullEntry == null) {
            return;
        }

        GenerateSummaryTask task = summarizationTaskAggregator.start(
                new GenerateSummaryTaskRequest(
                        filePreferences,
                        chatModel.get(),
                        summarizator.get(),
                        fullEntry,
                        true
                )
        );

        currentTask.set(task);
    }

    private void updateByTaskState(TrackedBackgroundTask.Status value) {
        GenerateSummaryTask task = currentTask.get();
        if (task == null) {
            return;
        }

        UiTaskExecutor.runInJavaFXThread(() -> {
            switch (value) {
                case TrackedBackgroundTask.Status.ERROR -> {
                    error.set(task.getException());
                    clearTask();
                }
                case TrackedBackgroundTask.Status.SUCCESS -> {
                    summary.set(task.getResult());
                    clearTask();
                }
            }
        });
    }

    public ObjectProperty<FullBibEntry> entryProperty() {
        return entry;
    }

    public FullBibEntry getEntry() {
        return entry.get();
    }

    public void setEntry(FullBibEntry entry) {
        this.entry.set(entry);
    }

    public ObjectProperty<State> stateProperty() {
        return state;
    }

    public ObjectProperty<Exception> errorProperty() {
        return error;
    }

    public ObjectProperty<AiSummary> summaryProperty() {
        return summary;
    }

    public ObjectProperty<Summarizator> summarizatorProperty() {
        return summarizator;
    }

    public ObjectProperty<ChatModel> chatModelProperty() {
        return chatModel;
    }
}
