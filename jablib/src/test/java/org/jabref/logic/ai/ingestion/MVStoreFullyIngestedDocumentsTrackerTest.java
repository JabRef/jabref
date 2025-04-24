package org.jabref.logic.ai.ingestion;

import java.nio.file.Path;

import org.jabref.logic.ai.ingestion.storages.MVStoreFullyIngestedDocumentsTracker;
import org.jabref.logic.util.NotificationService;

import static org.mockito.Mockito.mock;

class MVStoreFullyIngestedDocumentsTrackerTest extends FullyIngestedDocumentsTrackerTest {
    @Override
    FullyIngestedDocumentsTracker makeTracker(Path path) {
        return new MVStoreFullyIngestedDocumentsTracker(path, mock(NotificationService.class));
    }

    @Override
    void close(FullyIngestedDocumentsTracker tracker) {
        ((MVStoreFullyIngestedDocumentsTracker) tracker).close();
    }
}
