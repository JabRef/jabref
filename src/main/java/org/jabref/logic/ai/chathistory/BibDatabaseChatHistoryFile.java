package org.jabref.logic.ai.chathistory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import org.jabref.gui.DialogService;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
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
public class BibDatabaseChatHistoryFile implements AutoCloseable {
    public static final String AI_CHATS_FILE_EXTENSION = "aichats";

    private static final Logger LOGGER = LoggerFactory.getLogger(BibDatabaseChatHistoryFile.class);

    private final MVStore mvStore;

    private final Map<Integer, String> messageCitationKey;
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

        this.messageCitationKey = mvStore.openMap("messageEntry");
        this.messageType = mvStore.openMap("messageType");
        this.messageContent = mvStore.openMap("messageContent");
    }

    public BibEntryChatHistory getChatHistoryForEntry(String citationKey) {
        return new BibEntryChatHistory(this, citationKey);
    }

    public List<ChatMessage> getMessagesForEntry(String citationKey) {
        return messageCitationKey
                .entrySet()
                .stream()
                .filter(integerStringEntry -> integerStringEntry.getValue().equals(citationKey))
                .map(Map.Entry::getKey)
                .sorted() // Assuming old message key is less than new message key.
                .map(this::retrieveChatMessage)
                .toList();
    }

    private ChatMessage retrieveChatMessage(int id) {
        String type = messageType.get(id);
        String content = messageContent.get(id);

        if (type.equals(ChatMessageType.AI.name())) {
            return new AiMessage(content);
        } else if (type.equals(ChatMessageType.USER.name())) {
            return new UserMessage(content);
        } else {
            LOGGER.warn("BibDatabaseChatHistoryFile supports only AI and user messages, but retrieved message has other type: " + type + ". Will treat as an AI message");
            return new AiMessage(content);
        }
    }

    public synchronized void addMessage(String citationKey, ChatMessage message) {
        int id = messageType.keySet().size() + 1;

        messageType.put(id, message.type().name());
        messageCitationKey.put(id, citationKey);

        if (message instanceof AiMessage aiMessage) {
            messageContent.put(id, aiMessage.text());
        } else if (message instanceof UserMessage userMessage) {
            messageContent.put(id, userMessage.singleText());
        } else {
            LOGGER.warn("BibDatabaseChatHistoryFile supports only AI and user messages, but added message has other type: " + message.type().name());
        }
    }

    public void clearMessagesForEntry(String citationKey) {
        messageCitationKey
                .entrySet()
                .stream()
                .filter(integerStringEntry -> integerStringEntry.getValue().equals(citationKey))
                .map(Map.Entry::getKey)
                .forEach(id -> {
                    messageType.remove(id);
                    messageContent.remove(id);
                    messageCitationKey.remove(id);
                });
    }

    public void close() {
        this.mvStore.close();
    }
}
