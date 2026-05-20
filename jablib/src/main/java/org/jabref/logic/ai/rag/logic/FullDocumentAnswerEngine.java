package org.jabref.logic.ai.rag.logic;

import java.util.List;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.ingestion.logic.parsing.UniversalContentParser;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.ai.pipeline.RelevantInformation;
import org.jabref.model.entry.BibEntry;

// [impl->feat~ai.answer-engines.full-document~1]
public class FullDocumentAnswerEngine implements AnswerEngine {
    private final FilePreferences filePreferences;

    private final UniversalContentParser universalContentParser = new UniversalContentParser();

    public FullDocumentAnswerEngine(FilePreferences filePreferences) {
        this.filePreferences = filePreferences;
    }

    @Override
    public List<RelevantInformation> process(
            String query,
            List<FullBibEntry> entriesFilter
    ) {
        return entriesFilter
                .stream()
                .flatMap(entryIdentifier ->
                        entryIdentifier
                                .entry()
                                .getFiles()
                                .stream()
                                .flatMap(linkedFile ->
                                        linkedFile
                                                .findIn(entryIdentifier.databaseContext(), filePreferences)
                                                .flatMap(universalContentParser::parse)
                                                .map(c -> new RelevantInformation(FullBibEntry.findEntryByLink(entryIdentifier, linkedFile.getLink()).flatMap(BibEntry::getCitationKey).orElse(null), c))
                                                .stream()
                                )
                )
                .toList();
    }

    @Override
    public AnswerEngineKind getKind() {
        return AnswerEngineKind.FULL_DOCUMENT;
    }
}
