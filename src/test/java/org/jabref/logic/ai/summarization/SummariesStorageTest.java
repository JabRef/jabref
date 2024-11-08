package org.jabref.logic.ai.summarization;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import org.jabref.model.ai.AiProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

abstract class SummariesStorageTest {
    @TempDir Path tempDir;

    private SummariesStorage summariesStorage;
    private Path bibPath;

    abstract SummariesStorage makeSummariesStorage(Path path);

    abstract void close(SummariesStorage summariesStorage);

    @BeforeEach
    void setUp() {
        bibPath = tempDir.resolve("test.bib");
        summariesStorage = makeSummariesStorage(tempDir.resolve("test.bib"));
    }

    private void reopen() {
        close(summariesStorage);
        setUp();
    }

    @AfterEach
    void tearDown() {
        close(summariesStorage);
    }

    @Test
    void set() {
        summariesStorage.set(bibPath, "citationKey", new Summary(LocalDateTime.now(), AiProvider.OPEN_AI, "model", "contents"));
        reopen();
        assertEquals(Optional.of("contents"), summariesStorage.get(bibPath, "citationKey").map(Summary::content));
    }

    @Test
    void clear() {
        summariesStorage.set(bibPath, "citationKey", new Summary(LocalDateTime.now(), AiProvider.OPEN_AI, "model", "contents"));
        reopen();
        summariesStorage.clear(bibPath, "citationKey");
        reopen();
        assertEquals(Optional.empty(), summariesStorage.get(bibPath, "citationKey"));
    }
}
