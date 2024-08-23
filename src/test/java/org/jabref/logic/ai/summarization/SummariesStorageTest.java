package org.jabref.logic.ai.summarization;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import org.jabref.preferences.ai.AiProvider;

import org.h2.mvstore.MVStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SummariesStorageTest {

    @TempDir Path tempDir;

    private MVStore mvStore;
    private SummariesStorage summariesStorage;
    private Path bibPath;

    @BeforeEach
    void setUp() {
        mvStore = MVStore.open(tempDir.resolve("test.mv").toString());
        bibPath = tempDir.resolve("test.bib");
        summariesStorage = new SummariesStorage(mvStore);
    }

    private void reopen() {
        mvStore.close();
        setUp();
    }

    @AfterEach
    void tearDown() {
        mvStore.close();
    }

    @Test
    void set() {
        summariesStorage.set(bibPath, "citationKey", new SummariesStorage.SummarizationRecord(LocalDateTime.now(), AiProvider.OPEN_AI, "model", "contents"));
        reopen();
        assertEquals(Optional.of("contents"), summariesStorage.get(bibPath, "citationKey").map(SummariesStorage.SummarizationRecord::content));
    }

    @Test
    void clear() {
        summariesStorage.set(bibPath, "citationKey", new SummariesStorage.SummarizationRecord(LocalDateTime.now(), AiProvider.OPEN_AI, "model", "contents"));
        reopen();
        summariesStorage.clear(bibPath, "citationKey");
        reopen();
        assertEquals(Optional.empty(), summariesStorage.get(bibPath, "citationKey"));
    }
}
