package org.jabref.logic.ai.summarization.exporters;

import java.util.List;

import org.jabref.logic.ai.chatting.exporters.AiChatMarkdownExporter;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.model.ai.AiMetadata;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

/// Exports an AI summary to Markdown format.
///
/// Internally constructs a single-message dummy chat containing the summary content
/// and delegates to {@link AiChatMarkdownExporter}.
public class AiSummaryMarkdownExporter implements AiSummaryExporter {
    private final AiChatMarkdownExporter chatExporter;

    public AiSummaryMarkdownExporter(BibEntryTypesManager entryTypesManager, FieldPreferences fieldPreferences, String markdownExportTemplate) {
        this.chatExporter = new AiChatMarkdownExporter(entryTypesManager, fieldPreferences, markdownExportTemplate);
    }

    @Override
    public String export(AiMetadata metadata, BibEntry entry, BibDatabaseMode mode, AiSummary summary) {
        List<ChatMessage> dummyChat = List.of(ChatMessage.aiMessage(summary.content(), List.of()));
        return chatExporter.export(metadata, List.of(entry), mode, dummyChat);
    }
}
