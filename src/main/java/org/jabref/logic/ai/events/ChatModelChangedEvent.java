package org.jabref.logic.ai.events;

import dev.langchain4j.model.chat.ChatLanguageModel;

public class ChatModelChangedEvent {
    private final ChatLanguageModel chatModel;

    public ChatModelChangedEvent(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    public ChatLanguageModel getChatModel() {
        return chatModel;
    }
}
