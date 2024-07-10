package org.jabref.logic.ai.chathistory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.jabref.gui.DialogService;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class stores the chat history with AI. The chat history file is stored alongside the BibTeX file.
 * <p>
 * It uses MVStore for serializing the messages. In case any error occurs while opening the MVStore,
 * the class will notify the user of this error and continue with in-memory store (meaning all messages will
 * be thrown away on exit).
 *
 * @implNote If something is changed in the data model, increase {@link org.jabref.logic.ai.AiService#VERSION}
 */
public class BibDatabaseChatHistory implements AutoCloseable {
    public static final String AI_CHATS_FILE_EXTENSION = "aichats";

    private static final Logger LOGGER = LoggerFactory.getLogger(BibDatabaseChatHistory.class);

    private final MVStore mvStore;

    // Map from citation key to list of messages
    // "ArrayList" is used, because it implements Serializable
    private final MVMap<String, ArrayList<ChatMessage>> messages;

    public BibDatabaseChatHistory(Path bibDatabasePath, DialogService dialogService) {
        MVStore mvStore;

        try {
            mvStore = MVStore.open(bibDatabasePath + "." + AI_CHATS_FILE_EXTENSION);
        } catch (Exception e) {
            dialogService.showErrorDialogAndWait("Unable to open chat history store for the library. Will use an in-memory store", e);
            mvStore = MVStore.open(null);
        }

        this.mvStore = mvStore;

        this.messages = this.mvStore.openMap("messages");
    }

    public BibEntryChatHistory getChatHistoryForEntry(String citationKey) {
        return new BibEntryChatHistory(this, citationKey);
    }

    public Stream<ChatMessage> getAllMessagesForEntry(String citationKey) {
        if (!messages.containsKey(citationKey)) {
            return Stream.empty();
        }
        return messages.get(citationKey).stream();
    }

    public void addMessage(String citationKey, ChatMessage message) {
        messages.computeIfAbsent(citationKey, k -> new ArrayList<>()).add(message);
    }

    public void clearMessagesForEntry(String citationKey) {
        messages.remove(citationKey);
    }

    public void close() {
        LOGGER.trace("Closing chat history store");
        this.mvStore.close();
        LOGGER.trace("Closed chat history store");
    }
}
