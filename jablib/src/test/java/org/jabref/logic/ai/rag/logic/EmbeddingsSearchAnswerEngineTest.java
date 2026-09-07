package org.jabref.logic.ai.rag.logic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.ingestion.util.FileHasher;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class EmbeddingsSearchAnswerEngineTest {

    @TempDir
    Path tempDir;

    private EmbeddingsSearchAnswerEngine engine;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        FilePreferences filePreferences = mock(FilePreferences.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        EmbeddingStore<TextSegment> embeddingStore = mock(EmbeddingStore.class);

        engine = new EmbeddingsSearchAnswerEngine(
                filePreferences,
                embeddingModel,
                embeddingStore,
                0.7,
                10
        );
    }

    @Test
    void findEntryByFileHashFindsMatchingEntryEvenIfDatabaseContextIsEmpty() throws IOException {
        Path file = tempDir.resolve("paper.pdf");
        Files.writeString(file, "test content for hashing");
        String hash = FileHasher.computeHash(file).orElseThrow();

        BibEntry entry = new BibEntry()
                .withCitationKey("Smith2024")
                .withFiles(List.of(new LinkedFile("", file, "PDF")));

        FullBibEntry fullEntry = new FullBibEntry(new BibDatabaseContext(), entry);

        Optional<BibEntry> result = engine.findEntryByFileHash(List.of(fullEntry), hash);

        assertEquals(Optional.of(entry), result);
    }

    @Test
    void findEntryByFileHashFallsBackToSingleEntryWhenHashNotMatched() throws IOException {
        Path file = tempDir.resolve("paper.pdf");
        Files.writeString(file, "test content for hashing");

        BibEntry entry = new BibEntry()
                .withCitationKey("Smith2024")
                .withFiles(List.of(new LinkedFile("", file, "PDF")));

        FullBibEntry fullEntry = new FullBibEntry(new BibDatabaseContext(), entry);

        Optional<BibEntry> result = engine.findEntryByFileHash(List.of(fullEntry), "nonexistenthash");

        assertEquals(Optional.of(entry), result);
    }

    @Test
    void findEntryByFileHashReturnsEmptyForNonMatchingHashWhenMultipleEntries() throws IOException {
        Path file1 = tempDir.resolve("paper1.pdf");
        Files.writeString(file1, "content 1");
        Path file2 = tempDir.resolve("paper2.pdf");
        Files.writeString(file2, "content 2");

        BibEntry entry1 = new BibEntry()
                .withCitationKey("Smith2024")
                .withFiles(List.of(new LinkedFile("", file1, "PDF")));
        BibEntry entry2 = new BibEntry()
                .withCitationKey("Doe2025")
                .withFiles(List.of(new LinkedFile("", file2, "PDF")));

        FullBibEntry fullEntry1 = new FullBibEntry(new BibDatabaseContext(), entry1);
        FullBibEntry fullEntry2 = new FullBibEntry(new BibDatabaseContext(), entry2);

        Optional<BibEntry> result = engine.findEntryByFileHash(List.of(fullEntry1, fullEntry2), "nonexistenthash");

        assertEquals(Optional.empty(), result);
    }

    @Test
    void findEntryByFileHashFindsCorrectEntryAmongMultipleEntries() throws IOException {
        Path file1 = tempDir.resolve("paper1.pdf");
        Files.writeString(file1, "content 1");

        Path file2 = tempDir.resolve("paper2.pdf");
        Files.writeString(file2, "content 2");
        String hash2 = FileHasher.computeHash(file2).orElseThrow();

        BibEntry entry1 = new BibEntry()
                .withCitationKey("Smith2024")
                .withFiles(List.of(new LinkedFile("", file1, "PDF")));
        BibEntry entry2 = new BibEntry()
                .withCitationKey("Doe2025")
                .withFiles(List.of(new LinkedFile("", file2, "PDF")));

        FullBibEntry fullEntry1 = new FullBibEntry(new BibDatabaseContext(), entry1);
        FullBibEntry fullEntry2 = new FullBibEntry(new BibDatabaseContext(), entry2);

        Optional<BibEntry> result = engine.findEntryByFileHash(List.of(fullEntry1, fullEntry2), hash2);

        assertEquals(Optional.of(entry2), result);
    }
}
