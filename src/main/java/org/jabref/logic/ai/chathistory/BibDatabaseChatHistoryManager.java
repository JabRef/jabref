package org.jabref.logic.ai.chathistory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;

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
    private static final String CHAT_HISTORY_FILE_NAME = "chatHistory.mv";

    private final MVStore mvStore;

    private final Map<Integer, String> messageLibrary;
    private final Map<Integer, String> messageCitationKey;
    private final Map<Integer, String> messageType;
    private final Map<Integer, String> messageContent;

    private final Map<Path, BibDatabaseChatHistory> bibDatabaseChatHistoryMap = new HashMap<>();

    public BibDatabaseChatHistoryManager(DialogService dialogService) {
        @Nullable Path ingestedFilesTrackerPath = JabRefDesktop.getAiFilesDirectory().resolve(CHAT_HISTORY_FILE_NAME);

        try {
            Files.createDirectories(JabRefDesktop.getAiFilesDirectory());
        } catch (IOException e) {
            dialogService.showErrorDialogAndWait("An error occurred while creating directories for storing chat history. Will store history in RAM", e);
            ingestedFilesTrackerPath = null;
        }

        MVStore mvStore;

        try {
            mvStore = MVStore.open(ingestedFilesTrackerPath == null ? null : ingestedFilesTrackerPath.toString());
        } catch (Exception e) {
            dialogService.showErrorDialogAndWait("An error occurred while creating file for storing chat history. Will store history in RAM", e);
            mvStore = MVStore.open(null);
        }

        this.mvStore = mvStore;
        this.messageLibrary = mvStore.openMap("messageLibrary");
        this.messageCitationKey = mvStore.openMap("messageCitationKey");
        this.messageType = mvStore.openMap("messageType");
        this.messageContent = mvStore.openMap("messageContent");
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
            LOGGER.warn("BibDatabaseChatHistoryManager supports only AI and user messages, but retrieved message has other type: " + type + ". Will treat as an AI message");
            return new AiMessage(content);
        }
    }

    public synchronized void addMessage(Path bibDatabasePath, String citationKey, ChatMessage message) {
        int id = messageType.keySet().size() + 1;

        messageLibrary.put(id, bibDatabasePath.toString());
        messageCitationKey.put(id, citationKey);
        messageType.put(id, message.type().name());

        if (message instanceof AiMessage aiMessage) {
            messageContent.put(id, aiMessage.text());
        } else if (message instanceof UserMessage userMessage) {
            messageContent.put(id, userMessage.singleText());
        } else {
            LOGGER.warn("BibDatabaseChatHistoryFile supports only AI and user messages, but added message has other type: " + message.type().name());
        }
    }

    public void clearMessagesForEntry(Path bibDatabasePath, String citationKey) {
        filterMessagesByLibraryAndEntry(bibDatabasePath, citationKey)
                .forEach(id -> {
                    messageType.remove(id);
                    messageContent.remove(id);
                    messageCitationKey.remove(id);
                });
    }

    private Stream<Integer> filterMessagesByLibraryAndEntry(Path bibDatabasePath, String citationKey) {
        return filterMessagesByLibrary(bibDatabasePath)
                .filter(id -> Objects.equals(messageCitationKey.get(id), citationKey));
    }

    private Stream<Integer> filterMessagesByLibrary(Path bibDatabasePath) {
        return messageLibrary
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(bibDatabasePath.toString()))
                .map(Map.Entry::getKey);
    }

    @Override
    public void close() {
        bibDatabaseChatHistoryMap.clear();
        mvStore.close();
    }
}
