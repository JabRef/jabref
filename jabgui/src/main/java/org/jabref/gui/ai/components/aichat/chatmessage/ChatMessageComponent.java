package org.jabref.gui.ai.components.aichat.chatmessage;

import java.util.function.Consumer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.util.MarkdownTextFlow;
import org.jabref.logic.ai.util.ChatMessageUtils;
import org.jabref.logic.ai.util.ErrorMessage;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.injection.Injector;
import com.airhacks.afterburner.views.ViewLoader;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;

public class ChatMessageComponent extends HBox {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatMessageComponent.class);

    private final ObjectProperty<ChatMessage> chatMessage = new SimpleObjectProperty<>();
    private final ObjectProperty<Consumer<ChatMessageComponent>> onDelete = new SimpleObjectProperty<>();

    @FXML private HBox wrapperHBox;
    @FXML private VBox vBox;
    @FXML private Label sourceLabel;
    @FXML private Pane markdownContentPane;
    @FXML private VBox buttonsVBox;

    private final MarkdownTextFlow markdownTextFlow;
    private String selectedText = "";
    private final ClipBoardManager clipBoardManager = new ClipBoardManager(Injector.instantiateModelOrService(StateManager.class));

    public ChatMessageComponent() {
        ViewLoader.view(this)
                  .root(this)
                  .load();

        chatMessage.addListener((_, _, newValue) -> {
            if (newValue != null) {
                loadChatMessage();
            }
        });

        markdownTextFlow = new MarkdownTextFlow(markdownContentPane);
        markdownContentPane.getChildren().add(markdownTextFlow);
        markdownContentPane.minHeightProperty().bind(markdownTextFlow.heightProperty());
        markdownContentPane.prefHeightProperty().bind(markdownTextFlow.heightProperty());

        setupContextMenu();
    }

    public ChatMessageComponent(ChatMessage chatMessage, Consumer<ChatMessageComponent> onDeleteCallback) {
        this();
        setChatMessage(chatMessage);
        setOnDelete(onDeleteCallback);
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem copyItem = new MenuItem(Localization.lang("Copy"));

        copyItem.setOnAction(_ -> {
            String textToCopy = !this.selectedText.isEmpty()
                                ? this.selectedText
                                : ChatMessageUtils.getContent(chatMessage.get()).orElse("");

            if (!textToCopy.isEmpty()) {
                clipBoardManager.setContent(textToCopy);
            }
        });

        contextMenu.getItems().add(copyItem);

        markdownContentPane.addEventFilter(MOUSE_PRESSED, event -> {
            if (event.isSecondaryButtonDown()) {
                try {
                    int start = markdownTextFlow.getSelectionStartIndex();
                    int end = markdownTextFlow.getSelectionEndIndex();

                    String fullText = ChatMessageUtils.getContent(chatMessage.get()).orElse("");

                    if (start >= 0 && end > start && !fullText.isEmpty()) {
                        this.selectedText = fullText.substring(start, Math.min(end, fullText.length()));
                    } else {
                        this.selectedText = "";
                    }
                } catch (AssertionError | IndexOutOfBoundsException e) {
                    LOGGER.debug("Failed to extract selection indices for copy action", e);
                    this.selectedText = "";
                }
            }
        });

        markdownContentPane.setOnContextMenuRequested(event ->
                contextMenu.show(markdownContentPane, event.getScreenX(), event.getScreenY())
        );
    }

    public void setChatMessage(ChatMessage chatMessage) {
        this.chatMessage.set(chatMessage);
    }

    public ChatMessage getChatMessage() {
        return chatMessage.get();
    }

    public void setOnDelete(Consumer<ChatMessageComponent> onDeleteCallback) {
        this.onDelete.set(onDeleteCallback);
    }

    private void loadChatMessage() {
        switch (chatMessage.get()) {
            case UserMessage userMessage -> {
                setColor("-jr-ai-message-user", "-jr-ai-message-user-border");
                setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                wrapperHBox.setAlignment(Pos.TOP_RIGHT);
                sourceLabel.setText(Localization.lang("User"));
                markdownTextFlow.setMarkdown(userMessage.singleText());
            }

            case AiMessage aiMessage -> {
                setColor("-jr-ai-message-ai", "-jr-ai-message-ai-border");
                setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
                wrapperHBox.setAlignment(Pos.TOP_LEFT);
                sourceLabel.setText(Localization.lang("AI"));
                markdownTextFlow.setMarkdown(aiMessage.text());
            }

            case ErrorMessage errorMessage -> {
                setColor("-jr-ai-message-error", "-jr-ai-message-error-border");
                setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
                sourceLabel.setText(Localization.lang("Error"));
                markdownTextFlow.setMarkdown(errorMessage.getText());
            }

            default ->
                    LOGGER.error("ChatMessageComponent supports only user, AI, or error messages, but other type was passed: {}", chatMessage.get().type().name());
        }
    }

    @FXML
    private void initialize() {
        buttonsVBox.visibleProperty().bind(wrapperHBox.hoverProperty());
        HBox.setHgrow(this, Priority.ALWAYS);
    }

    @FXML
    private void onDeleteClick() {
        if (onDelete.get() != null) {
            onDelete.get().accept(this);
        }
    }

    private void setColor(String fillColor, String borderColor) {
        vBox.setStyle("-fx-background-color: " + fillColor + "; -fx-border-radius: 10; -fx-background-radius: 10; -fx-border-color: " + borderColor + "; -fx-border-width: 3;");
    }
}
