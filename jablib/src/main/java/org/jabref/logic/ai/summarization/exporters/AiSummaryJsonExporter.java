package org.jabref.logic.ai.summarization.exporters;

import java.util.List;

import org.jabref.logic.ai.chatting.exporters.AiChatJsonExporter;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.model.ai.AiMetadata;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

/// Exports an AI summary to JSON format.
///
/// Internally constructs a single-message dummy chat containing the summary content
/// and delegates to {@link AiChatJsonExporter}.
public class AiSummaryJsonExporter implements AiSummaryExporter {
    private final AiChatJsonExporter chatExporter;

    public AiSummaryJsonExporter(BibEntryTypesManager entryTypesManager, FieldPreferences fieldPreferences) {
        this.chatExporter = new AiChatJsonExporter(entryTypesManager, fieldPreferences);
    }

    @Override
    public String export(AiMetadata metadata, BibEntry entry, BibDatabaseMode mode, AiSummary summary) {
        List<ChatMessage> dummyChat = List.of(ChatMessage.aiMessage(summary.content(), List.of()));
        return chatExporter.export(metadata, List.of(entry), mode, dummyChat);
    }
}
