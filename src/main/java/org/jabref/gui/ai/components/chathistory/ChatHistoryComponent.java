package org.jabref.gui.ai.components.chathistory;

import java.util.function.Consumer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.ai.components.chatmessage.ChatMessageComponent;
import org.jabref.logic.ai.misc.ErrorMessage;

import com.airhacks.afterburner.views.ViewLoader;
import dev.langchain4j.data.message.ChatMessage;

public class ChatHistoryComponent extends ScrollPane {
    @FXML private VBox vBox;

    private final ObjectProperty<Consumer<Integer>> deleteMessageCallback = new SimpleObjectProperty<>();

    public ChatHistoryComponent() {
        ViewLoader.view(this)
                  .root(this)
                  .load();

        deleteMessageCallback.addListener((observable, oldValue, newValue) ->
            vBox.getChildren().forEach(child -> ((ChatMessageComponent) child).setOnDelete(generateDeleteMessageCallback()))
        );

        this.needsLayoutProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                scrollDown();
            }
        });
    }

    private Consumer<ChatMessageComponent> generateDeleteMessageCallback() {
        return chatMessageComponent -> {
            int index = vBox.getChildren().indexOf(chatMessageComponent);
            deleteMessageCallback.get().accept(index);
        };
    }

    public void setDeleteMessageCallback(Consumer<Integer> deleteMessageCallback) {
        this.deleteMessageCallback.set(deleteMessageCallback);
    }

    public void scrollDown() {
        this.layout();
        this.setVvalue(this.getVmax());
    }

    public void addMessage(ChatMessage chatMessage) {
        ChatMessageComponent component = new ChatMessageComponent(chatMessage, generateDeleteMessageCallback());
        vBox.getChildren().add(component);
        scrollDown();
    }

    // WARNING: This method won't call `deleteMessageCallback` for the deleted message.
    public void deleteMessage(int index) {
        vBox.getChildren().remove(index);
    }

    // WARNING: This method won't call `deleteMessageCallback` for each message that is deleted.
    public void clearAll() {
        vBox.getChildren().clear();
    }

    private void removeLastMessage() {
        if (!vBox.getChildren().isEmpty()) {
            deleteMessage(vBox.getChildren().size() - 1);
        }
    }
}
