package org.jabref.logic.ai;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AiChatsFile {
    private final Map<String, List<ChatMessage>> chatHistoryMap;

    @JsonCreator
    public AiChatsFile(@JsonProperty("chatHistoryMap") Map<String, List<ChatMessage>> chatHistoryMap) {
        this.chatHistoryMap = chatHistoryMap;
    }

    public Map<String, List<ChatMessage>> getChatHistoryMap() {
        return chatHistoryMap;
    }
}
