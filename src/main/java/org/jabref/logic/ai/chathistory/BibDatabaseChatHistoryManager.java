package org.jabref.logic.ai.chathistory;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.model.search.rules.SearchRule;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.Nullable;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class stores the chat history with AI for .bib files. The chat history file is stored in a user local folder.
 * <p>
 * It uses MVStore for serializing the messages. In case any error occurs while opening the MVStore,
 * the class will notify the user of this error and continue with in-memory store (meaning all messages will
 * be thrown away on exit).
 *
 * @implNote If something is changed in the data model, increase {@link org.jabref.logic.ai.AiService#VERSION}
 */
public class BibDatabaseChatHistoryManager implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibDatabaseChatHistoryManager.class);
    private static final String CHAT_HISTORY_FILE_NAME = "chat-history.mv";

    private final MVStore mvStore;

    private record ChatHistoryRecord(String library, String citationKey, String type, String content) implements Serializable {
        public ChatMessage toLangchainMessage() {
            if (type.equals(ChatMessageType.AI.name())) {
                return new AiMessage(content);
            } else if (type.equals(ChatMessageType.USER.name())) {
                return new UserMessage(content);
            } else {
                LOGGER.warn("BibDatabaseChatHistoryManager supports only AI and user messages, but retrieved message has other type: {}. Will treat as an AI message", type);
                return new AiMessage(content);
            }
        }
    }

    private final Map<Integer, ChatHistoryRecord> messages;

    private final Map<Path, BibDatabaseChatHistory> bibDatabaseChatHistoryMap = new HashMap<>();

    public BibDatabaseChatHistoryManager(DialogService dialogService) {
        @Nullable Path ingestedFilesTrackerPath = JabRefDesktop.getAiFilesDirectory().resolve(CHAT_HISTORY_FILE_NAME);

        try {
            Files.createDirectories(JabRefDesktop.getAiFilesDirectory());
        } catch (IOException e) {
            LOGGER.error("An error occurred while creating directories for storing chat history. Chat history won't be remembered in next session", e);
            dialogService.notify("An error occurred while creating directories for storing chat history. Chat history won't be remembered in next session");
            ingestedFilesTrackerPath = null;
        }

        MVStore mvStore;

        try {
            mvStore = MVStore.open(ingestedFilesTrackerPath == null ? null : ingestedFilesTrackerPath.toString());
        } catch (Exception e) {
            LOGGER.error("An error occurred while creating directories for storing chat history. Chat history won't be remembered in next session", e);
            dialogService.notify("An error occurred while creating directories for storing chat history. Chat history won't be remembered in next session");
            mvStore = MVStore.open(null);
        }

        this.mvStore = mvStore;
        this.messages = mvStore.openMap("messages");
    }

    public BibDatabaseChatHistory getChatHistoryForBibDatabase(Path bibDatabasePath) {
        return bibDatabaseChatHistoryMap.computeIfAbsent(
                bibDatabasePath,
                path -> new BibDatabaseChatHistory(bibDatabasePath, this)
        );
    }

    public List<ChatMessage> getMessagesForEntry(Path bibDatabasePath, String citationKey) {
        return filterMessagesByLibraryAndEntry(bibDatabasePath, citationKey)
                .sorted() // Assuming old message key is less than new message key.
                .map(id -> messages.get(id).toLangchainMessage())
                .toList();
    }

    public synchronized void addMessage(Path bibDatabasePath, String citationKey, ChatMessage message) {
        int id = messages.keySet().size() + 1;

        String content;

        if (message instanceof AiMessage aiMessage) {
            content = aiMessage.text();
        } else if (message instanceof UserMessage userMessage) {
            content = userMessage.singleText();
        } else {
            LOGGER.warn("BibDatabaseChatHistoryFile supports only AI and user messages, but added message has other type: {}", message.type().name());
            return;
        }

        messages.put(id, new ChatHistoryRecord(bibDatabasePath.toString(), citationKey, message.type().name(), content));
    }

    public void clearMessagesForEntry(Path bibDatabasePath, String citationKey) {
        filterMessagesByLibraryAndEntry(bibDatabasePath, citationKey).forEach(messages::remove);
    }

    private Stream<Integer> filterMessagesByLibraryAndEntry(Path bibDatabasePath, String citationKey) {
        return messages
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().library.equals(bibDatabasePath.toString()) && entry.getValue().citationKey.equals(citationKey))
                .map(Map.Entry::getKey);
    }

    @Override
    public void close() {
        bibDatabaseChatHistoryMap.clear();
        mvStore.close();
    }
}
