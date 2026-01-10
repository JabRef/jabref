package org.jabref.logic.ai.templates;

import java.io.StringWriter;
import java.util.List;

import org.jabref.logic.ai.AiPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.CanonicalBibEntry;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class AiTemplatesService {
    private final AiPreferences aiPreferences;

    private final VelocityEngine velocityEngine = new VelocityEngine();
    private final VelocityContext baseContext = new VelocityContext();

    public AiTemplatesService(AiPreferences aiPreferences) {
        this.aiPreferences = aiPreferences;

        velocityEngine.init();

        baseContext.put("CanonicalBibEntry", CanonicalBibEntry.class);
    }

    public String makeChattingSystemMessage(List<BibEntry> entries) {
        VelocityContext context = new VelocityContext(baseContext);
        context.put("entries", entries);

        return makeTemplate(AiTemplate.CHATTING_SYSTEM_MESSAGE, context);
    }

    public String makeChattingUserMessage(List<BibEntry> entries, String message, List<PaperExcerpt> excerpts) {
        VelocityContext context = new VelocityContext(baseContext);
        context.put("entries", entries);
        context.put("message", message);
        context.put("excerpts", excerpts);

        return makeTemplate(AiTemplate.CHATTING_USER_MESSAGE, context);
    }

    public String makeSummarizationChunkSystemMessage() {
        VelocityContext context = new VelocityContext(baseContext);
        return makeTemplate(AiTemplate.SUMMARIZATION_CHUNK_SYSTEM_MESSAGE, context);
    }

    public String makeSummarizationChunkUserMessage(String text) {
        VelocityContext context = new VelocityContext(baseContext);
        context.put("text", text);

        return makeTemplate(AiTemplate.SUMMARIZATION_CHUNK_USER_MESSAGE, context);
    }

    public String makeSummarizationCombineSystemMessage() {
        VelocityContext context = new VelocityContext(baseContext);
        return makeTemplate(AiTemplate.SUMMARIZATION_COMBINE_SYSTEM_MESSAGE, context);
    }

    public String makeSummarizationCombineUserMessage(List<String> chunks) {
        VelocityContext context = new VelocityContext(baseContext);
        context.put("chunks", chunks);

        return makeTemplate(AiTemplate.SUMMARIZATION_COMBINE_USER_MESSAGE, context);
    }

    public String makeCitationParsingSystemMessage() {
        VelocityContext context = new VelocityContext(baseContext);
        return makeTemplate(AiTemplate.CITATION_PARSING_SYSTEM_MESSAGE, context);
    }

    public String makeCitationParsingUserMessage(String citation) {
        VelocityContext context = new VelocityContext(baseContext);
        context.put("citation", citation);

        return makeTemplate(AiTemplate.CITATION_PARSING_USER_MESSAGE, context);
    }

    public String makeFollowUpQuestionsPrompt(String userMessage, String aiResponse, int count) {
        VelocityContext context = new VelocityContext(baseContext);
        context.put("userMessage", userMessage);
        context.put("aiResponse", aiResponse);
        context.put("count", count);

        return makeTemplate(AiTemplate.FOLLOW_UP_QUESTIONS, context);
    }

    public String makeCitationContextExtractionSystemMessage() {
        VelocityContext context = new VelocityContext(baseContext);
        return makeTemplate(AiTemplate.CITATION_CONTEXT_EXTRACTION_SYSTEM_MESSAGE, context);
    }

    public String makeCitationContextExtractionUserMessage(String text) {
        VelocityContext context = new VelocityContext(baseContext);
        context.put("text", text);
        return makeTemplate(AiTemplate.CITATION_CONTEXT_EXTRACTION_USER_MESSAGE, context);
    }

    private String makeTemplate(AiTemplate template, VelocityContext context) {
        StringWriter writer = new StringWriter();

        velocityEngine.evaluate(context, writer, template.name(), aiPreferences.getTemplate(template));

        return writer.toString();
    }
}
