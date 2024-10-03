package org.jabref.logic.ai.templates;

public enum AiTemplate {
    // System message that is applied in the AI chat.
    CHATTING_SYSTEM_MESSAGE,

    // Template of a last user message in the AI chat with embeddings.
    CHATTING_USER_MESSAGE,

    // Template that is used to summarize the chunks of text.
    SUMMARIZATION_CHUNK,

    // Template that is used to combine the summarized chunks of text.
    SUMMARIZATION_COMBINE;

    public String getLocalizedName() {
        return switch (this) {
            case CHATTING_SYSTEM_MESSAGE ->
                    "System message for chatting";
            case CHATTING_USER_MESSAGE ->
                    "User message for chatting";
            case SUMMARIZATION_CHUNK ->
                    "Summarization chunk";
            case SUMMARIZATION_COMBINE ->
                    "Summarization combine";
        };
    }
}
