package org.jabref.logic.ai.summarization.exporters;

import org.jabref.model.ai.AiMetadata;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;

/// Common contract for exporting an AI summary.
///
/// Implementations may produce different output formats (Markdown, JSON, etc.).
public interface AiSummaryExporter {
    String export(AiMetadata metadata, BibEntry entry, BibDatabaseMode mode, AiSummary summary);
}
