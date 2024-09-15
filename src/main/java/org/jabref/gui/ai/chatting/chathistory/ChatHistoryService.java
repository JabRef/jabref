package org.jabref.gui.ai.chatting.chathistory;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.jabref.gui.StateManager;
import org.jabref.logic.ai.chatting.chathistory.ChatHistoryStorage;
import org.jabref.logic.ai.chatting.chathistory.storages.MVStoreChatHistoryStorage;
import org.jabref.logic.ai.util.CitationKeyCheck;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.GroupTreeNode;

import com.airhacks.afterburner.injection.Injector;
import com.google.common.eventbus.Subscribe;
import dev.langchain4j.data.message.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for getting and storing chat history for entries and groups.
 * Use this class <s>in logic and</s> UI.
 * It currently resides in the UI package because it relies on the {@link StateManager} to get the open databases and to find the correct {@link BibDatabaseContext} based on an entry.
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
 * Constraints for serialization and deserialization of a chat history of an {@link GroupTreeNode}:
 * 1. There should exist an associated {@link BibDatabaseContext} for the {@link GroupTreeNode}.
 * 2. The database path of the associated {@link BibDatabaseContext} must be set.
 * 3. The name of an {@link GroupTreeNode} must be set and unique (this requirement is possibly already satisfied in
 *    JabRef, but for {@link BibEntry} it is definitely not).
 */
public class ChatHistoryService implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatHistoryService.class);

    private static final String CHAT_HISTORY_FILE_NAME = "chat-histories.mv";

    private final StateManager stateManager = Injector.instantiateModelOrService(StateManager.class);

    private final CitationKeyPatternPreferences citationKeyPatternPreferences;

    private final ChatHistoryStorage implementation;

    private record ChatHistoryManagementRecord(Optional<BibDatabaseContext> bibDatabaseContext, ObservableList<ChatMessage> chatHistory) { }

    // We use a {@link TreeMap} here to store {@link BibEntry} chat histories by their id.
    // When you compare {@link BibEntry} instances, they are compared by value, not by reference.
    // And when you store {@link BibEntry} instances in a {@link HashMap}, an old hash may be stored when the {@link BibEntry} is changed.
    // See also ADR-38.
    private final TreeMap<BibEntry, ChatHistoryManagementRecord> bibEntriesChatHistory = new TreeMap<>(Comparator.comparing(BibEntry::getId));

    // We use {@link TreeMap} for group chat history for the same reason as for {@link BibEntry}ies.
    private final TreeMap<GroupTreeNode, ChatHistoryManagementRecord> groupsChatHistory = new TreeMap<>((o1, o2) -> {
        // The most important thing is to catch equality/non-equality.
        // For "less" or "bigger" comparison, we will fall back to group names.
        return o1 == o2 ? 0 : o1.getGroup().getName().compareTo(o2.getGroup().getName());
    });

    public ChatHistoryService(CitationKeyPatternPreferences citationKeyPatternPreferences, NotificationService notificationService) {
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
        this.implementation = new MVStoreChatHistoryStorage(Directories.getAiFilesDirectory().resolve(CHAT_HISTORY_FILE_NAME), notificationService);
        configureHistoryTransfer();
    }

    public ChatHistoryService(CitationKeyPatternPreferences citationKeyPatternPreferences,
                              ChatHistoryStorage implementation) {
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
        this.implementation = implementation;

        configureHistoryTransfer();
    }

    private void configureHistoryTransfer() {
        stateManager.getOpenDatabases().addListener((ListChangeListener<BibDatabaseContext>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(this::configureHistoryTransfer);
                }
            }
        });
    }

    private void configureHistoryTransfer(BibDatabaseContext bibDatabaseContext) {
        bibDatabaseContext.getMetaData().getGroups().ifPresent(rootGroupTreeNode -> {
            rootGroupTreeNode.iterateOverTree().forEach(groupNode -> {
                groupNode.getGroup().nameProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue != null && oldValue != null) {
                        transferGroupHistory(bibDatabaseContext, groupNode, oldValue, newValue);
                    }
                });

                groupNode.getGroupProperty().addListener((obs, oldValue, newValue) -> {
                    if (oldValue != null && newValue != null) {
                        transferGroupHistory(bibDatabaseContext, groupNode, oldValue.getName(), newValue.getName());
                    }
                });
            });
        });

        bibDatabaseContext.getDatabase().getEntries().forEach(entry -> {
            entry.registerListener(new CitationKeyChangeListener(bibDatabaseContext));
        });
    }

    public ObservableList<ChatMessage> getChatHistoryForEntry(BibEntry entry) {
        return bibEntriesChatHistory.computeIfAbsent(entry, entryArg -> {
            Optional<BibDatabaseContext> bibDatabaseContext = findBibDatabaseForEntry(entry);

            ObservableList<ChatMessage> chatHistory;

            if (bibDatabaseContext.isEmpty() || entry.getCitationKey().isEmpty() || !correctCitationKey(bibDatabaseContext.get(), entry) || bibDatabaseContext.get().getDatabasePath().isEmpty()) {
                chatHistory = FXCollections.observableArrayList();
            } else {
                List<ChatMessage> chatMessagesList = implementation.loadMessagesForEntry(bibDatabaseContext.get().getDatabasePath().get(), entry.getCitationKey().get());
                chatHistory = FXCollections.observableArrayList(chatMessagesList);
            }

            return new ChatHistoryManagementRecord(bibDatabaseContext, chatHistory);
        }).chatHistory;
    }

    /**
     * Removes the chat history for the given {@link BibEntry} from the internal RAM map.
     * If the {@link BibEntry} satisfies requirements for serialization and deserialization of chat history (see
     * the docstring for the {@link ChatHistoryService}), then the chat history will be stored via the
     * {@link ChatHistoryStorage}.
     * <p>
     * It is not necessary to call this method (everything will be stored in {@link ChatHistoryService#close()},
     * but it's best to call it when the chat history {@link BibEntry} is no longer needed.
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

    public ObservableList<ChatMessage> getChatHistoryForGroup(GroupTreeNode group) {
        return groupsChatHistory.computeIfAbsent(group, groupArg -> {
            Optional<BibDatabaseContext> bibDatabaseContext = findBibDatabaseForGroup(group);

            ObservableList<ChatMessage> chatHistory;

            if (bibDatabaseContext.isEmpty() || bibDatabaseContext.get().getDatabasePath().isEmpty()) {
                chatHistory = FXCollections.observableArrayList();
            } else {
                List<ChatMessage> chatMessagesList = implementation.loadMessagesForGroup(
                        bibDatabaseContext.get().getDatabasePath().get(),
                        group.getGroup().getName()
                );

                chatHistory = FXCollections.observableArrayList(chatMessagesList);
            }

            return new ChatHistoryManagementRecord(bibDatabaseContext, chatHistory);
        }).chatHistory;
    }

    /**
     * Removes the chat history for the given {@link GroupTreeNode} from the internal RAM map.
     * If the {@link GroupTreeNode} satisfies requirements for serialization and deserialization of chat history (see
     * the docstring for the {@link ChatHistoryService}), then the chat history will be stored via the
     * {@link ChatHistoryStorage}.
     * <p>
     * It is not necessary to call this method (everything will be stored in {@link ChatHistoryService#close()},
     * but it's best to call it when the chat history {@link GroupTreeNode} is no longer needed.
     */
    public void closeChatHistoryForGroup(GroupTreeNode group) {
        ChatHistoryManagementRecord chatHistoryManagementRecord = groupsChatHistory.get(group);
        if (chatHistoryManagementRecord == null) {
            return;
        }

        Optional<BibDatabaseContext> bibDatabaseContext = chatHistoryManagementRecord.bibDatabaseContext();

        if (bibDatabaseContext.isPresent() && bibDatabaseContext.get().getDatabasePath().isPresent()) {
            implementation.storeMessagesForGroup(
                    bibDatabaseContext.get().getDatabasePath().get(),
                    group.getGroup().getName(),
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

    private Optional<BibDatabaseContext> findBibDatabaseForGroup(GroupTreeNode group) {
        return stateManager
                .getOpenDatabases()
                .stream()
                .filter(dbContext ->
                        dbContext.getMetaData().groupsBinding().get().map(groupTreeNode ->
                                groupTreeNode.containsGroup(group.getGroup())
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

    private void transferGroupHistory(BibDatabaseContext bibDatabaseContext, GroupTreeNode groupTreeNode, String oldName, String newName) {
        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.warn("Could not transfer chat history of group {} (old name: {}): database path is empty.", newName, oldName);
            return;
        }

        List<ChatMessage> chatMessages = groupsChatHistory.computeIfAbsent(groupTreeNode,
                e -> new ChatHistoryManagementRecord(Optional.of(bibDatabaseContext), FXCollections.observableArrayList())).chatHistory;
        implementation.storeMessagesForGroup(bibDatabaseContext.getDatabasePath().get(), oldName, List.of());
        implementation.storeMessagesForGroup(bibDatabaseContext.getDatabasePath().get(), newName, chatMessages);
    }

    private void transferEntryHistory(BibDatabaseContext bibDatabaseContext, BibEntry entry, String oldCitationKey, String newCitationKey) {
        // TODO: This method does not check if the citation key is valid.

        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.warn("Could not transfer chat history of entry {} (old key: {}): database path is empty.", newCitationKey, oldCitationKey);
            return;
        }

        List<ChatMessage> chatMessages = bibEntriesChatHistory.computeIfAbsent(entry,
                e -> new ChatHistoryManagementRecord(Optional.of(bibDatabaseContext), FXCollections.observableArrayList())).chatHistory;
        implementation.storeMessagesForGroup(bibDatabaseContext.getDatabasePath().get(), oldCitationKey, List.of());
        implementation.storeMessagesForEntry(bibDatabaseContext.getDatabasePath().get(), newCitationKey, chatMessages);
    }

    private class CitationKeyChangeListener {
        private final BibDatabaseContext bibDatabaseContext;

        public CitationKeyChangeListener(BibDatabaseContext bibDatabaseContext) {
            this.bibDatabaseContext = bibDatabaseContext;
        }

        @Subscribe
        void listen(FieldChangedEvent e) {
            if (e.getField() != InternalField.KEY_FIELD) {
                return;
            }

            transferEntryHistory(bibDatabaseContext, e.getBibEntry(), e.getOldValue(), e.getNewValue());
        }
    }
}
