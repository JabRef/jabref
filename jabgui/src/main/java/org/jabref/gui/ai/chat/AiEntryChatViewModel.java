package org.jabref.gui.ai.chat;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.ai.chatting.InMemoryChatHistoryCache;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;

import com.tobiasdiez.easybind.EasyBind;

public class AiEntryChatViewModel extends AbstractViewModel {
    private final BooleanProperty enabled = new SimpleBooleanProperty();
    private final ObjectProperty<FullBibEntry> selectedEntry = new SimpleObjectProperty<>();
    private final ListProperty<FullBibEntry> entries = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<ChatMessage> chatHistory = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final AiPreferences aiPreferences;
    private final InMemoryChatHistoryCache chatHistoryCache;

    public AiEntryChatViewModel(
            AiPreferences aiPreferences,
            InMemoryChatHistoryCache chatHistoryCache
    ) {
        this.aiPreferences = aiPreferences;
        this.chatHistoryCache = chatHistoryCache;

        setupBindings();
        setupListeners();
    }

    private void setupBindings() {
        enabled.bind(aiPreferences.enableAiProperty());
    }

    private void setupListeners() {
        EasyBind.subscribe(selectedEntry, this::load);
    }

    private void load(FullBibEntry identifier) {
        if (identifier == null || !enabled.get()) {
            return;
        }

        entries.set(FXCollections.observableArrayList(identifier));

        chatHistory.set(chatHistoryCache.getForEntry(
                identifier.databaseContext(),
                identifier.entry()
        ));
    }

    public ObjectProperty<FullBibEntry> selectedEntryProperty() {
        return selectedEntry;
    }

    public BooleanProperty enabledProperty() {
        return enabled;
    }

    public ListProperty<FullBibEntry> entriesProperty() {
        return entries;
    }

    public ListProperty<ChatMessage> chatHistoryProperty() {
        return chatHistory;
    }
}
