package org.jabref.logic.ai.templates;

import org.jabref.logic.l10n.Localization;

public enum AiTemplate {
    // Templates that are used in AI chats,
    CHATTING_SYSTEM_MESSAGE,
    CHATTING_USER_MESSAGE,

    // Templates that are used for summarization of text chunks,
    SUMMARIZATION_CHUNK_SYSTEM_MESSAGE,
    SUMMARIZATION_CHUNK_USER_MESSAGE,

    // Templates that are used for combining summaries of several chunks,
    SUMMARIZATION_COMBINE_SYSTEM_MESSAGE,
    SUMMARIZATION_COMBINE_USER_MESSAGE,

    // Templates that are used to convert a raw citation into a {@link BibEntry}.
    CITATION_PARSING_SYSTEM_MESSAGE,
    CITATION_PARSING_USER_MESSAGE,

    // Template that is used to generate follow-up questions in chat.
    FOLLOW_UP_QUESTIONS,

    // Templates that are used for citation context extraction.
    CITATION_CONTEXT_EXTRACTION_SYSTEM_MESSAGE,
    CITATION_CONTEXT_EXTRACTION_USER_MESSAGE;

    public String getLocalizedName() {
        return switch (this) {
            case CHATTING_SYSTEM_MESSAGE ->
                    Localization.lang("System message for chatting");
            case CHATTING_USER_MESSAGE ->
                    Localization.lang("User message for chatting");
            case SUMMARIZATION_CHUNK_SYSTEM_MESSAGE ->
                    Localization.lang("System message for summarization of a chunk");
            case SUMMARIZATION_CHUNK_USER_MESSAGE ->
                    Localization.lang("User message for summarization of a chunk");
            case SUMMARIZATION_COMBINE_SYSTEM_MESSAGE ->
                    Localization.lang("System message for summarization of several chunks");
            case SUMMARIZATION_COMBINE_USER_MESSAGE ->
                    Localization.lang("User message for summarization of several chunks");
            case CITATION_PARSING_SYSTEM_MESSAGE ->
                    Localization.lang("System message for citation parsing");
            case CITATION_PARSING_USER_MESSAGE ->
                    Localization.lang("User message for citation parsing");
            case FOLLOW_UP_QUESTIONS ->
                    Localization.lang("Prompt for generating follow-up questions");
            case CITATION_CONTEXT_EXTRACTION_SYSTEM_MESSAGE ->
                    Localization.lang("System message for citation context extraction");
            case CITATION_CONTEXT_EXTRACTION_USER_MESSAGE ->
                    Localization.lang("User message for citation context extraction");
        };
    }
}
