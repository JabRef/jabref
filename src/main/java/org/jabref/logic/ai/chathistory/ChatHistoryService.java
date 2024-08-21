package org.jabref.logic.ai.chathistory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.StateManager;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.airhacks.afterburner.injection.Injector;
import dev.langchain4j.data.message.ChatMessage;

public class ChatHistoryService implements AutoCloseable {
    private final StateManager stateManager = Injector.instantiateModelOrService(StateManager.class);

    private final CitationKeyPatternPreferences citationKeyPatternPreferences;

    private final ChatHistoryImplementation implementation;

    private record ChatHistoryManagementRecord(Optional<BibDatabaseContext> bibDatabaseContext, ObservableList<ChatMessage> chatHistory) { }

    private final Map<BibEntry, ChatHistoryManagementRecord> bibEntriesChatHistory = new HashMap<>();

    public ChatHistoryService(CitationKeyPatternPreferences citationKeyPatternPreferences,
                              ChatHistoryImplementation implementation) {
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
        this.implementation = implementation;
    }

    public ObservableList<ChatMessage> getChatHistoryForEntry(BibEntry entry) {
        return bibEntriesChatHistory.computeIfAbsent(entry, entryArg -> {
            Optional<BibDatabaseContext> bibDatabaseContext = findBibDatabase(entry);

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

    // Do not forget to call this method.
    public void closeChatHistoryForEntry(BibEntry entry) {
        ChatHistoryManagementRecord chatHistoryManagementRecord = bibEntriesChatHistory.get(entry);
        if (chatHistoryManagementRecord == null) {
            return;
        }

        Optional<BibDatabaseContext> bibDatabaseContext = chatHistoryManagementRecord.bibDatabaseContext();

        if (bibDatabaseContext.isPresent() && entry.getCitationKey().isPresent() && correctCitationKey(bibDatabaseContext.get(), entry) && bibDatabaseContext.get().getDatabasePath().isPresent()) {
            implementation.storeMessagesForEntry(
                    bibDatabaseContext.get().getDatabasePath().get(),
                    entry.getCitationKey().get(),
                    chatHistoryManagementRecord.chatHistory()
            );
        }

        // TODO: What if there is two AI chats for the same entry? And one is closed and one is not?
        bibEntriesChatHistory.remove(entry);
    }

    private boolean correctCitationKey(BibDatabaseContext bibDatabaseContext, BibEntry bibEntry) {
        if (!citationKeyIsValid(bibDatabaseContext, bibEntry)) {
            tryToGenerateCitationKey(bibDatabaseContext, bibEntry);
        }

        return citationKeyIsValid(bibDatabaseContext, bibEntry);
    }

    private void tryToGenerateCitationKey(BibDatabaseContext bibDatabaseContext, BibEntry bibEntry) {
        new CitationKeyGenerator(bibDatabaseContext, citationKeyPatternPreferences).generateAndSetKey(bibEntry);
    }

    public boolean citationKeyIsValid(BibEntry bibEntry) {
        Optional<BibDatabaseContext> bibDatabaseContext = findBibDatabase(bibEntry);
        return bibDatabaseContext.filter(databaseContext -> citationKeyIsValid(databaseContext, bibEntry)).isPresent();
    }

    private static boolean citationKeyIsValid(BibDatabaseContext bibDatabaseContext, BibEntry bibEntry) {
        return !hasEmptyCitationKey(bibEntry) && bibEntry.getCitationKey().map(key -> citationKeyIsUnique(bibDatabaseContext, key)).orElse(false);
    }

    private static boolean hasEmptyCitationKey(BibEntry bibEntry) {
        return bibEntry.getCitationKey().map(String::isEmpty).orElse(true);
    }

    private static boolean citationKeyIsUnique(BibDatabaseContext bibDatabaseContext, String citationKey) {
        return bibDatabaseContext.getDatabase().getNumberOfCitationKeyOccurrences(citationKey) == 1;
    }

    private Optional<BibDatabaseContext> findBibDatabase(BibEntry entry) {
        return stateManager
                .getOpenDatabases()
                .stream()
                .filter(dbContext -> dbContext.getDatabase().getEntries().contains(entry))
                .findFirst();
    }

    @Override
    public void close() {
        // We need to clone `bibEntriesChatHistory.keySet()` because closeChatHistoryForEntry() modifies the `bibEntriesChatHistory` map.
        new HashSet<>(bibEntriesChatHistory.keySet()).forEach(this::closeChatHistoryForEntry);
    }
}
