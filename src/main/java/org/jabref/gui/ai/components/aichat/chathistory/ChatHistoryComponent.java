package org.jabref.gui.ai.components.aichat.chathistory;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.ai.components.aichat.chatmessage.ChatMessageComponent;
import org.jabref.gui.util.UiTaskExecutor;

import com.airhacks.afterburner.views.ViewLoader;
import dev.langchain4j.data.message.ChatMessage;

public class ChatHistoryComponent extends ScrollPane {
    @FXML private VBox vBox;

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

    private void fill(ObservableList<ChatMessage> items) {
        UiTaskExecutor.runInJavaFXThread(() -> {
            vBox.getChildren().clear();
            items.forEach(chatMessage ->
                    vBox.getChildren().add(new ChatMessageComponent(chatMessage, chatMessageComponent -> {
                        int index = vBox.getChildren().indexOf(chatMessageComponent);
                        items.remove(index);
                    })));
        });
    }

    public void scrollDown() {
        this.layout();
        this.setVvalue(this.getVmax());
    }
}
