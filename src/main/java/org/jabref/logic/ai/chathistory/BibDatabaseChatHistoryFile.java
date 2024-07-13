package org.jabref.logic.ai.chathistory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jabref.gui.DialogService;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

/**
 * This class stores the chat history with AI. The chat history file is stored alongside the BibTeX file.
 * <p>
 * It uses MVStore for serializing the messages. In case any error occurs while opening the MVStore,
 * the class will notify the user of this error and continue with in-memory store (meaning all messages will
 * be thrown away on exit).
 *
 * @implNote If something is changed in the data model, increase {@link org.jabref.logic.ai.AiService#VERSION}
 */
public class BibDatabaseChatHistoryFile implements AutoCloseable {
    public static final String AI_CHATS_FILE_EXTENSION = "aichats";

    private final MVStore mvStore;

    private final Map<Integer, String> messageEntry;
    private final Map<Integer, String> messageType;
    private final Map<Integer, String> messageContent;

    public BibDatabaseChatHistoryFile(Path bibDatabasePath, DialogService dialogService) {
        MVStore mvStore;

        try {
            mvStore = MVStore.open(bibDatabasePath + "." + AI_CHATS_FILE_EXTENSION);
        } catch (Exception e) {
            dialogService.showErrorDialogAndWait("Unable to open chat history store for the library. Will use an in-memory store", e);
            mvStore = MVStore.open(null);
        }

        this.mvStore = mvStore;

        this.messageEntry = mvStore.openMap("messageEntry");
        this.messageType = mvStore.openMap("messageType");
        this.messageContent = mvStore.openMap("messageContent");
    }

    public BibEntryChatHistory getChatHistoryForEntry(String citationKey) {
        return new BibEntryChatHistory(this, citationKey);
    }

    public List<ChatMessage> getMessagesForEntry(String citationKey) {
        if (!messages.containsKey(citationKey)) {
            return List.of();
        }

        return messages.get(citationKey);
    }

    public void addMessage(String citationKey, ChatMessage message) {
        messages.computeIfAbsent(citationKey, k -> new ArrayList<>()).add(message);
    }

    public void clearMessagesForEntry(String citationKey) {
        messages.remove(citationKey);
    }

    public void close() {
        this.mvStore.close();
    }
}
