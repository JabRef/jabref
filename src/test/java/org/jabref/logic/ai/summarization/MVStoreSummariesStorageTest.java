package org.jabref.logic.ai.summarization;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.ai.summarization.storages.MVStoreSummariesStorage;
import org.jabref.preferences.ai.AiProvider;

import org.h2.mvstore.MVStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class MVStoreSummariesStorageTest {

    @TempDir Path tempDir;

    private MVStoreSummariesStorage summariesStorage;
    private Path bibPath;

    @BeforeEach
    void setUp() {
        bibPath = tempDir.resolve("test.bib");
        summariesStorage = new MVStoreSummariesStorage(tempDir.resolve("test.mv"), mock(DialogService.class));
    }

    private void reopen() {
        summariesStorage.close();
        setUp();
    }

    @AfterEach
    void tearDown() {
        summariesStorage.close();
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
