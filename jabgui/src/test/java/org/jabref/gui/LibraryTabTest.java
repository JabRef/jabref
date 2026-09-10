package org.jabref.gui;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.shared.DatabaseSynchronizer;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
@ExtendWith(JavaFxExtension.class)
class LibraryTabTest {

    // [utest->req~ux.tabs.library-kind-icon~1]
    @ParameterizedTest
    @CsvSource({
            "LOCAL, BIBTEX, BIBTEX_LIBRARY",
            "LOCAL, BIBLATEX, BIBLATEX_LIBRARY",
            "SHARED, BIBTEX, SHARED_DATABASE_LIBRARY",
            "SHARED, BIBLATEX, SHARED_DATABASE_LIBRARY"
    })
    void tabIconTellsLibraryKind(DatabaseLocation location, BibDatabaseMode mode, IconTheme.JabRefIcons expected) {
        assertEquals(expected, LibraryTab.tabIcon(location, mode));
    }

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
