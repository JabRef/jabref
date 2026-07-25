package org.jabref.logic.ai.chatting.exporters;

import java.util.List;

import org.jabref.model.ai.AiMetadata;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;

/// Common contract for exporting an AI chat conversation.
///
/// Implementations may produce different output formats (Markdown, JSON, etc.).
public interface AiChatExporter {
    String export(AiMetadata metadata, List<BibEntry> entries, BibDatabaseMode mode, List<ChatMessage> messages);
}
