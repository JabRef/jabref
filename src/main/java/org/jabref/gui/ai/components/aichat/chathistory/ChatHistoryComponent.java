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
            for (ChatMessage chatMessage : items) {
                ChatMessageComponent component = new ChatMessageComponent(chatMessage, comp -> {
                    items.remove(chatMessage);
                });
                if (chatMessage instanceof AiMessage) {
                    component.setOnRegenerate(comp -> {
                        if (items.size() > 1 && items.getLast() == chatMessage) {
                            ChatMessage previous = items.get(items.size() - 2);
                            if (previous instanceof UserMessage message) {
                                String userText = message.singleText();
                                // Remove the last two messages: first the AI message, then the corresponding user message
                                items.removeLast();
                                items.removeLast();
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

