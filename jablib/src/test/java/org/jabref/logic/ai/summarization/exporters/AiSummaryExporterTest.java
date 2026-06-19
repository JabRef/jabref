package org.jabref.logic.ai.summarization.exporters;

import java.time.Instant;
import java.util.List;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.model.ai.AiMetadata;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.ai.summarization.SummarizatorKind;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiSummaryExporterTest {

    private static final String MARKDOWN_TEMPLATE = """
            # AI Summary Export

            **Provider:** $metadata.aiProvider
            **Model:** $metadata.model

            ## Summary

            #foreach( $msg in $messages )
            $msg.content
            #end
            """;

    private AiMetadata metadata;
    private BibEntry entry;
    private AiSummary summary;

    @BeforeEach
    void setUp() {
        metadata = new AiMetadata(AiProvider.OPEN_AI, "gpt-4", Instant.now());
        entry = new BibEntry(StandardEntryType.Article).withCitationKey("smith2024");
        summary = new AiSummary(metadata, SummarizatorKind.CHUNKED, "This paper discusses important findings.");
    }

    static List<AiSummaryExporter> exporters() {
        BibEntryTypesManager entryTypesManager = new BibEntryTypesManager();
        FieldPreferences fieldPreferences = new FieldPreferences(true, List.of(), List.of());
        return List.of(
                new AiSummaryMarkdownExporter(entryTypesManager, fieldPreferences, MARKDOWN_TEMPLATE),
                new AiSummaryJsonExporter(entryTypesManager, fieldPreferences)
        );
    }

    @ParameterizedTest
    @MethodSource("exporters")
    void summaryContentAppearsInExport(AiSummaryExporter exporter) {
        String result = exporter.export(metadata, entry, BibDatabaseMode.BIBTEX, summary);

        assertTrue(result.contains("important findings"));
    }

    @ParameterizedTest
    @MethodSource("exporters")
    void summaryWithSpecialCharactersAppearsInExport(AiSummaryExporter exporter) {
        AiSummary specialSummary = new AiSummary(metadata, SummarizatorKind.CHUNKED,
                "Results show \"significant\" improvement & <better> outcomes.");

        String result = exporter.export(metadata, entry, BibDatabaseMode.BIBTEX, specialSummary);

        assertTrue(result.contains("significant") && result.contains("improvement"));
    }
}
