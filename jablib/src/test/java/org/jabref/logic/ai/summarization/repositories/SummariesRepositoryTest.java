package org.jabref.logic.ai.summarization.repositories;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.jabref.model.ai.AiMetadata;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.ai.summarization.AiSummaryIdentifier;
import org.jabref.model.ai.summarization.SummarizatorKind;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// [utest->req~ai.summarization.general.storage~1]
class SummariesRepositoryTest {

    private static final AiSummaryIdentifier IDENTIFIER = new AiSummaryIdentifier("lib-1", "Smith2024");

    @TempDir
    private static Path tempDir;

    private static List<SummariesRepository> repositories() {
        return List.of(
                new MVStoreSummariesRepository(_ -> {
                }, tempDir.resolve("summaries-test.mv"))
        );
    }

    private static AiSummary buildSummary(String content) {
        AiMetadata metadata = new AiMetadata(AiProvider.OPEN_AI, "gpt-4", Instant.now());
        return new AiSummary(metadata, SummarizatorKind.CHUNKED, content);
    }

    @ParameterizedTest
    @MethodSource("repositories")
    void setAndGetRoundTrips(SummariesRepository repo) {
        AiSummary summary = buildSummary("This paper is very important.");

        repo.set(IDENTIFIER, summary);

        Optional<AiSummary> retrieved = repo.get(IDENTIFIER);
        assertTrue(retrieved.isPresent());
        assertEquals("This paper is very important.", retrieved.get().content());
    }

    @ParameterizedTest
    @MethodSource("repositories")
    void getReturnsEmptyForUnknownIdentifier(SummariesRepository repo) {
        AiSummaryIdentifier unknown = new AiSummaryIdentifier("lib-x", "Unknown2099");

        Optional<AiSummary> result = repo.get(unknown);

        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("repositories")
    void clearRemovesTheSummary(SummariesRepository repo) {
        AiSummary summary = buildSummary("temporary summary");
        repo.set(IDENTIFIER, summary);

        repo.clear(IDENTIFIER);

        assertTrue(repo.get(IDENTIFIER).isEmpty());
    }

    @ParameterizedTest
    @MethodSource("repositories")
    void overwritingExistingSummaryReplacesIt(SummariesRepository repo) {
        repo.set(IDENTIFIER, buildSummary("original"));
        repo.set(IDENTIFIER, buildSummary("updated"));

        Optional<AiSummary> result = repo.get(IDENTIFIER);
        assertTrue(result.isPresent());
        assertEquals("updated", result.get().content());
    }

    @ParameterizedTest
    @MethodSource("repositories")
    void differentIdentifiersStoredIndependently(SummariesRepository repo) {
        AiSummaryIdentifier id1 = new AiSummaryIdentifier("lib-1", "Alpha2024");
        AiSummaryIdentifier id2 = new AiSummaryIdentifier("lib-1", "Beta2024");

        repo.set(id1, buildSummary("alpha summary"));
        repo.set(id2, buildSummary("beta summary"));

        assertEquals("alpha summary", repo.get(id1).get().content());
        assertEquals("beta summary", repo.get(id2).get().content());
    }
}
