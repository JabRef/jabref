package org.jabref.gui.ai.components.aichat.chathistory;

import java.util.function.Consumer;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.ai.components.aichat.chatmessage.ChatMessageComponent;
import org.jabref.gui.util.UiTaskExecutor;

import com.airhacks.afterburner.views.ViewLoader;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;

public class ChatHistoryComponent extends ScrollPane {
    @FXML private VBox vBox;

    private Consumer<String> regenerateCallback;

    public ChatHistoryComponent() {
        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.needsLayoutProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                scrollDown();
            }
        });
    }

    /**
     * @implNote You must call this method only once.
     */
    public void setItems(ObservableList<ChatMessage> items) {
        fill(items);
        items.addListener((ListChangeListener<? super ChatMessage>) obs -> fill(items));
    }

    public void setRegenerateCallback(Consumer<String> regenerateCallback) {
        this.regenerateCallback = regenerateCallback;
    }

    private void fill(ObservableList<ChatMessage> items) {
        UiTaskExecutor.runInJavaFXThread(() -> {
            vBox.getChildren().clear();
            // Go through the message list, create message components, and set regenerate functionality for AI messages
            for (ChatMessage chatMessage : items) {
                ChatMessageComponent component = new ChatMessageComponent(chatMessage, comp -> {
                    // Directly remove the corresponding message based on the message instance instead of UI index lookup
                    items.remove(chatMessage);
                });
                // If the message is an AI message, set the regenerate callback
                if (chatMessage instanceof AiMessage) {
                    component.setOnRegenerate(comp -> {
                        // Check if the current AI message is the last one, and ensure there are at least 2 messages (a user message must exist)
                        if (!items.isEmpty() && items.get(items.size() - 1) == chatMessage && items.size() > 1) {
                            ChatMessage previous = items.get(items.size() - 2);
                            if (previous instanceof UserMessage) {
                                String userText = ((UserMessage) previous).singleText();
                                // Remove the last 2 messages: first the AI message, then the corresponding user message
                                items.remove(items.size() - 1);
                                items.remove(items.size() - 1);
                                if (regenerateCallback != null) {
                                    regenerateCallback.accept(userText);
                                }
                            }
                        }
                    });
                }
                vBox.getChildren().add(component);
            }
        });
    }

    public void scrollDown() {
        this.layout();
        this.setVvalue(this.getVmax());
    }
}

