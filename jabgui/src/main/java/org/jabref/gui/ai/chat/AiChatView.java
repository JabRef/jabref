package org.jabref.gui.ai.chat;

import java.util.List;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ListProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.AiPrivacyNoticeView;
import org.jabref.gui.ai.statuspane.UniversalStatusPaneView;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.ScrollUtils;
import org.jabref.gui.util.component.HistoryTextArea;
import org.jabref.gui.util.component.ListScrollPane;
import org.jabref.gui.util.component.MarkdownTextFlow;
import org.jabref.gui.util.component.SimpleListView;
import org.jabref.logic.ai.AiNamingUtils;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

/// General AI chat component with [org.jabref.model.entry.BibEntry]. Can be used for chatting with one entry ([AiEntryChatView]) or with several entries (like [AiGroupChatView]).
///
/// To set up this component, set or bind the [#entriesProperty()] and [#chatHistoryProperty()] properties.
// [impl->feat~ai.chatting~1]
public class AiChatView extends StackPane {
    @FXML private AiPrivacyNoticeView privacyNotice;
    @FXML private UniversalStatusPaneView restartNeededPane;
    @FXML private UniversalStatusPaneView noFilesErrorPane;
    @FXML private BorderPane mainContainer;

    @FXML private ListScrollPane<ChatMessage> chatHistoryScrollPane;

    @FXML private Pane transparentPane;
    @FXML private ProgressIndicator loadingIndicator;

    @FXML private HBox findBar;
    @FXML private TextField findField;
    @FXML private Label findResultLabel;

    @FXML private HBox followUpQuestionsArea;
    @FXML private SimpleListView<String> followUpQuestionsSimpleListView;

    @FXML private Button infoButton;
    // [impl->feat~ai.chat.smart-prompt-field~1]
    @FXML private HistoryTextArea userMessageTextArea;
    @FXML private Button sendButton;
    @FXML private Button retryButton;
    @FXML private Button cancelButton;

    @FXML private Label noticeText;

    @Inject private GuiPreferences preferences;
    @Inject private AiService aiService;
    @Inject private DialogService dialogService;
    @Inject private TaskExecutor taskExecutor;

    private AiChatViewModel viewModel;

    private int findTotal;
    private int findCurrent;

    public AiChatView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new AiChatViewModel(
                preferences.getAiPreferences(),
                preferences.getFilePreferences(),
                dialogService,
                aiService.getIngestionTaskAggregator(),
                aiService.getIngestedDocumentsRepository(),
                aiService.getEmbeddingsStore(),
                aiService.getEmbeddingModelCache(),
                taskExecutor
        );

        setupBindings();
        setupValues();
        setupFollowUpQuestions();
        setupFind();
    }

    /// Ctrl+F inside the chat searches the chat messages instead of the library, see [org.jabref.gui.frame.JabRefFrame].
    // [impl->feat~ai.chat.find~1]
    private void setupFind() {
        findBar.managedProperty().bind(findBar.visibleProperty());
        findBar.setVisible(false);

        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (preferences.getKeyBindingRepository().matches(event, KeyBinding.SEARCH)) {
                findBar.setVisible(true);
                findField.requestFocus();
                findField.selectAll();
                event.consume();
            }
        });
        findField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    findPrevious();
                } else {
                    findNext();
                }
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                closeFind();
                event.consume();
            }
        });
        findField.textProperty().addListener(_ -> {
            findCurrent = 0;
            updateFind(true);
        });
        // New or deleted messages are rendered after the list change, thus search them afterwards
        viewModel.chatHistoryProperty().addListener((ListChangeListener<ChatMessage>) _ -> Platform.runLater(() -> updateFind(false)));
    }

    /// Highlights all occurrences of the find query in the rendered messages and scrolls to the current one.
    private void updateFind(boolean scrollToCurrent) {
        String query = findBar.isVisible() ? findField.getText() : "";
        List<MarkdownTextFlow> flows = chatHistoryScrollPane.getContent() instanceof Pane content
                                       ? content.getChildrenUnmodifiable().stream()
                                                .filter(AiChatMessageView.class::isInstance)
                                                .map(node -> ((AiChatMessageView) node).getMarkdownTextFlow())
                                                .toList()
                                       : List.of();

        findTotal = 0;
        for (MarkdownTextFlow flow : flows) {
            findTotal += flow.highlightOccurrences(query, findCurrent - findTotal);
        }
        if (findTotal > 0 && findCurrent >= findTotal) {
            findCurrent = 0;
            updateFind(scrollToCurrent);
            return;
        }

        findResultLabel.setText(query.isEmpty() ? "" : (findTotal == 0 ? 0 : findCurrent + 1) + "/" + findTotal);
        if (scrollToCurrent) {
            flows.stream()
                 .flatMap(flow -> flow.getCurrentOccurrence().stream())
                 .findFirst()
                 .ifPresent(node -> ScrollUtils.scrollIntoScrollPane(chatHistoryScrollPane, node.localToScene(node.getBoundsInLocal())));
        }
    }

    @FXML
    private void findNext() {
        if (findTotal > 0) {
            findCurrent = (findCurrent + 1) % findTotal;
            updateFind(true);
        }
    }

    @FXML
    private void findPrevious() {
        if (findTotal > 0) {
            findCurrent = (findCurrent - 1 + findTotal) % findTotal;
            updateFind(true);
        }
    }

    @FXML
    private void closeFind() {
        findBar.setVisible(false);
        updateFind(false);
        chatHistoryScrollPane.requestFocus();
    }

    private void setupBindings() {
        chatHistoryScrollPane.itemsProperty().bind(viewModel.chatHistoryProperty());
        chatHistoryScrollPane.setRenderer(this::renderChatMessage);
        chatHistoryScrollPane.setAutoScrollToBottom(true);

        // [pp->feat~ai.ingestion.trigger-on-demand~1]
        privacyNotice.managedProperty().bind(privacyNotice.visibleProperty());
        restartNeededPane.managedProperty().bind(restartNeededPane.visibleProperty());
        noFilesErrorPane.managedProperty().bind(noFilesErrorPane.visibleProperty());
        mainContainer.managedProperty().bind(mainContainer.visibleProperty());
        loadingIndicator.managedProperty().bind(loadingIndicator.visibleProperty());
        transparentPane.managedProperty().bind(transparentPane.visibleProperty());
        infoButton.managedProperty().bind(infoButton.visibleProperty());
        userMessageTextArea.managedProperty().bind(userMessageTextArea.visibleProperty());
        sendButton.managedProperty().bind(sendButton.visibleProperty());
        retryButton.managedProperty().bind(retryButton.visibleProperty());
        cancelButton.managedProperty().bind(cancelButton.visibleProperty());
        followUpQuestionsArea.managedProperty().bind(followUpQuestionsArea.visibleProperty());

        BooleanBinding isAiTurnedOff = viewModel.stateProperty().isEqualTo(AiChatViewModel.State.AI_TURNED_OFF);
        BooleanBinding isRestartNeeded = viewModel.stateProperty().isEqualTo(AiChatViewModel.State.RESTART_NEEDED);
        BooleanBinding isNoFiles = viewModel.stateProperty().isEqualTo(AiChatViewModel.State.NO_FILES);
        BooleanBinding isWaiting = viewModel.stateProperty().isEqualTo(AiChatViewModel.State.WAITING_FOR_MESSAGE);
        BooleanBinding isError = viewModel.stateProperty().isEqualTo(AiChatViewModel.State.ERROR);
        BooleanBinding isIdle = viewModel.stateProperty().isEqualTo(AiChatViewModel.State.IDLE);

        privacyNotice.visibleProperty().bind(isAiTurnedOff);
        restartNeededPane.visibleProperty().bind(isRestartNeeded);
        noFilesErrorPane.visibleProperty().bind(isNoFiles);
        mainContainer.visibleProperty().bind(isAiTurnedOff.not()
                                                          .and(isNoFiles.not())
                                                          .and(isRestartNeeded.not()));

        loadingIndicator.visibleProperty().bind(isWaiting);
        transparentPane.visibleProperty().bind(isWaiting);
        userMessageTextArea.visibleProperty().bind(isIdle);
        sendButton.visibleProperty().bind(isIdle);
        retryButton.visibleProperty().bind(isError);
        cancelButton.visibleProperty().bind(isWaiting.or(isError));
        noticeText.textProperty().bind(viewModel.chatModelProperty().map(AiChatView::formatNoticeText));
    }

    private static String formatNoticeText(ChatModel model) {
        String modelName = AiNamingUtils.getDisplayName(model.getAiProvider()) + " " + model.getName();
        return Localization.lang("Current AI model: %0. The AI may generate inaccurate or inappropriate responses. Please verify any information provided.", modelName);
    }

    private void setupFollowUpQuestions() {
        followUpQuestionsArea.visibleProperty().bind(
                viewModel.followUpQuestionsProperty().emptyProperty().not()
                         .and(preferences.getAiPreferences().generateFollowUpQuestionsProperty())
                         .and(viewModel.stateProperty().isEqualTo(AiChatViewModel.State.IDLE))
        );

        followUpQuestionsSimpleListView.itemsProperty().bind(viewModel.followUpQuestionsProperty());
        followUpQuestionsSimpleListView.setRenderer(question -> {
            Button button = new Button(question);
            button.getStyleClass().addAll("exampleQuestionStyle", "padding-4-12");
            button.setOnAction(_ -> viewModel.sendFollowUpMessage(question));
            return button;
        });
    }

    private void setupValues() {
        userMessageTextArea.getHistory().addAll(
                viewModel
                        .chatHistoryProperty()
                        .stream()
                        .map(ChatMessage::content)
                        .filter(StringUtil::isNotBlank)
                        .toList()
        );
    }

    private Node renderChatMessage(ChatMessage chatMessage) {
        AiChatMessageView aiChatMessageView = new AiChatMessageView();

        aiChatMessageView.setChatMessage(chatMessage);
        aiChatMessageView.setOnDelete(_ -> viewModel.delete(chatMessage.id()));
        aiChatMessageView.setOnRegenerate(_ -> viewModel.regenerate(chatMessage.id()));

        return aiChatMessageView;
    }

    @FXML
    private void showInfo() {
        viewModel.showInfo();
    }

    @FXML
    private void send() {
        viewModel.sendMessage(userMessageTextArea.getText());
        userMessageTextArea.clear();
    }

    // [impl->feat~ai.chat.retry-error~1]
    @FXML
    private void retry() {
        viewModel.regenerate();
    }

    // [impl->feat~ai.chat.cancel-generation~1]
    // [impl->feat~ai.chat.cancel-error-state~1]
    @FXML
    private void cancel() {
        viewModel.cancel();
    }

    public ListProperty<ChatMessage> chatHistoryProperty() {
        return viewModel.chatHistoryProperty();
    }

    public ListProperty<FullBibEntry> entriesProperty() {
        return viewModel.entriesProperty();
    }
}
