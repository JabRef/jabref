package org.jabref.logic.ai.templates;

import java.io.StringWriter;
import java.util.List;

import org.jabref.logic.ai.AiPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.CanonicalBibEntry;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class TemplatesService {
    private final AiPreferences aiPreferences;
    private final VelocityEngine velocityEngine = new VelocityEngine();
    private final VelocityContext baseContext = new VelocityContext();

    public TemplatesService(AiPreferences aiPreferences) {
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

    public String makeSummarizationChunk(String text) {
        VelocityContext context = new VelocityContext(baseContext);
        context.put("text", text);

        return makeTemplate(AiTemplate.SUMMARIZATION_CHUNK, context);
    }

    public String makeSummarizationCombine(List<String> chunks) {
        VelocityContext context = new VelocityContext(baseContext);
        context.put("chunks", chunks);

        return makeTemplate(AiTemplate.SUMMARIZATION_COMBINE, context);
    }

    private String makeTemplate(AiTemplate template, VelocityContext context) {
        StringWriter writer = new StringWriter();

        velocityEngine.evaluate(context, writer, template.name(), aiPreferences.getTemplate(template));

        return writer.toString();
    }
}
