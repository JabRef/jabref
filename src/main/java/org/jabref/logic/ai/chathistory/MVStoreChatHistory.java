package org.jabref.logic.ai.chathistory;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jabref.logic.ai.misc.ErrorMessage;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import kotlin.ranges.IntRange;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MVStoreChatHistory implements ChatHistoryImplementation {
    private final MVStore mvStore;

    private record ChatHistoryRecord(String className, String content) implements Serializable {
        private static final Logger LOGGER = LoggerFactory.getLogger(ChatHistoryRecord.class);

        public static ChatHistoryRecord fromLangchainMessage(ChatMessage chatMessage) {
            String className = chatMessage.getClass().getName();
            String content = getContentFromLangchainMessage(chatMessage);
            return new ChatHistoryRecord(className, content);
        }

        private static String getContentFromLangchainMessage(ChatMessage chatMessage) {
            String content;

            switch (chatMessage) {
                case AiMessage aiMessage ->
                        content = aiMessage.text();
                case UserMessage userMessage ->
                        content = userMessage.singleText();
                case ErrorMessage errorMessage ->
                        content = errorMessage.getText();
                default -> {
                    LOGGER.warn("ChatHistoryRecord supports only AI, user. and error messages, but added message has other type: {}", chatMessage.type().name());
                    return "";
                }
            }

            return content;
        }

        public ChatMessage toLangchainMessage() {
            if (className.equals(AiMessage.class.getName())) {
                return new AiMessage(content);
            } else if (className.equals(UserMessage.class.getName())) {
                return new UserMessage(content);
            } else if (className.equals(ErrorMessage.class.getName())) {
                return new ErrorMessage(content);
            } else {
                LOGGER.warn("ChatHistoryRecord supports only AI and user messages, but retrieved message has other type: {}. Will treat as an AI message.", className);
                return new AiMessage(content);
            }
        }
    }

    public MVStoreChatHistory(MVStore mvStore) {
        this.mvStore = mvStore;
    }

    @Override
    public List<ChatMessage> loadMessagesForEntry(Path bibDatabasePath, String citationKey) {
        return loadMessagesFromMap(getMapForEntry(bibDatabasePath, citationKey));
    }

    @Override
    public void storeMessagesForEntry(Path bibDatabasePath, String citationKey, List<ChatMessage> messages) {
        storeMessagesForMap(getMapForEntry(bibDatabasePath, citationKey), messages);
    }

    @Override
    public List<ChatMessage> loadMessagesForGroup(Path bibDatabasePath, String name) {
        return loadMessagesFromMap(getMapForGroup(bibDatabasePath, name));
    }

    @Override
    public void storeMessagesForGroup(Path bibDatabasePath, String name, List<ChatMessage> messages) {
        storeMessagesForMap(getMapForGroup(bibDatabasePath, name), messages);
    }

    private List<ChatMessage> loadMessagesFromMap(Map<Integer, ChatHistoryRecord> map) {
        return map
                .entrySet()
                // We need to check all keys, because upon deletion, there can be "holes" in the integer.
                .stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(entry -> entry.getValue().toLangchainMessage())
                .toList();
    }

    private void storeMessagesForMap(Map<Integer, ChatHistoryRecord> map, List<ChatMessage> messages) {
        map.clear();

        new IntRange(0, messages.size() - 1).forEach(i ->
                map.put(i, ChatHistoryRecord.fromLangchainMessage(messages.get(i)))
        );
    }

    private Map<Integer, ChatHistoryRecord> getMapForEntry(Path bibDatabasePath, String citationKey) {
        return getMap(bibDatabasePath, "entry", citationKey);
    }

    private Map<Integer, ChatHistoryRecord> getMapForGroup(Path bibDatabasePath, String name) {
        return getMap(bibDatabasePath, "group", name);
    }

    private Map<Integer, ChatHistoryRecord> getMap(Path bibDatabasePath, String type, String name) {
        return mvStore.openMap("chathistory-" + bibDatabasePath + "-" + type + "-" + name);
    }
}
