package org.jabref.logic.ai.summarization;

import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.logic.ai.summarization.storages.MVStoreSummariesStorage;

import static org.mockito.Mockito.mock;

class MVStoreSummariesStorageTest extends SummariesStorageTest {
    @Override
    SummariesStorage makeSummariesStorage(Path path) {
        return new MVStoreSummariesStorage(path, mock(DialogService.class));
    }

    @Override
    void close(SummariesStorage summariesStorage) {
        ((MVStoreSummariesStorage) summariesStorage).close();
    }
}
