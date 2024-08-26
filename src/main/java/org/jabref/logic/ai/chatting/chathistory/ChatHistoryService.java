package org.jabref.logic.ai.chatting.chathistory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.StateManager;
import org.jabref.logic.ai.util.CitationKeyCheck;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.AbstractGroup;

import com.airhacks.afterburner.injection.Injector;
import dev.langchain4j.data.message.ChatMessage;

/**
 * Main class for getting and storing chat history for entries and groups.
 * Use this class in the logic and UI.
 * <p>
 * The returned chat history is a {@link ObservableList}. So chat history exists for every possible
 * {@link BibEntry} and {@link AbstractGroup}. The chat history is stored in runtime.
 * <p>
 * To save and load chat history, {@link BibEntry} and {@link AbstractGroup} must satisfy several constraints.
 * Serialization and deserialization is handled in {@link ChatHistoryStorage}.
 * <p>
 * Constraints for serialization and deserialization of a chat history of a {@link BibEntry}:
 * 1. There should exist an associated {@link BibDatabaseContext} for the {@link BibEntry}.
 * 2. The database path of the associated {@link BibDatabaseContext} must be set.
 * 3. The citation key of the {@link BibEntry} must be set and unique.
 * <p>
 * Constraints for serialization and deserialization of a chat history of an {@link AbstractGroup}:
 * 1. There should exist an associated {@link BibDatabaseContext} for the {@link AbstractGroup}.
 * 2. The database path of the associated {@link BibDatabaseContext} must be set.
 * 3. The name of an {@link AbstractGroup} must be set and unique (this requirement is possibly already satisfied in
 *    JabRef, but for {@link BibEntry} it is definitely not).
 */
public class ChatHistoryService implements AutoCloseable {
    private final StateManager stateManager = Injector.instantiateModelOrService(StateManager.class);

    private final CitationKeyPatternPreferences citationKeyPatternPreferences;

    private final ChatHistoryStorage implementation;

    private record ChatHistoryManagementRecord(Optional<BibDatabaseContext> bibDatabaseContext, ObservableList<ChatMessage> chatHistory) { }

    private final Map<BibEntry, ChatHistoryManagementRecord> bibEntriesChatHistory = new HashMap<>();
    private final Map<AbstractGroup, ChatHistoryManagementRecord> groupsChatHistory = new HashMap<>();

    public ChatHistoryService(CitationKeyPatternPreferences citationKeyPatternPreferences,
                              ChatHistoryStorage implementation) {
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
        this.implementation = implementation;
    }

    public ObservableList<ChatMessage> getChatHistoryForEntry(BibEntry entry) {
        return bibEntriesChatHistory.computeIfAbsent(entry, entryArg -> {
            Optional<BibDatabaseContext> bibDatabaseContext = findBibDatabaseForEntry(entry);

            if (bibDatabaseContext.isEmpty() || entry.getCitationKey().isEmpty() || !correctCitationKey(bibDatabaseContext.get(), entry) || bibDatabaseContext.get().getDatabasePath().isEmpty()) {
                return new ChatHistoryManagementRecord(bibDatabaseContext, FXCollections.observableArrayList());
            } else {
                return new ChatHistoryManagementRecord(
                        bibDatabaseContext,
                        FXCollections.observableArrayList(
                                implementation.loadMessagesForEntry(
                                        bibDatabaseContext.get().getDatabasePath().get(),
                                        entry.getCitationKey().get()
                                )
                        )
                );
            }
        }).chatHistory;
    }

    /**
     * Removes the chat history for the given {@link AbstractGroup} from the internal RAM map.
     * If the {@link AbstractGroup} satisfies requirements for serialization and deserialization of chat history (see
     * the docstring for the {@link ChatHistoryService}), then the chat history will be stored via the
     * {@link ChatHistoryStorage}.
     * <p>
     * It is not necessary to call this method (everything will be stored in {@link ChatHistoryService#close()},
     * but it's best to call it when the chat history {@link AbstractGroup} is no longer needed.
     */
    public void closeChatHistoryForEntry(BibEntry entry) {
        ChatHistoryManagementRecord chatHistoryManagementRecord = bibEntriesChatHistory.get(entry);
        if (chatHistoryManagementRecord == null) {
            return;
        }

        Optional<BibDatabaseContext> bibDatabaseContext = chatHistoryManagementRecord.bibDatabaseContext();

        if (bibDatabaseContext.isPresent() && entry.getCitationKey().isPresent() && correctCitationKey(bibDatabaseContext.get(), entry) && bibDatabaseContext.get().getDatabasePath().isPresent()) {
            // Method `correctCitationKey` will already check `entry.getCitationKey().isPresent()`, but it is still
            // there, to suppress warning from IntelliJ IDEA on `entry.getCitationKey().get()`.
            implementation.storeMessagesForEntry(
                    bibDatabaseContext.get().getDatabasePath().get(),
                    entry.getCitationKey().get(),
                    chatHistoryManagementRecord.chatHistory()
            );
        }

        // TODO: What if there is two AI chats for the same entry? And one is closed and one is not?
        bibEntriesChatHistory.remove(entry);
    }

    public ObservableList<ChatMessage> getChatHistoryForGroup(AbstractGroup group) {
        return groupsChatHistory.computeIfAbsent(group, groupArg -> {
            Optional<BibDatabaseContext> bibDatabaseContext = findBibDatabaseForGroup(group);

            if (bibDatabaseContext.isEmpty() || bibDatabaseContext.get().getDatabasePath().isEmpty()) {
                return new ChatHistoryManagementRecord(bibDatabaseContext, FXCollections.observableArrayList());
            } else {
                return new ChatHistoryManagementRecord(
                        bibDatabaseContext,
                        FXCollections.observableArrayList(
                                implementation.loadMessagesForGroup(
                                        bibDatabaseContext.get().getDatabasePath().get(),
                                        group.nameProperty().get()
                                )
                        )
                );
            }
        }).chatHistory;
    }

    /**
     * Removes the chat history for the given {@link AbstractGroup} from the internal RAM map.
     * If the {@link AbstractGroup} satisfies requirements for serialization and deserialization of chat history (see
     * the docstring for the {@link ChatHistoryService}), then the chat history will be stored via the
     * {@link ChatHistoryStorage}.
     * <p>
     * It is not necessary to call this method (everything will be stored in {@link ChatHistoryService#close()},
     * but it's best to call it when the chat history {@link AbstractGroup} is no longer needed.
     */
    public void closeChatHistoryForGroup(AbstractGroup group) {
        ChatHistoryManagementRecord chatHistoryManagementRecord = groupsChatHistory.get(group);
        if (chatHistoryManagementRecord == null) {
            return;
        }

        Optional<BibDatabaseContext> bibDatabaseContext = chatHistoryManagementRecord.bibDatabaseContext();

        if (bibDatabaseContext.isPresent() && bibDatabaseContext.get().getDatabasePath().isPresent()) {
            implementation.storeMessagesForGroup(
                    bibDatabaseContext.get().getDatabasePath().get(),
                    group.nameProperty().get(),
                    chatHistoryManagementRecord.chatHistory()
            );
        }

        // TODO: What if there is two AI chats for the same entry? And one is closed and one is not?
        groupsChatHistory.remove(group);
    }

    private boolean correctCitationKey(BibDatabaseContext bibDatabaseContext, BibEntry bibEntry) {
        if (!CitationKeyCheck.citationKeyIsValid(bibDatabaseContext, bibEntry)) {
            tryToGenerateCitationKey(bibDatabaseContext, bibEntry);
        }

        return CitationKeyCheck.citationKeyIsValid(bibDatabaseContext, bibEntry);
    }

    private void tryToGenerateCitationKey(BibDatabaseContext bibDatabaseContext, BibEntry bibEntry) {
        new CitationKeyGenerator(bibDatabaseContext, citationKeyPatternPreferences).generateAndSetKey(bibEntry);
    }

    private Optional<BibDatabaseContext> findBibDatabaseForEntry(BibEntry entry) {
        return stateManager
                .getOpenDatabases()
                .stream()
                .filter(dbContext -> dbContext.getDatabase().getEntries().contains(entry))
                .findFirst();
    }

    private Optional<BibDatabaseContext> findBibDatabaseForGroup(AbstractGroup group) {
        return stateManager
                .getOpenDatabases()
                .stream()
                .filter(dbContext ->
                        dbContext.getMetaData().groupsBinding().get().map(groupTreeNode ->
                                groupTreeNode.containsGroup(group)
                        ).orElse(false)
                )
                .findFirst();
    }

    @Override
    public void close() {
        // We need to clone `bibEntriesChatHistory.keySet()` because closeChatHistoryForEntry() modifies the `bibEntriesChatHistory` map.
        new HashSet<>(bibEntriesChatHistory.keySet()).forEach(this::closeChatHistoryForEntry);

        // Clone is for the same reason, as written above.
        new HashSet<>(groupsChatHistory.keySet()).forEach(this::closeChatHistoryForGroup);

        implementation.commit();
    }
}
