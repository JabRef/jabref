package org.jabref.gui.ai.chat;

import java.util.List;
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
import org.jabref.gui.groups.GroupNodeViewModel;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chatting.InMemoryChatHistoryCache;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class AiGroupChatViewModel extends AbstractViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiGroupChatViewModel.class);

    private final BooleanProperty enabled = new SimpleBooleanProperty();
    private final ReadOnlyBooleanWrapper historyLoaded = new ReadOnlyBooleanWrapper();

    private final ObjectProperty<GroupNodeViewModel> groupNode = new SimpleObjectProperty<>();
    private final ObjectProperty<BibDatabaseContext> databaseContext = new SimpleObjectProperty<>();

    private final ListProperty<FullBibEntry> entries = new SimpleListProperty<>(FXCollections.observableArrayList(
            identifier -> identifier.entry().getObservables()
    ));
    private final ListProperty<ChatMessage> chatHistory = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final AiPreferences aiPreferences;
    private final InMemoryChatHistoryCache chatHistoryCache;
    private long loadGeneration;

    public AiGroupChatViewModel(AiPreferences aiPreferences, AiService aiService) {
        this.aiPreferences = aiPreferences;
        this.chatHistoryCache = aiService.getChatHistoryCache();

        enabled.bind(aiPreferences.aiFeaturesEnabledCurrentlyProperty());

        BindingsHelper.listen(this::loadGroupChat, groupNode, databaseContext, enabled);
    }

    private void loadGroupChat() {
        long generation = ++loadGeneration;
        historyLoaded.set(false);
        chatHistory.set(FXCollections.observableArrayList());
        entries.set(FXCollections.observableArrayList(identifier -> identifier.entry().getObservables()));
        if (!enabled.get()) {
            return;
        }

        Optional.ofNullable(groupNode.get()).ifPresent(group -> Optional.ofNullable(databaseContext.get())
                                                                        .ifPresent(context -> loadGroupChat(context, group, generation)));
    }

    private void loadGroupChat(BibDatabaseContext context, GroupNodeViewModel group, long generation) {
        assert context.getMetaData().getAiLibraryId().isPresent();

        List<BibEntry> matchedEntries = group.getGroupNode().findMatches(context.getDatabase());
        List<FullBibEntry> matchedEntryIdentifiers = FullBibEntry.fromSeveral(context, matchedEntries);

        ObservableList<FullBibEntry> observableEntries = FXCollections.observableArrayList(
                identifier -> identifier.entry().getObservables()
        );
        observableEntries.addAll(matchedEntryIdentifiers);
        entries.set(observableEntries);

        chatHistoryCache.getForGroupAsync(context, group.getGroupNode())
                        .whenComplete((history, failure) -> UiTaskExecutor.runNowOrInJavaFXThread(() -> {
                            if (generation != loadGeneration) {
                                return;
                            }
                            Optional.ofNullable(failure).ifPresentOrElse(
                                    error -> LOGGER.error("Unable to load group chat", error),
                                    () -> {
                                        chatHistory.set(history);
                                        historyLoaded.set(true);
                                    });
                        }));
    }

    public ObjectProperty<GroupNodeViewModel> groupNodeProperty() {
        return groupNode;
    }

    public ObjectProperty<BibDatabaseContext> databaseContextProperty() {
        return databaseContext;
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
}
