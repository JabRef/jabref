package org.jabref.logic.ai.templates;

import java.util.List;

import org.jabref.logic.ai.preferences.AiDefaultTemplates;
import org.jabref.model.ai.pipeline.RelevantInformation;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiTemplateRendererTest {

    @Test
    void renderChattingUserMessageRendersCitationKeyForExcerpts() {
        BibEntry entry = new BibEntry().withCitationKey("Smith2024");
        RelevantInformation excerpt = new RelevantInformation("Smith2024", "Some excerpt text from paper.");

        String rendered = AiTemplateRenderer.renderChattingUserMessage(
                AiDefaultTemplates.CHATTING_USER_MESSAGE_TEMPLATE,
                List.of(entry),
                "User query",
                List.of(excerpt)
        );

        assertTrue(rendered.contains("Smith2024"));
        assertTrue(rendered.contains("Some excerpt text from paper."));
        assertTrue(rendered.contains("User query"));
    }

    @Test
    void renderChattingUserMessageRendersWithLegacyExcerptSource() {
        BibEntry entry = new BibEntry().withCitationKey("Smith2024");
        RelevantInformation excerpt = new RelevantInformation("Smith2024", "Some excerpt text from paper.");

        String legacyTemplate = """
                $message

                Here is some relevant information for you:
                #foreach( $excerpt in $excerpts )
                ${excerpt.source}:
                ${excerpt.text()}
                #end""";

        String rendered = AiTemplateRenderer.renderChattingUserMessage(
                legacyTemplate,
                List.of(entry),
                "User query",
                List.of(excerpt)
        );

        assertTrue(rendered.contains("Smith2024:"));
        assertTrue(rendered.contains("Some excerpt text from paper."));
    }
}
