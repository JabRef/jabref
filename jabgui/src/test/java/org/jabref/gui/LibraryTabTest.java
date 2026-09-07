package org.jabref.gui;

import org.jabref.logic.shared.DatabaseSynchronizer;
import org.jabref.model.database.BibDatabaseContext;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
@ExtendWith(ApplicationExtension.class)
class LibraryTabTest {

    @Test
    void closesSharedDatabaseContextWhenConnectionCompletesAfterCancellation() throws Exception {
        BibDatabaseContext connectedContext = mock(BibDatabaseContext.class);
        DatabaseSynchronizer synchronizer = mock(DatabaseSynchronizer.class);
        when(connectedContext.getDBMSSynchronizer()).thenReturn(synchronizer);

        LibraryTab.SharedDatabaseLoadingCallbacks callbacks = new LibraryTab.SharedDatabaseLoadingCallbacks(
                mock(LibraryTab.class),
                (_, _) -> {
                },
                _ -> {
                });
        LibraryTab.SharedDatabaseLoadingTask task = new LibraryTab.SharedDatabaseLoadingTask(() -> connectedContext, callbacks);

        task.cancel();
        task.call();

        verify(connectedContext).convertToLocalDatabase();
        verify(synchronizer).closeSharedDatabase();
        verify(connectedContext).clearDBMSSynchronizer();
    }
}
