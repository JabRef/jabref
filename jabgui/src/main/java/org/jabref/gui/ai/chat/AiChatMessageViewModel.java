package org.jabref.gui.ai.chat;

import java.time.Instant;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.input.ClipboardContent;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.ai.AiNamingUtils;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.ai.chatting.ChatMessage;

public class AiChatMessageViewModel extends AbstractViewModel {
    private final ObjectProperty<ChatMessage> chatMessage = new SimpleObjectProperty<>();

    private final StringProperty id = new SimpleStringProperty("");
    private final StringProperty source = new SimpleStringProperty("");
    private final StringProperty messageContent = new SimpleStringProperty("");
    private final ObjectProperty<Instant> timestamp = new SimpleObjectProperty<>(Instant.now());

    private final BooleanProperty showDelete = new SimpleBooleanProperty(true);
    private final BooleanProperty showRegenerate = new SimpleBooleanProperty(false);

    private final ObjectProperty<EventHandler<ActionEvent>> onDelete = new SimpleObjectProperty<>();
    private final ObjectProperty<EventHandler<ActionEvent>> onRegenerate = new SimpleObjectProperty<>();

    private final ClipBoardManager clipBoardManager;

    public AiChatMessageViewModel(ClipBoardManager clipBoardManager) {
        this.clipBoardManager = clipBoardManager;

        setupBindings();
    }

    private void setupBindings() {
        id.bind(chatMessage.map(ChatMessage::id));
        source.bind(chatMessage
                .map(ChatMessage::role)
                .map(AiNamingUtils::getDisplayName));
        messageContent.bind(chatMessage.map(ChatMessage::content).map(StringUtil::makeSafe));
        timestamp.bind(chatMessage.map(ChatMessage::timestamp));

        showRegenerate.bind(chatMessage.map(ChatMessage::role).map(ChatMessage.Role::canRegenerate));
    }

    public void delete() {
        BindingsHelper.handle(onDelete);
    }

    public void regenerate() {
        BindingsHelper.handle(onRegenerate);
    }

    public void copyToClipboard() {
        ClipboardContent content = new ClipboardContent();
        content.putString(messageContent.get());

        clipBoardManager.setContent(content);
    }

    public ObjectProperty<ChatMessage> chatMessageProperty() {
        return chatMessage;
    }

    public ReadOnlyStringProperty idProperty() {
        return id;
    }

    public ReadOnlyStringProperty sourceProperty() {
        return source;
    }

    public ReadOnlyStringProperty messageContentProperty() {
        return messageContent;
    }

    public ReadOnlyObjectProperty<Instant> timestampProperty() {
        return timestamp;
    }

    public ReadOnlyBooleanProperty showDeleteProperty() {
        return showDelete;
    }

    public ReadOnlyBooleanProperty showRegenerateProperty() {
        return showRegenerate;
    }

    public ObjectProperty<EventHandler<ActionEvent>> onDeleteProperty() {
        return onDelete;
    }

    public ObjectProperty<EventHandler<ActionEvent>> onRegenerateProperty() {
        return onRegenerate;
    }
}
