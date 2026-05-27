package org.jabref.gui.ai.chat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.tasks.GenerateRagResponseTask;
import org.jabref.logic.ai.chatting.util.ChatHistoryUtils;
import org.jabref.logic.ai.chatting.util.ChatModelFactory;
import org.jabref.logic.ai.embedding.AsyncEmbeddingModel;
import org.jabref.logic.ai.embedding.EmbeddingModelCache;
import org.jabref.logic.ai.embedding.EmbeddingModelFactory;
import org.jabref.logic.ai.followup.tasks.GenerateFollowUpQuestions;
import org.jabref.logic.ai.ingestion.IngestionTaskAggregator;
import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTask;
import org.jabref.logic.ai.ingestion.tasks.generateembeddings.GenerateEmbeddingsTaskRequest;
import org.jabref.logic.ai.ingestion.util.DocumentSplitterFactory;
import org.jabref.logic.ai.ingestion.util.FileHasher;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.ai.rag.util.AnswerEngineFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.ObservablesHelper;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.entry.LinkedFile;

import com.google.common.collect.Comparators;
import com.tobiasdiez.easybind.EasyBind;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChatViewModel extends AbstractViewModel {
    public enum State {
        AI_TURNED_OFF,
        NO_FILES,
        IDLE,
        WAITING_FOR_MESSAGE,
        ERROR
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatViewModel.class);

    private static final String EXAMPLE_QUESTION_1 = Localization.lang("What is the goal of the paper?");
    private static final String EXAMPLE_QUESTION_2 = Localization.lang("Which methods were used in the research?");
    private static final String EXAMPLE_QUESTION_3 = Localization.lang("What are the key findings?");

    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.IDLE);
    private final ObjectProperty<AnswerEngine> answerEngine = new SimpleObjectProperty<>();
    private final ListProperty<FullBibEntry> entries = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasks = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<GenerateRagResponseTask> generateRagResponseTask = new SimpleObjectProperty<>();
    private BackgroundTask<List<String>> generateFollowUpQuestionsTask;

    private final ListProperty<String> followUpQuestions = new SimpleListProperty<>(FXCollections.observableArrayList(
            EXAMPLE_QUESTION_1,
            EXAMPLE_QUESTION_2,
            EXAMPLE_QUESTION_3
    ));

    private final ObjectProperty<ChatModel> chatModel = new SimpleObjectProperty<>();
    private final ObjectProperty<AsyncEmbeddingModel> embeddingModel = new SimpleObjectProperty<>();
    private final ObjectProperty<DocumentSplitter> documentSplitter = new SimpleObjectProperty<>();

    private final TreeMap<List<FullBibEntry>, GenerateRagResponseTask> tasksMap =
            new TreeMap<>(Comparators.lexicographical(Comparator.comparing(id -> id.entry().getId())));

    private final TreeMap<List<FullBibEntry>, List<String>> followUpQuestionsCache =
            new TreeMap<>(Comparators.lexicographical(Comparator.comparing(id -> id.entry().getId())));

    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;
    private final DialogService dialogService;
    private final IngestionTaskAggregator ingestionTaskAggregator;
    private final IngestedDocumentsRepository ingestedDocumentsRepository;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModelCache embeddingModelCache;
    private final TaskExecutor taskExecutor;

    private final ListProperty<ChatMessage> chatHistory = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final StringProperty systemMessageTemplate = new SimpleStringProperty();
    private final StringProperty userMessageTemplate = new SimpleStringProperty();

    private List<FullBibEntry> currentEntriesSnapshot = new ArrayList<>();

    public AiChatViewModel(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            DialogService dialogService,
            IngestionTaskAggregator ingestionTaskAggregator,
            IngestedDocumentsRepository ingestedDocumentsRepository,
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModelCache embeddingModelCache,
            TaskExecutor taskExecutor
    ) {
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;
        this.dialogService = dialogService;
        this.ingestionTaskAggregator = ingestionTaskAggregator;
        this.ingestedDocumentsRepository = ingestedDocumentsRepository;
        this.embeddingStore = embeddingStore;
        this.embeddingModelCache = embeddingModelCache;
        this.taskExecutor = taskExecutor;

        setupBindings();
        setupValues();
        setupListeners();
    }

    private void setupBindings() {
        systemMessageTemplate.bind(aiPreferences.chattingSystemMessageTemplateProperty());
        userMessageTemplate.bind(aiPreferences.chattingUserMessageTemplateProperty());

        this.embeddingModel.bind(ObservablesHelper.createClosableObjectBinding(
                () -> EmbeddingModelFactory.create(aiPreferences, embeddingModelCache),
                aiPreferences.getEmbeddingsProperties()
        ));

        this.documentSplitter.bind(ObservablesHelper.createObjectBinding(
                () -> DocumentSplitterFactory.create(aiPreferences),
                aiPreferences.getDocumentSplitterProperties()
        ));

        BooleanBinding isAiTurnedOff = aiPreferences.enableAiProperty().not();
        BooleanBinding isWaiting = generateRagResponseTask.isNotNull();
        BooleanBinding hasNoFiles = Bindings.createBooleanBinding(() ->
                        entries.get() == null ||
                                entries.isEmpty() ||
                                entries.stream().flatMap(identifier -> identifier.entry().getFiles().stream()).findAny().isEmpty(),
                entries, aiPreferences.enableAiProperty()
        );

        BooleanBinding isError = Bindings.createBooleanBinding(() -> {
            if (chatHistory.isEmpty()) {
                return false;
            }
            return chatHistory.getLast().role() == ChatMessage.Role.ERROR;
        }, chatHistory);

        BindingsHelper.bindEnum(
                state,
                State.IDLE,

                Map.entry(State.AI_TURNED_OFF, isAiTurnedOff),
                Map.entry(State.WAITING_FOR_MESSAGE, isWaiting),
                Map.entry(State.NO_FILES, hasNoFiles),
                Map.entry(State.ERROR, isError)
        );
    }

    private void setupValues() {
        answerEngine.setValue(AnswerEngineFactory.create(
                aiPreferences.getAnswerEngineKind(),
                filePreferences,
                embeddingModel.get(),
                embeddingStore,
                aiPreferences.getRagMinScore(),
                aiPreferences.getRagMaxResultsCount()
        ));

        chatModel.setValue(ChatModelFactory.create(aiPreferences));
    }

    private void setupListeners() {
        BindingsHelper.listenToListChange(
                entries,
                this::changeEmbeddingTasks
        );

        EasyBind.subscribe(
                systemMessageTemplate,
                _ -> ChatHistoryUtils.updateSystemMessage(chatHistory, systemMessageTemplate.get())
        );
    }

    private void changeEmbeddingTasks() {
        if (!aiPreferences.getEnableAi() || entries.isEmpty()) {
            return;
        }

        if (!currentEntriesSnapshot.isEmpty()) {
            followUpQuestionsCache.put(new ArrayList<>(currentEntriesSnapshot), new ArrayList<>(followUpQuestions));
        }

        if (generateFollowUpQuestionsTask != null && !generateFollowUpQuestionsTask.isCancelled()) {
            generateFollowUpQuestionsTask.cancel();
            generateFollowUpQuestionsTask = null;
        }

        currentEntriesSnapshot = new ArrayList<>(entries);
        List<String> cached = followUpQuestionsCache.get(currentEntriesSnapshot);
        if (cached != null) {
            followUpQuestions.setAll(cached);
        } else {
            followUpQuestions.clear();
            followUpQuestions.addAll(
                    EXAMPLE_QUESTION_1,
                    EXAMPLE_QUESTION_2,
                    EXAMPLE_QUESTION_3
            );
        }

        generateEmbeddingsTasks.clear();
        generateRagResponseTask.set(tasksMap.get(entries));

        entries.forEach(identifier ->
                identifier.entry().getFiles().forEach(file -> {
                            if (checkIngested(identifier, file)) {
                                return;
                            }

                            // [impl->req~ai.ingestion.trigger-on-demand~1]
                            GenerateEmbeddingsTask task = ingestionTaskAggregator.start(
                                    new GenerateEmbeddingsTaskRequest(
                                            filePreferences,
                                            ingestedDocumentsRepository,
                                            embeddingStore,
                                            embeddingModel.get(),
                                            documentSplitter.get(),
                                            identifier.databaseContext(),
                                            file
                                    )
                            );

                            generateEmbeddingsTasks.add(task);
                        }
                )
        );
    }

    private boolean checkIngested(FullBibEntry fullBibEntry, LinkedFile linkedFile) {
        Optional<Path> path = linkedFile.findIn(fullBibEntry.databaseContext(), filePreferences);

        if (path.isEmpty()) {
            return false;
        }

        Optional<String> hash = FileHasher.computeHash(path.get());

        return hash.map(ingestedDocumentsRepository::isDocumentIngested).orElse(false);
    }

    public void showInfo() {
        AiChatStatusWindow window = new AiChatStatusWindow();

        window.chatModelProperty().bind(chatModel);
        window.entriesProperty().bind(entries);
        window.generateEmbeddingsTasksProperty().bind(generateEmbeddingsTasks);
        window.chatHistoryProperty().bind(chatHistory);

        window.setAnswerEngine(answerEngine.get());

        dialogService.showCustomDialogAndWait(window);

        answerEngine.set(window.answerEngineProperty().get());
    }

    public void sendMessage(String userMessage) {
        assert state.get() == State.IDLE;

        if (StringUtil.isBlank(userMessage)) {
            return;
        }

        followUpQuestions.clear();
        clearGenerateRagResponseTask();

        ChatMessage userChatMessage = ChatMessage.userMessage(userMessage);
        chatHistory.add(userChatMessage);

        GenerateRagResponseTask task = new GenerateRagResponseTask(
                chatModel.get(),
                answerEngine.get(),
                chatHistory,
                entries.get(),
                systemMessageTemplate.get(),
                userMessageTemplate.get()
        );

        List<FullBibEntry> taskEntries = entries.get();

        final ObservableList<ChatMessage> originalChatHistory = chatHistory.get();

        task.onSuccess(aiMessage -> {
            originalChatHistory.add(aiMessage);

            if (aiPreferences.getGenerateFollowUpQuestions() && chatModel.get() != null) {
                scheduleFollowUpQuestionsGeneration(userMessage, aiMessage.content());
            }
        });

        task.onFailure(ex ->
                // [impl->req~ai.chat.show-errors~1]
                originalChatHistory.add(ChatMessage.errorMessage(ex)));

        task.onFinished(() -> {
            tasksMap.remove(taskEntries);
            if (generateRagResponseTask.get() == task) {
                generateRagResponseTask.set(null);
            }
        });

        task.executeWith(taskExecutor);
        generateRagResponseTask.set(task);
        tasksMap.put(taskEntries, task);
    }

    private void scheduleFollowUpQuestionsGeneration(String userMessage, String aiResponse) {
        ChatModel currentChatModel = chatModel.get();
        if (currentChatModel == null) {
            return;
        }

        if (generateFollowUpQuestionsTask != null && !generateFollowUpQuestionsTask.isCancelled()) {
            generateFollowUpQuestionsTask.cancel();
        }

        List<FullBibEntry> entriesSnapshot = new ArrayList<>(entries);

        final ObservableList<String> originalFollowUpQuestions = followUpQuestions.get();

        generateFollowUpQuestionsTask = new GenerateFollowUpQuestions(
                currentChatModel,
                aiPreferences,
                userMessage,
                aiResponse
        );

        generateFollowUpQuestionsTask
                .onSuccess(questions -> {
                    originalFollowUpQuestions.setAll(questions);
                    followUpQuestionsCache.put(entriesSnapshot, new ArrayList<>(questions));
                })
                .onFailure(ex -> LOGGER.error("Failed to generate follow-up questions", ex))
                .executeWith(taskExecutor);
    }

    public void sendFollowUpMessage(String question) {
        followUpQuestions.clear();
        sendMessage(question);
    }

    private void clearGenerateRagResponseTask() {
        if (generateRagResponseTask.get() != null) {
            if (!generateRagResponseTask.get().isCancelled()) {
                generateRagResponseTask.get().cancel();
            }
            generateRagResponseTask.set(null);
        }
    }

    public void cancel() {
        assert state.get() == State.WAITING_FOR_MESSAGE || state.get() == State.ERROR;

        if (state.get() == State.WAITING_FOR_MESSAGE) {
            clearGenerateRagResponseTask();
        } else if (state.get() == State.ERROR) {
            if (!chatHistory.isEmpty()) {
                chatHistory.removeLast();
            }
        }
        followUpQuestions.clear();
    }

    public void delete(String id) {
        assert state.get() == State.IDLE;
        ChatHistoryUtils.delete(chatHistory, id);
    }

    public void regenerate(String id) {
        assert state.get() == State.ERROR || state.get() == State.IDLE;

        Optional<String> contentToRegenerate = ChatHistoryUtils.regenerate(chatHistory, id);

        contentToRegenerate.ifPresent(this::sendMessage);
    }

    public void regenerate() {
        if (!chatHistory.isEmpty()) {
            regenerate(chatHistory.getLast().id());
        }
    }

    public ListProperty<FullBibEntry> entriesProperty() {
        return entries;
    }

    public ListProperty<ChatMessage> chatHistoryProperty() {
        return chatHistory;
    }

    public ObjectProperty<State> stateProperty() {
        return state;
    }

    public ObjectProperty<ChatModel> chatModelProperty() {
        return chatModel;
    }

    public ObjectProperty<AnswerEngine> answerEngineProperty() {
        return answerEngine;
    }

    public ListProperty<GenerateEmbeddingsTask> generateEmbeddingsTasksProperty() {
        return generateEmbeddingsTasks;
    }

    public ListProperty<String> followUpQuestionsProperty() {
        return followUpQuestions;
    }
}
