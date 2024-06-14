package org.jabref.logic.ai;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

public class BibDatabaseChats {
    private final MVStore mvStore;

    private final MVMap<Integer, ChatMessageType> messageType;
    private final MVMap<Integer, String> messageContent;
    private final MVMap<Integer, String> messageCitationKey;

    public static final String AI_CHATS_FILE_EXTENSION = "aichats";

    public BibDatabaseChats(Path bibDatabasePath) {
        this.mvStore = MVStore.open(bibDatabasePath + "." + AI_CHATS_FILE_EXTENSION);
        this.messageType = this.mvStore.openMap("messageType");
        this.messageContent = this.mvStore.openMap("messageContent");
        this.messageCitationKey = this.mvStore.openMap("messageCitationKey");
    }

    public Stream<ChatMessage> getAllMessagesForEntry(String citationKey) {
        return messageCitationKey
                .entrySet()
                .stream()
                .filter(integerStringEntry -> integerStringEntry.getValue().equals(citationKey))
                .map(Map.Entry::getKey)
                .sorted() // Violating normal forms :)
                .map(id -> new ChatMessage(messageType.get(id), messageContent.get(id)));
    }

    public void addMessage(String citationKey, ChatMessage message) {
        int id = getMaxInt() + 1;

        this.messageType.put(id, message.getType());
        this.messageContent.put(id, message.getContent());
        this.messageCitationKey.put(id, citationKey);
    }

    public void close() {
        this.mvStore.close();
    }

    private int getMaxInt() {
        return messageContent.size();
    }
}

