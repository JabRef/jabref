package org.jabref.logic.ai.templates;

import java.io.StringWriter;
import java.util.List;

import org.jabref.model.ai.AiMetadata;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.pipeline.RelevantInformation;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.CanonicalBibEntry;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public final class AiTemplateRenderer {
    private static final VelocityEngine VELOCITY_ENGINE = new VelocityEngine();
    private static final VelocityContext BASE_CONTEXT = new VelocityContext();

    static {
        VELOCITY_ENGINE.init();
        BASE_CONTEXT.put("CanonicalBibEntry", CanonicalBibEntry.class);
    }

    private AiTemplateRenderer() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    public static String renderChattingSystemMessage(String templateSource, List<BibEntry> entries) {
        VelocityContext context = new VelocityContext(BASE_CONTEXT);
        context.put("entries", entries);
        return render(templateSource, "CHATTING_SYSTEM_MESSAGE", context);
    }

    public static String renderChattingUserMessage(String templateSource, List<BibEntry> entries, String message, List<RelevantInformation> excerpts) {
        VelocityContext context = new VelocityContext(BASE_CONTEXT);
        context.put("entries", entries);
        context.put("message", message);
        context.put("excerpts", excerpts);
        return render(templateSource, "CHATTING_USER_MESSAGE", context);
    }

    public static String renderSummarizationChunkSystemMessage(String templateSource) {
        VelocityContext context = new VelocityContext(BASE_CONTEXT);
        return render(templateSource, "SUMMARIZATION_CHUNK_SYSTEM_MESSAGE", context);
    }

    public static String renderSummarizationCombineSystemMessage(String templateSource) {
        VelocityContext context = new VelocityContext(BASE_CONTEXT);
        return render(templateSource, "SUMMARIZATION_COMBINE_SYSTEM_MESSAGE", context);
    }

    public static String renderSummarizationFullDocumentSystemMessage(String templateSource) {
        VelocityContext context = new VelocityContext(BASE_CONTEXT);
        return render(templateSource, "SUMMARIZATION_FULL_DOCUMENT_SYSTEM_MESSAGE", context);
    }

    public static String renderCitationParsingSystemMessage(String templateSource) {
        VelocityContext context = new VelocityContext(BASE_CONTEXT);
        return render(templateSource, "CITATION_PARSING_SYSTEM_MESSAGE", context);
    }

    public static String renderMarkdownChatExport(String templateSource, AiMetadata metadata, String bibtex, List<ChatMessage> messages) {
        VelocityContext context = new VelocityContext(BASE_CONTEXT);
        context.put("metadata", metadata);
        context.put("bibtex", bibtex);
        context.put("messages", messages);
        return render(templateSource, "MARKDOWN_CHAT_EXPORT", context);
    }

    public static String renderFollowUpQuestionsPrompt(String templateSource, String userMessage, String aiResponse, int count) {
        VelocityContext context = new VelocityContext(BASE_CONTEXT);
        context.put("userMessage", userMessage);
        context.put("aiResponse", aiResponse);
        context.put("count", count);
        return render(templateSource, "FOLLOW_UP_QUESTIONS", context);
    }

    private static String render(String templateSource, String logName, VelocityContext context) {
        StringWriter writer = new StringWriter();
        VELOCITY_ENGINE.evaluate(context, writer, logName, templateSource);
        return writer.toString();
    }
}
