package org.jabref.gui.ai.components.aichat.chatprompt;

import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;

import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.gemsfx.ExpandingTextArea;

public class ChatPromptComponent extends HBox {
    // If current message that user is typing in prompt is non-existent, new, or empty, then we use
    // this value in currentUserMessageScroll.
    private static final int NEW_NON_EXISTENT_MESSAGE = -1;

    private final ObjectProperty<Consumer<String>> sendCallback = new SimpleObjectProperty<>();
    private final ObjectProperty<Consumer<String>> retryCallback = new SimpleObjectProperty<>();
    private final ObjectProperty<Runnable> cancelCallback = new SimpleObjectProperty<>();
    private final ObjectProperty<Runnable> regenerateCallback = new SimpleObjectProperty<>();

    private final ListProperty<String> history = new SimpleListProperty<>(FXCollections.observableArrayList());

    // This property stores index of a user history message.
    // When user scrolls history in the prompt, this value is updated.
    // Whenever user edits the prompt, this value is reset to NEW_NON_EXISTENT_MESSAGE.
    private final IntegerProperty currentUserMessageScroll = new SimpleIntegerProperty(NEW_NON_EXISTENT_MESSAGE);

    // If the current content of the prompt is a history message, then this property is true.
    // If user begins to edit or type a new text, then this property is false.
    private final BooleanProperty showingHistoryMessage = new SimpleBooleanProperty(false);

    @FXML private ExpandingTextArea userPromptTextArea;
    @FXML private Button submitButton;
    @FXML private Button regenerateButton;

    private String lastUserPrompt = null;

    public ChatPromptComponent() {
        ViewLoader.view(this)
                  .root(this)
                  .load();

        history.addListener((observable, oldValue, newValue) -> {
            currentUserMessageScroll.set(NEW_NON_EXISTENT_MESSAGE);
            showingHistoryMessage.set(false);
        });
    }

    public void setSendCallback(Consumer<String> sendCallback) {
        this.sendCallback.set(sendCallback);
    }

    public void setRetryCallback(Consumer<String> retryCallback) {
        this.retryCallback.set(retryCallback);
    }

    public void setCancelCallback(Runnable cancelCallback) {
        this.cancelCallback.set(cancelCallback);
    }

    public void setRegenerateCallback(Runnable regenerateCallback) {
        this.regenerateCallback.set(regenerateCallback);
    }

    public ListProperty<String> getHistory() {
        return history;
    }

    @FXML
    private void initialize() {
        userPromptTextArea.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.DOWN) {
                // Do not go down in the history.
                if (currentUserMessageScroll.get() != NEW_NON_EXISTENT_MESSAGE) {
                    showingHistoryMessage.set(true);
                    currentUserMessageScroll.set(currentUserMessageScroll.get() - 1);

                    // There could be two effects after setting the properties:
                    // 1) User scrolls to a recent message, then we should properly update the prompt text.
                    // 2) Scroll is set to -1 (which is NEW_NON_EXISTENT_MESSAGE) and we should clear the prompt text.
                    // On the second event currentUserMessageScroll will be set to -1 and showingHistoryMessage
                    // will be true (this is important).
                }
            } else if (keyEvent.getCode() == KeyCode.UP) {
                // [impl->req~ai.chat.new-message-based-on-previous~1]
                if ((currentUserMessageScroll.get() < history.get().size() - 1) && (userPromptTextArea.getText().isEmpty() || showingHistoryMessage.get())) {
                    // 1. We should not go up the maximum number of user messages.
                    // 2. We can scroll history only on two conditions:
                    //    1) The prompt is empty.
                    //    2) User has already been scrolling the history.
                    showingHistoryMessage.set(true);
                    currentUserMessageScroll.set(currentUserMessageScroll.get() + 1);
                }
            } else {
                // Cursor left/right should not stop history scrolling
                if (keyEvent.getCode() != KeyCode.RIGHT && keyEvent.getCode() != KeyCode.LEFT) {
                    // It is okay to go back and forth in the prompt while showing a history message.
                    // But if user begins doing something else, we should not track the history and reset
                    // all the properties.
                    showingHistoryMessage.set(false);
                    currentUserMessageScroll.set(NEW_NON_EXISTENT_MESSAGE);
                }

                if (keyEvent.getCode() == KeyCode.ENTER) {
                    if (keyEvent.isControlDown()) {
                        userPromptTextArea.appendText("\n");
                    } else {
                        onSendMessage();
                    }
                }
            }
        });

        currentUserMessageScroll.addListener((observable, oldValue, newValue) -> {
            // When currentUserMessageScroll is reset, then its value is
            // 1) either to NEW_NON_EXISTENT_MESSAGE,
            // 2) or to a new history entry.
            if (newValue.intValue() != NEW_NON_EXISTENT_MESSAGE && showingHistoryMessage.get()) {
                if (userPromptTextArea.getCaretPosition() == 0 || !userPromptTextArea.getText().contains("\n")) {
                    // If there are new lines in the prompt, then it is ambiguous whether the user tries to scroll up or down in history or editing lines in the current prompt.
                    // The easy way to get rid of this ambiguity is to disallow scrolling when there are new lines in the prompt.
                    // But the exception to this situation is when the caret position is at the beginning of the prompt.
                    history.get().stream()
                           .skip(newValue.intValue())
                           .findFirst()
                           .ifPresent(message -> userPromptTextArea.setText(message));
                }
            } else {
                // When currentUserMessageScroll is set to NEW_NON_EXISTENT_MESSAGE, then we should:
                // 1) either clear the prompt, if user scrolls down the most recent history entry.
                // 2) do nothing, if user starts to edit the history entry.
                // We distinguish these two cases by checking showingHistoryMessage, which is true for -1 message, and false for others.
                if (showingHistoryMessage.get()) {
                    userPromptTextArea.setText("");
                }
            }
        });
    }

    public void setDisableToButtons(boolean disable) {
        this.getChildren().forEach(node -> node.setDisable(disable));
    }

    public void switchToErrorState(String userMessage) {
        this.getChildren().clear();

        Button retryButton = new Button(Localization.lang("Retry"));

        retryButton.setOnAction(event -> {
            if (retryCallback.get() != null) {
                retryCallback.get().accept(userMessage);
            }
        });

        Button cancelButton = new Button(Localization.lang("Cancel"));

        cancelButton.setOnAction(event -> {
            if (cancelCallback.get() != null) {
                cancelCallback.get().run();
            }
        });

        this.getChildren().add(retryButton);
        this.getChildren().add(cancelButton);
    }

    public void switchToNormalState() {
        this.getChildren().clear();
        this.getChildren().add(userPromptTextArea);
        this.getChildren().add(submitButton);
        this.getChildren().add(regenerateButton);
        requestPromptFocus();
    }

    public void requestPromptFocus() {
        // TODO: Check what would happen when programmer calls requestPromptFocus() while the component is in error state.
        Platform.runLater(() -> userPromptTextArea.requestFocus());
    }

    @FXML
    private void onSendMessage() {
        String userPrompt = userPromptTextArea.getText().trim();
        userPromptTextArea.clear();

        if (!userPrompt.isEmpty() && sendCallback.get() != null) {
            lastUserPrompt = userPrompt; // 🔹 Saving last message for regeneration
            sendCallback.get().accept(userPrompt);
        }
    }

    @FXML
    private void onRegenerateMessage() {
        if (regenerateCallback.get() != null) {
            regenerateCallback.get().run();
        }
    }

    public String getLastUserPrompt() {
        return lastUserPrompt;
    }
}
