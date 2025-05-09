package org.jabref.logic.ai.summarization;

import java.nio.file.Path;

import org.jabref.logic.ai.summarization.storages.MVStoreSummariesStorage;
import org.jabref.logic.util.NotificationService;

import static org.mockito.Mockito.mock;

class MVStoreSummariesStorageTest extends SummariesStorageTest {
    @Override
    SummariesStorage makeSummariesStorage(Path path) {
        return new MVStoreSummariesStorage(path, mock(NotificationService.class));
    }

    @Override
    void close(SummariesStorage summariesStorage) {
        ((MVStoreSummariesStorage) summariesStorage).close();
    }
}
