package org.jabref.logic.ai.preferences;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A collection of default AI templates loaded from resource files.
///
/// This collection is made into a separate class (instead of putting into defaults at [org.jabref.logic.preferences.JabRefCliPreferences]),
/// because they are too big. Templates are stored in src/main/resources/ai/templates/ and loaded at startup.
public final class AiDefaultTemplates {
    public static final String CHATTING_SYSTEM_MESSAGE_TEMPLATE;
    public static final String CHATTING_USER_MESSAGE_TEMPLATE;
    public static final String SUMMARIZATION_CHUNK_SYSTEM_MESSAGE_TEMPLATE;
    public static final String SUMMARIZATION_COMBINE_SYSTEM_MESSAGE_TEMPLATE;
    public static final String SUMMARIZATION_FULL_DOCUMENT_SYSTEM_MESSAGE_TEMPLATE;
    public static final String CITATION_PARSING_SYSTEM_MESSAGE_TEMPLATE;
    public static final String CITATION_PARSING_USER_MESSAGE_TEMPLATE;
    public static final String MARKDOWN_CHAT_EXPORT_TEMPLATE;
    public static final String FOLLOW_UP_QUESTIONS_TEMPLATE;

    private static final Logger LOGGER = LoggerFactory.getLogger(AiDefaultTemplates.class);
    private static final String TEMPLATE_BASE_PATH = "/ai/templates/";

    static {
        CHATTING_SYSTEM_MESSAGE_TEMPLATE = loadTemplate("chatting_system_message");
        CHATTING_USER_MESSAGE_TEMPLATE = loadTemplate("chatting_user_message");
        SUMMARIZATION_CHUNK_SYSTEM_MESSAGE_TEMPLATE = loadTemplate("summarization_chunk_system_message");
        SUMMARIZATION_COMBINE_SYSTEM_MESSAGE_TEMPLATE = loadTemplate("summarization_combine_system_message");
        SUMMARIZATION_FULL_DOCUMENT_SYSTEM_MESSAGE_TEMPLATE = loadTemplate("summarization_full_document_system_message");
        CITATION_PARSING_SYSTEM_MESSAGE_TEMPLATE = loadTemplate("citation_parsing_system_message");
        CITATION_PARSING_USER_MESSAGE_TEMPLATE = loadTemplate("citation_parsing_user_message");
        MARKDOWN_CHAT_EXPORT_TEMPLATE = loadTemplate("markdown_chat_export");
        FOLLOW_UP_QUESTIONS_TEMPLATE = loadTemplate("follow_up_questions");
    }

    private AiDefaultTemplates() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    /// Loads a template from resources.
    /// @param templateName the name of the template (without extension), e.g., "chatting_system_message"
    /// @return the template content as a string
    /// @throws IllegalStateException if the template cannot be loaded
    private static String loadTemplate(String templateName) {
        String resourcePath = TEMPLATE_BASE_PATH + templateName + ".vm";
        try (InputStream inputStream = AiDefaultTemplates.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Template resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to load template: {}", resourcePath, e);
            throw new IllegalStateException("Failed to load template: " + resourcePath, e);
        }
    }
}
