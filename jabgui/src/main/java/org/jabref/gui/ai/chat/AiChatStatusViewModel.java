package org.jabref.gui.ai.chat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javafx.beans.Observable;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.exporters.AiChatJsonExporter;
import org.jabref.logic.ai.chatting.exporters.AiChatMarkdownExporter;
import org.jabref.logic.ai.chatting.util.ChatModelFactory;
import org.jabref.logic.ai.embedding.AsyncEmbeddingModel;
import org.jabref.logic.ai.embedding.EmbeddingModelCache;
import org.jabref.logic.ai.embedding.EmbeddingModelFactory;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.ai.rag.util.AnswerEngineFactory;
import org.jabref.logic.ai.util.TrackedBackgroundTask;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.ObservablesHelper;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.ai.AiMetadata;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChatStatusViewModel extends AbstractViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatStatusViewModel.class);

    public enum FileStatus {
        PENDING(Localization.lang("Pending")),
        PROCESSING(Localization.lang("Processing")),
        ERROR_WHILE_PROCESSING(Localization.lang("Error")),
        INGESTED(Localization.lang("Ingested")),
        CANCELLED(Localization.lang("Cancelled"));

        private final String displayName;

        FileStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasks = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final Map<GenerateEmbeddingsTask, ChangeListener<? super TrackedBackgroundTask.Status>> taskListeners = new HashMap<>();
    private final ListProperty<FullBibEntry> entries = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObservableList<IngestionStatusRow> ingestionStatuses = FXCollections.observableArrayList(row ->
            new Observable[] {row.statusProperty(), row.errorProperty()}
    );

    private final ListProperty<AnswerEngineKind> answerEngineKinds = new SimpleListProperty<>(FXCollections.observableArrayList(AnswerEngineKind.values()));
    private final ObjectProperty<AnswerEngineKind> selectedAnswerEngineKind = new SimpleObjectProperty<>();

    private final ObjectProperty<AnswerEngine> answerEngine = new SimpleObjectProperty<>();
    private final ObjectProperty<ChatModel> chatModel = new SimpleObjectProperty<>();
    private final ObjectProperty<AsyncEmbeddingModel> embeddingModel = new SimpleObjectProperty<>();

    private final ListProperty<ChatMessage> chatHistory = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;
    private final FieldPreferences fieldPreferences;
    private final BibEntryTypesManager entryTypesManager;
    private final DialogService dialogService;
    private final EmbeddingModelCache embeddingModelCache;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public AiChatStatusViewModel(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            FieldPreferences fieldPreferences,
            BibEntryTypesManager entryTypesManager,
            DialogService dialogService,
            EmbeddingModelCache embeddingModelCache,
            EmbeddingStore<TextSegment> embeddingStore
    ) {
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;
        this.fieldPreferences = fieldPreferences;
        this.entryTypesManager = entryTypesManager;
        this.dialogService = dialogService;
        this.embeddingModelCache = embeddingModelCache;
        this.embeddingStore = embeddingStore;

        setupValues();
        setupBindings();
        setupListeners();
    }

    private void setupValues() {
        selectedAnswerEngineKind.set(aiPreferences.getAnswerEngineKind());
    }

    private void setupBindings() {
        this.chatModel.bind(ObservablesHelper.createClosableObjectBinding(
                () -> ChatModelFactory.create(aiPreferences),
                aiPreferences.getChatProperties()
        ));

        this.embeddingModel.bind(ObservablesHelper.createClosableObjectBinding(
                () -> EmbeddingModelFactory.create(aiPreferences, embeddingModelCache),
                aiPreferences.getEmbeddingsProperties()
        ));

        this.answerEngine.bind(ObservablesHelper.createObjectBinding(
                () -> AnswerEngineFactory.create(
                        selectedAnswerEngineKind.get(),
                        filePreferences,
                        embeddingModel.get(),
                        embeddingStore,
                        aiPreferences.getRagMinScore(),
                        aiPreferences.getRagMaxResultsCount()
                ),
                Stream.concat(
                        aiPreferences.getAnswerEngineProperties().stream(),
                        Stream.of(selectedAnswerEngineKind)
                ).toList()
        ));
    }

    private void setupListeners() {
        BindingsHelper.listenToListContentChanges(generateEmbeddingsTasks, this::wireTask, this::unwireTask);
    }

    private void wireTask(GenerateEmbeddingsTask task) {
        if (taskListeners.containsKey(task)) {
            return;
        }

        UiTaskExecutor.runInJavaFXThread(() -> getOrCreateRow(task.getLinkedFile()));

        ChangeListener<Object> statusListener = (_, _, _) -> processTask(task);
        taskListeners.put(task, statusListener);
        task.statusProperty().addListener(statusListener);

        processTask(task);
    }

    private void unwireTask(GenerateEmbeddingsTask task) {
        ChangeListener<? super TrackedBackgroundTask.Status> listener = taskListeners.remove(task);
        if (listener != null) {
            task.statusProperty().removeListener(listener);
        }

        UiTaskExecutor.runInJavaFXThread(() ->
                ingestionStatuses.removeIf(row -> row.getLinkedFile().equals(task.getLinkedFile()))
        );
    }

    private IngestionStatusRow getOrCreateRow(LinkedFile file) {
        return ingestionStatuses.stream()
                                .filter(row -> row.getLinkedFile().equals(file))
                                .findFirst()
                                .orElseGet(() -> {
                                    IngestionStatusRow newRow = new IngestionStatusRow(file);
                                    ingestionStatuses.add(newRow);
                                    return newRow;
                                });
    }

    private void processTask(GenerateEmbeddingsTask task) {
        UiTaskExecutor.runInJavaFXThread(() -> {
            IngestionStatusRow row = getOrCreateRow(task.getLinkedFile());

            switch (task.getStatus()) {
                case SUCCESS -> {
                    row.errorProperty().set(null);
                    row.statusProperty().set(FileStatus.INGESTED);
                }
                case ERROR -> {
                    row.errorProperty().set(task.getException());
                    row.statusProperty().set(FileStatus.ERROR_WHILE_PROCESSING);
                }
                case PENDING ->
                        row.statusProperty().set(FileStatus.PENDING);
                case PROCESSING ->
                        row.statusProperty().set(FileStatus.PROCESSING);
                case CANCELLED ->
                        row.statusProperty().set(FileStatus.CANCELLED);
            }
        });
    }

    public void exportMarkdown() {
        List<ChatMessage> messages = chatHistory.get();

        if (messages == null || messages.isEmpty()) {
            dialogService.notify(Localization.lang("No chat history available to export"));
            return;
        }

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.MARKDOWN)
                .withDefaultExtension(StandardFileType.MARKDOWN)
                .withInitialDirectory(Directories.getUserDirectory())
                .build();

        dialogService.showFileSaveDialog(fileDialogConfiguration)
                     .ifPresent(path -> {
                         try {
                             AiChatMarkdownExporter exporter = new AiChatMarkdownExporter(entryTypesManager, fieldPreferences, aiPreferences.getMarkdownChatExportTemplate());
                             String content = exporter.export(buildMetadata(), getBibEntriesFromFullEntries(), getDatabaseModeOrDefault(), messages);
                             Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                             dialogService.notify(Localization.lang("Export operation finished successfully."));
                         } catch (IOException e) {
                             LOGGER.error("Problem occurred while writing the export file", e);
                             dialogService.showErrorDialogAndWait(Localization.lang("Problem occurred while writing the export file"), e);
                         }
                     });
    }

    public void exportJson() {
        List<ChatMessage> messages = chatHistory.get();

        if (messages == null || messages.isEmpty()) {
            dialogService.notify(Localization.lang("No chat history available to export"));
            return;
        }

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.JSON)
                .withDefaultExtension(StandardFileType.JSON)
                .withInitialDirectory(Directories.getUserDirectory())
                .build();

        dialogService.showFileSaveDialog(fileDialogConfiguration)
                     .ifPresent(path -> {
                         try {
                             AiChatJsonExporter exporter = new AiChatJsonExporter(entryTypesManager, fieldPreferences);
                             String content = exporter.export(buildMetadata(), getBibEntriesFromFullEntries(), getDatabaseModeOrDefault(), messages);
                             Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                             dialogService.notify(Localization.lang("Export operation finished successfully."));
                         } catch (IOException e) {
                             LOGGER.error("Problem occurred while writing the export file", e);
                             dialogService.showErrorDialogAndWait(Localization.lang("Problem occurred while writing the export file"), e);
                         }
                     });
    }

    public void setAnswerEngine(AnswerEngine answerEngine) {
        selectedAnswerEngineKind.set(answerEngine.getKind());
    }

    public void clearChatHistory() {
        // [guard->req~ai.chat.clear-history~1]
        boolean confirmed = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Clear chat history"),
                Localization.lang("Are you sure you want to clear the chat history?")
        );

        if (confirmed) {
            chatHistory.clear();
        }
    }

    private AiMetadata buildMetadata() {
        ChatModel model = chatModel.get();
        if (model == null) {
            return new AiMetadata(null, "", Instant.now());
        }

        return new AiMetadata(model.getAiProvider(), model.getName(), Instant.now());
    }

    private List<BibEntry> getBibEntriesFromFullEntries() {
        return entries.stream()
                      .map(FullBibEntry::entry)
                      .toList();
    }

    private BibDatabaseMode getDatabaseModeOrDefault() {
        return entries.isEmpty()
               ? BibDatabaseMode.BIBTEX
               : entries.getFirst().databaseContext().getMode();
    }

    public ObservableList<IngestionStatusRow> getIngestionStatuses() {
        return ingestionStatuses;
    }

    public ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasksProperty() {
        return generateEmbeddingsTasks;
    }

    public ListProperty<FullBibEntry> entriesProperty() {
        return entries;
    }

    public ObjectProperty<AnswerEngine> answerEngineProperty() {
        return answerEngine;
    }

    public ListProperty<AnswerEngineKind> answerEngineKindsProperty() {
        return answerEngineKinds;
    }

    public ObjectProperty<AnswerEngineKind> selectedAnswerEngineKindProperty() {
        return selectedAnswerEngineKind;
    }

    public ObjectProperty<ChatModel> chatModelProperty() {
        return chatModel;
    }

    public ListProperty<ChatMessage> chatHistoryProperty() {
        return chatHistory;
    }

    public static class IngestionStatusRow {
        private final LinkedFile linkedFile;
        private final ReadOnlyStringWrapper name;
        private final ObjectProperty<FileStatus> status;
        private final ObjectProperty<Exception> error;

        public IngestionStatusRow(LinkedFile linkedFile) {
            this.linkedFile = linkedFile;
            this.name = new ReadOnlyStringWrapper(linkedFile.getLink());
            this.status = new SimpleObjectProperty<>(FileStatus.PENDING);
            this.error = new SimpleObjectProperty<>();
        }

        public ReadOnlyStringWrapper nameProperty() {
            return name;
        }

        public ObjectProperty<FileStatus> statusProperty() {
            return status;
        }

        public FileStatus getStatus() {
            return status.get();
        }

        public ObjectProperty<Exception> errorProperty() {
            return error;
        }

        public Exception getError() {
            return error.get();
        }

        public LinkedFile getLinkedFile() {
            return linkedFile;
        }
    }
}
