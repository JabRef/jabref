package org.jabref.logic.ai.chatting;

import java.util.Comparator;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.Map;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.ai.chatting.chathistory.ChatHistoryStorage;
import org.jabref.logic.ai.util.CitationKeyCheck;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.groups.GroupTreeNode;

import com.google.common.eventbus.Subscribe;
import dev.langchain4j.data.message.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Main class for getting and storing chat history for entries and groups.
/// Use this class <s>in logic and</s> UI.
/// It currently resides in the UI package because it relies on the `org.jabref.gui.StateManager` to get the open databases and to find the correct [BibDatabaseContext] based on an entry.
///
/// The returned chat history is a [ObservableList]. So chat history exists for every possible
/// [BibEntry] and [org.jabref.model.groups.AbstractGroup]. The chat history is stored in runtime.
///
/// To save and load chat history, [BibEntry] and [org.jabref.model.groups.AbstractGroup] must satisfy several constraints.
/// Serialization and deserialization is handled in [ChatHistoryStorage].
///
/// Constraints for serialization and deserialization of a chat history of a [BibEntry]:
/// 1. There should exist an associated [BibDatabaseContext] for the [BibEntry].
/// 2. The database path of the associated [BibDatabaseContext] must be set.
/// 3. The citation key of the [BibEntry] must be set and unique.
///
/// Constraints for serialization and deserialization of a chat history of an [GroupTreeNode]:
/// 1. There should exist an associated [BibDatabaseContext] for the [GroupTreeNode].
/// 2. The database path of the associated [BibDatabaseContext] must be set.
/// 3. The name of an [GroupTreeNode] must be set and unique (this requirement is possibly already satisfied in
///    JabRef, but for [BibEntry] it is definitely not).
public class ChatHistoryService implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatHistoryService.class);

    private final CitationKeyPatternPreferences citationKeyPatternPreferences;
    private final ChatHistoryStorage implementation;

    // Note about `Optional<BibDatabaseContext>`: it was necessary in previous version, but currently we never save an `Optional.empty()`.
    // However, we decided to left it here: to reduce migrations and to make possible to chat with a {@link BibEntry} without {@link BibDatabaseContext}
    // ({@link BibDatabaseContext} is required only for load/store of the chat).
    private record ChatHistoryManagementRecord(Optional<BibDatabaseContext> bibDatabaseContext, ObservableList<ChatMessage> chatHistory) {
    }

    // We use a {@link TreeMap} here to store {@link BibEntry} chat histories by their id.
    // When you compare {@link BibEntry} instances, they are compared by value, not by reference.
    // And when you store {@link BibEntry} instances in a {@link HashMap}, an old hash may be stored when the {@link BibEntry} is changed.
    // See also ADR-38.
    private final TreeMap<BibEntry, ChatHistoryManagementRecord> bibEntriesChatHistory = new TreeMap<>(Comparator.comparing(BibEntry::getId));

    private record GroupKey(String libraryId, String groupName) implements Comparable<GroupKey> {
        @Override
        public int compareTo(GroupKey other) {
            int libraryCompare = libraryId.compareTo(other.libraryId);
            if (libraryCompare != 0) {
                return libraryCompare;
            }
            return groupName.compareTo(other.groupName);
        }
    }
    
    // We use {@link TreeMap} for group chat history for the same reason as for {@link BibEntry}ies.
    private final TreeMap<GroupKey, ChatHistoryManagementRecord> groupsChatHistory = new TreeMap<>();

    public ChatHistoryService(CitationKeyPatternPreferences citationKeyPatternPreferences, ChatHistoryStorage implementation) {
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
        this.implementation = implementation;
    }

    public void setupDatabase(BibDatabaseContext bibDatabaseContext) {
        bibDatabaseContext.getMetaData().getGroups().ifPresent(rootGroupTreeNode ->
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
                }));

        bibDatabaseContext.getDatabase().getEntries().forEach(entry -> entry.registerListener(new CitationKeyChangeListener(bibDatabaseContext)));
    }

    public ObservableList<ChatMessage> getChatHistoryForEntry(BibDatabaseContext bibDatabaseContext, BibEntry entry) {
        return bibEntriesChatHistory.computeIfAbsent(entry, entryArg -> {
            ObservableList<ChatMessage> chatHistory;

            if (entry.getCitationKey().isEmpty() || !correctCitationKey(bibDatabaseContext, entry) || bibDatabaseContext.getDatabasePath().isEmpty()) {
                chatHistory = FXCollections.observableArrayList();
            } else {
                List<ChatMessage> chatMessagesList = implementation.loadMessagesForEntry(bibDatabaseContext.getDatabasePath().get(), entry.getCitationKey().get());
                chatHistory = FXCollections.observableArrayList(chatMessagesList);
            }

            return new ChatHistoryManagementRecord(Optional.of(bibDatabaseContext), chatHistory);
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

    public ObservableList<ChatMessage> getChatHistoryForGroup(BibDatabaseContext bibDatabaseContext, GroupTreeNode group) {
        String libraryId = bibDatabaseContext.getUid().toString();
        String groupName = group.getGroup().getName();
        GroupKey key = new GroupKey(libraryId, groupName);
    
        return groupsChatHistory.computeIfAbsent(key, k -> {
            ObservableList<ChatMessage> chatHistory;

            if (bibDatabaseContext.getDatabasePath().isEmpty()) {
                chatHistory = FXCollections.observableArrayList();
            } else {
                List<ChatMessage> chatMessagesList = implementation.loadMessagesForGroup(
                        bibDatabaseContext.getDatabasePath().get(),
                        groupName
                );

                chatHistory = FXCollections.observableArrayList(chatMessagesList);
            }

            return new ChatHistoryManagementRecord(Optional.of(bibDatabaseContext), chatHistory);
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
        String groupName = group.getGroup().getName();
    
        // Finding all entries for this group
        List<GroupKey> keysToRemove = new ArrayList<>();
    
        for (Map.Entry<GroupKey, ChatHistoryManagementRecord> entry : groupsChatHistory.entrySet()) {
            GroupKey key = entry.getKey();
            ChatHistoryManagementRecord record = entry.getValue();
        
            if (key.groupName().equals(groupName)) {
                Optional<BibDatabaseContext> bibDatabaseContext = record.bibDatabaseContext();
            
                if (bibDatabaseContext.isPresent() && bibDatabaseContext.get().getDatabasePath().isPresent()) {
                    implementation.storeMessagesForGroup(
                            bibDatabaseContext.get().getDatabasePath().get(),
                            groupName,
                            record.chatHistory()
                    );
                }
            
                keysToRemove.add(key);
            }
        }
    
        // Removing all matching entries
        for (GroupKey key : keysToRemove) {
            groupsChatHistory.remove(key);
        }
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

    @Override
    public void close() {
        new HashSet<>(bibEntriesChatHistory.keySet()).forEach(this::closeChatHistoryForEntry);

        // Saving all group chat histories
        List<GroupKey> groupKeys = new ArrayList<>(groupsChatHistory.keySet());
        for (GroupKey key : groupKeys) {
            ChatHistoryManagementRecord record = groupsChatHistory.get(key);
            if (record != null && record.bibDatabaseContext().isPresent() && record.bibDatabaseContext().get().getDatabasePath().isPresent()) {
                implementation.storeMessagesForGroup(
                    record.bibDatabaseContext().get().getDatabasePath().get(),
                    key.groupName(),
                    record.chatHistory()
                );
            }
            groupsChatHistory.remove(key);
        }

        implementation.commit();
        implementation.close();
    }

    private void transferGroupHistory(BibDatabaseContext bibDatabaseContext, GroupTreeNode groupTreeNode, String oldName, String newName) {
        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.warn("Could not transfer chat history of group {} (old name: {}): database path is empty.", newName, oldName);
            return;
        }

        String libraryId = bibDatabaseContext.getUid().toString();
        GroupKey oldKey = new GroupKey(libraryId, oldName);
        GroupKey newKey = new GroupKey(libraryId, newName);

        List<ChatMessage> chatMessages = groupsChatHistory.computeIfAbsent(newKey,
                k -> new ChatHistoryManagementRecord(Optional.of(bibDatabaseContext), FXCollections.observableArrayList())).chatHistory;
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
