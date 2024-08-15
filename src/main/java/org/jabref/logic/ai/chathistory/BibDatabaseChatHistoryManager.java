package org.jabref.logic.ai.chathistory;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.gui.StateManager;
import org.jabref.logic.ai.misc.ErrorMessage;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.InternalField;

import com.airhacks.afterburner.injection.Injector;
import com.google.common.eventbus.Subscribe;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
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
public class BibDatabaseChatHistoryManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibDatabaseChatHistoryManager.class);

    private final StateManager stateManager = Injector.instantiateModelOrService(StateManager.class);

    private record ChatHistoryRecord(String className, String content) implements Serializable {
        public ChatMessage toLangchainMessage() {
            if (className.equals(AiMessage.class.getName())) {
                return new AiMessage(content);
            } else if (className.equals(UserMessage.class.getName())) {
                return new UserMessage(content);
            } else if (className.equals(ErrorMessage.class.getName())) {
                return new ErrorMessage(content);
            } else {
                LOGGER.warn("BibDatabaseChatHistoryManager supports only AI and user messages, but retrieved message has other type: {}. Will treat as an AI message.", className);
                return new AiMessage(content);
            }
        }
    }

    private final MVStore mvStore;

    public BibDatabaseChatHistoryManager(MVStore mvStore) {
        this.mvStore = mvStore;
    }

    private Map<Integer, ChatHistoryRecord> getMap(Path bibDatabasePath, String citationKey) {
        return mvStore.openMap("chathistory-" + bibDatabasePath + "-" + citationKey);
    }

    public AiChatHistory getChatHistory(Path bibDatabasePath, String citationKey) {
        return new AiChatHistory() {
            @Override
            public List<ChatMessage> getMessages() {
                Map<Integer, ChatHistoryRecord> messages = getMap(bibDatabasePath, citationKey);

                return messages
                        .entrySet()
                        .stream()
                        .sorted(Comparator.comparingInt(Map.Entry::getKey))
                        .map(entry -> entry.getValue().toLangchainMessage())
                        .toList();
            }

            @Override
            public void add(ChatMessage chatMessage) {
                Map<Integer, ChatHistoryRecord> map = getMap(bibDatabasePath, citationKey);

                // We count 0-based, thus "size()" is the next number.
                // 0 entries -> 0 is the first new id.
                // 1 entry -> 0 is assigned, 1 is the next number, which is also the size.
                // But if an entry is removed, keys are not updated, so we have to find the maximum key.
                int id = map.keySet().stream().max(Integer::compareTo).orElse(0) + 1;

                String content = getContentFromLangchainMessage(chatMessage);

                map.put(id, new ChatHistoryRecord(chatMessage.getClass().getName(), content));
            }

            @Override
            public void remove(int index) {
                Map<Integer, ChatHistoryRecord> map = getMap(bibDatabasePath, citationKey);

                Optional<Integer> id = map
                        .entrySet()
                        .stream()
                        .sorted(Comparator.comparingInt(Map.Entry::getKey))
                        .skip(index)
                        .map(Map.Entry::getKey)
                        .findFirst();

                if (id.isPresent()) {
                    map.remove(id.get());
                } else {
                    LOGGER.error("Attempted to delete a message that does not exist in the chat history at index {}", index);
                }
            }

            @Override
            public void clear() {
                getMap(bibDatabasePath, citationKey).clear();
            }
        };
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
                LOGGER.warn("BibDatabaseChatHistoryManager supports only AI, user. and error messages, but added message has other type: {}", chatMessage.type().name());
                return "";
            }
        }

        return content;
    }

    @Subscribe
    private void fieldChangedEventListener(FieldChangedEvent event) {
        // TODO: This methods doesn't take into account if the new citation key is valid.

        if (event.getField() != InternalField.KEY_FIELD) {
            return;
        }

        Optional<BibDatabaseContext> bibDatabaseContext = stateManager.getOpenDatabases().stream().filter(dbContext -> dbContext.getDatabase().getEntries().contains(event.getBibEntry())).findFirst();

        if (bibDatabaseContext.isEmpty()) {
            LOGGER.error("Could not listen to field change event because no database context was found. BibEntry: {}", event.getBibEntry());
            return;
        }

        Optional<Path> bibDatabasePath = bibDatabaseContext.get().getDatabasePath();

        if (bibDatabasePath.isEmpty()) {
            LOGGER.error("Could not listen to field change event because no database path was found. BibEntry: {}", event.getBibEntry());
            return;
        }

        Map<Integer, ChatHistoryRecord> oldMap = getMap(bibDatabasePath.get(), event.getOldValue());
        getMap(bibDatabasePath.get(), event.getNewValue()).putAll(oldMap);
        oldMap.clear();
    }
}
