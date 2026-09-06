package org.jabref.logic.ai;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.plaincitation.PlainCitationParser;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.groups.AbstractGroup;

import org.jspecify.annotations.NullMarked;

/// The boundary interface for AI functionality.
/// Represents the high-level façade and database lifecycle hooks for AI features.
@NullMarked
public interface AiService extends AutoCloseable {

    String VERSION = "2";

    /// Indicates whether the AI subsystem is available in this edition/runtime.
    boolean isAvailable();

    /// Sets up AI database listeners and runs migrations if enabled.
    void setupDatabase(BibDatabaseContext context, boolean isDummyContext);

    /// Returns the LLM-based plain citation parser if available.
    Optional<PlainCitationParser> getLlmPlainCitationParser(ImportFormatPreferences importFormatPreferences);

    /// Clears cached embeddings for the given linked files in the database context.
    void clearEmbeddingsFor(List<LinkedFile> linkedFiles, BibDatabaseContext bibDatabaseContext, FilePreferences filePreferences);

    /// Generates embeddings for all matching entries in the given group.
    void generateEmbeddingsForGroup(BibDatabaseContext context, AbstractGroup group, List<LinkedFile> linkedFiles, FilePreferences filePreferences);

    /// Generates summaries for the given entries.
    void generateSummariesForEntries(BibDatabaseContext context, List<BibEntry> entries, FilePreferences filePreferences);

    @Override
    void close() throws Exception;
}
