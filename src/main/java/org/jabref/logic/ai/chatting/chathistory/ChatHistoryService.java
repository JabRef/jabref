package org.jabref.logic.ai.chatting.chathistory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatHistoryService.class);

    private final StateManager stateManager = Injector.instantiateModelOrService(StateManager.class);

    private final CitationKeyPatternPreferences citationKeyPatternPreferences;

    private final ChatHistoryStorage implementation;

    private record ChatHistoryManagementRecord<T>(T object, Optional<BibDatabaseContext> bibDatabaseContext, ObservableList<ChatMessage> chatHistory) { }

    private final Map<String, ChatHistoryManagementRecord<BibEntry>> bibEntriesChatHistory = new HashMap<>();
    private final Map<AbstractGroup, ChatHistoryManagementRecord<AbstractGroup>> groupsChatHistory = new HashMap<>();

    public ChatHistoryService(CitationKeyPatternPreferences citationKeyPatternPreferences,
                              ChatHistoryStorage implementation) {
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
        this.implementation = implementation;

        stateManager.getOpenDatabases().addListener((ListChangeListener<BibDatabaseContext>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(bibDatabaseContext -> {
                        bibDatabaseContext.getMetaData().getGroups().ifPresent(groupTreeNode -> {
                            groupTreeNode.iterateOverTree().forEach(groupNode -> {
                                groupNode.getGroup().nameProperty().addListener((observable, oldValue, newValue) -> {
                                    if (newValue != null && oldValue != null) {
                                        transferGroupHistory(bibDatabaseContext, oldValue, newValue);
                                    }
                                });
                            });
                        });

                        bibDatabaseContext.getDatabase().getEntries().forEach(entry -> {
                            entry.getCiteKeyBinding().addListener((observable, oldValue, newValue) -> {
                                if (newValue.isPresent() && oldValue.isPresent()) {
                                    transferEntryHistory(bibDatabaseContext, oldValue.get(), newValue.get());
                                }
                            });
                        });
                    });
                }
            }
        });
    }

    public ObservableList<ChatMessage> getChatHistoryForEntry(BibEntry entry) {
        return bibEntriesChatHistory.computeIfAbsent(entry.getId(), entryArg -> {
            Optional<BibDatabaseContext> bibDatabaseContext = findBibDatabaseForEntry(entry);

            ObservableList<ChatMessage> chatHistory;

            if (bibDatabaseContext.isEmpty() || entry.getCitationKey().isEmpty() || !correctCitationKey(bibDatabaseContext.get(), entry) || bibDatabaseContext.get().getDatabasePath().isEmpty()) {
                chatHistory = FXCollections.observableArrayList();
            } else {
                List<ChatMessage> chatMessagesList = implementation.loadMessagesForEntry(bibDatabaseContext.get().getDatabasePath().get(), entry.getCitationKey().get());
                chatHistory = FXCollections.observableArrayList(chatMessagesList);
            }

            return new ChatHistoryManagementRecord<>(entry, bibDatabaseContext, chatHistory);
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
    public void closeChatHistoryForEntry(String entryId) {
        ChatHistoryManagementRecord<BibEntry> chatHistoryManagementRecord = bibEntriesChatHistory.get(entryId);
        if (chatHistoryManagementRecord == null) {
            return;
        }

        BibEntry entry = chatHistoryManagementRecord.object();
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
        bibEntriesChatHistory.remove(entry.getId());
    }

    public ObservableList<ChatMessage> getChatHistoryForGroup(AbstractGroup group) {
        return groupsChatHistory.computeIfAbsent(group, groupArg -> {
            Optional<BibDatabaseContext> bibDatabaseContext = findBibDatabaseForGroup(group);

            ObservableList<ChatMessage> chatHistory;

            if (bibDatabaseContext.isEmpty() || bibDatabaseContext.get().getDatabasePath().isEmpty()) {
                chatHistory = FXCollections.observableArrayList();
            } else {
                List<ChatMessage> chatMessagesList = implementation.loadMessagesForGroup(
                        bibDatabaseContext.get().getDatabasePath().get(),
                        group.nameProperty().get()
                );

                chatHistory = FXCollections.observableArrayList(chatMessagesList);
            }

            return new ChatHistoryManagementRecord<>(group, bibDatabaseContext, chatHistory);
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
        ChatHistoryManagementRecord<AbstractGroup> chatHistoryManagementRecord = groupsChatHistory.get(group);
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
        if (!CitationKeyCheck.citationKeyIsPresentAndUnique(bibDatabaseContext, bibEntry)) {
            tryToGenerateCitationKey(bibDatabaseContext, bibEntry);
        }

        return CitationKeyCheck.citationKeyIsPresentAndUnique(bibDatabaseContext, bibEntry);
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

    private void transferGroupHistory(BibDatabaseContext bibDatabaseContext, String oldName, String newName) {
        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.warn("Could not transfer chat history of group {} (old name: {}): database path is empty.", newName, oldName);
            return;
        }

        List<ChatMessage> chatMessages = implementation.loadMessagesForGroup(bibDatabaseContext.getDatabasePath().get(), oldName);
        implementation.storeMessagesForGroup(bibDatabaseContext.getDatabasePath().get(), newName, chatMessages);
    }

    private void transferEntryHistory(BibDatabaseContext bibDatabaseContext, String oldCitationKey, String newCitationKey) {
        // TODO: This method does not check if the citation key is valid.

        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.warn("Could not transfer chat history of entry {} (old key: {}): database path is empty.", newCitationKey, oldCitationKey);
            return;
        }

        List<ChatMessage> chatMessages = implementation.loadMessagesForEntry(bibDatabaseContext.getDatabasePath().get(), oldCitationKey);
        implementation.storeMessagesForEntry(bibDatabaseContext.getDatabasePath().get(), newCitationKey, chatMessages);
    }
}
