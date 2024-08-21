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
    private static final Logger LOGGER = LoggerFactory.getLogger(MVStoreChatHistory.class);

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
        Map<Integer, ChatHistoryRecord> map = getMap(bibDatabasePath, citationKey);

        return map
                .entrySet()
                // We need to check all keys, because upon deletion, there can be "holes" in the integer.
                .stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(entry -> entry.getValue().toLangchainMessage())
                .toList();
    }

    @Override
    public void storeMessagesForEntry(Path bibDatabasePath, String citationKey, List<ChatMessage> messages) {
        Map<Integer, ChatHistoryRecord> map = getMap(bibDatabasePath, citationKey);
        map.clear();

        new IntRange(0, messages.size() - 1).forEach(i ->
            map.put(i, ChatHistoryRecord.fromLangchainMessage(messages.get(i)))
        );
    }

    private Map<Integer, ChatHistoryRecord> getMap(Path bibDatabasePath, String citationKey) {
        return mvStore.openMap("chathistory-" + bibDatabasePath + "-" + citationKey);
    }
}
