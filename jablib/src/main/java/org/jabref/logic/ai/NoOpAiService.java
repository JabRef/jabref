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

/// Null Object implementation of [AiService] for runtimes without AI capabilities.
@NullMarked
public class NoOpAiService implements AiService {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void setupDatabase(BibDatabaseContext context, boolean isDummyContext) {
        // No-op
    }

    @Override
    public Optional<PlainCitationParser> getLlmPlainCitationParser(ImportFormatPreferences importFormatPreferences) {
        return Optional.empty();
    }

    @Override
    public void clearEmbeddingsFor(List<LinkedFile> linkedFiles, BibDatabaseContext bibDatabaseContext, FilePreferences filePreferences) {
        // No-op
    }

    @Override
    public void generateEmbeddingsForGroup(BibDatabaseContext context, AbstractGroup group, List<LinkedFile> linkedFiles, FilePreferences filePreferences) {
        // No-op
    }

    @Override
    public void generateSummariesForEntries(BibDatabaseContext context, List<BibEntry> entries, FilePreferences filePreferences) {
        // No-op
    }

    @Override
    public void close() {
        // No-op
    }
}
