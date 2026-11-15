package org.jabref.gui.ai.chat;

import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.chatting.InMemoryChatHistoryCache;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;

import com.tobiasdiez.easybind.Subscription;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class AiEntryChatViewModel extends AbstractViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiEntryChatViewModel.class);

    private final BooleanProperty enabled = new SimpleBooleanProperty();
    private final ReadOnlyBooleanWrapper historyLoaded = new ReadOnlyBooleanWrapper();
    private final ObjectProperty<FullBibEntry> selectedEntry = new SimpleObjectProperty<>();
    private final ListProperty<FullBibEntry> entries = new SimpleListProperty<>(FXCollections.observableArrayList(
            entryIdentifier -> entryIdentifier.entry().getObservables()
    ));
    private final ListProperty<ChatMessage> chatHistory = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final AiPreferences aiPreferences;
    private final InMemoryChatHistoryCache chatHistoryCache;
    private final Subscription selectionSubscription;
    private long loadGeneration;

    public AiEntryChatViewModel(
            AiPreferences aiPreferences,
            InMemoryChatHistoryCache chatHistoryCache
    ) {
        this.aiPreferences = aiPreferences;
        this.chatHistoryCache = chatHistoryCache;

        setupBindings();
        selectionSubscription = BindingsHelper.listen(() -> load(selectedEntry.get()), selectedEntry, enabled);
    }

    private void setupBindings() {
        enabled.bind(aiPreferences.aiFeaturesEnabledCurrentlyProperty());
    }

    private void load(@Nullable FullBibEntry identifier) {
        long generation = ++loadGeneration;
        historyLoaded.set(false);
        chatHistory.set(FXCollections.observableArrayList());
        entries.set(FXCollections.observableArrayList(entryIdentifier -> entryIdentifier.entry().getObservables()));
        if (!enabled.get()) {
            return;
        }

        Optional.ofNullable(identifier).ifPresent(entry -> loadEntry(entry, generation));
    }

    private void loadEntry(FullBibEntry identifier, long generation) {
        ObservableList<FullBibEntry> observableEntries = FXCollections.observableArrayList(
                entryIdentifier -> entryIdentifier.entry().getObservables()
        );
        observableEntries.add(identifier);
        entries.set(observableEntries);

        chatHistoryCache.getForEntryAsync(identifier.databaseContext(), identifier.entry())
                        .whenComplete((history, failure) -> UiTaskExecutor.runNowOrInJavaFXThread(() -> {
                            if (generation != loadGeneration) {
                                return;
                            }
                            Optional.ofNullable(failure).ifPresentOrElse(
                                    error -> LOGGER.error("Unable to load entry chat", error),
                                    () -> {
                                        chatHistory.set(history);
                                        historyLoaded.set(true);
                                    });
                        }));
    }

    public ObjectProperty<FullBibEntry> selectedEntryProperty() {
        return selectedEntry;
    }

    public BooleanProperty enabledProperty() {
        return enabled;
    }

    public ReadOnlyBooleanProperty historyLoadedProperty() {
        return historyLoaded.getReadOnlyProperty();
    }

    public ListProperty<FullBibEntry> entriesProperty() {
        return entries;
    }

    public ListProperty<ChatMessage> chatHistoryProperty() {
        return chatHistory;
    }

    public void dispose() {
        loadGeneration++;
        selectionSubscription.unsubscribe();
        historyLoaded.set(false);
        chatHistory.set(FXCollections.observableArrayList());
        entries.set(FXCollections.observableArrayList(entryIdentifier -> entryIdentifier.entry().getObservables()));
    }
}
