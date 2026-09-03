package org.jabref.gui.autosaveandbackup;

import org.jabref.logic.util.CoarseChangeFilter;
import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AutosaveManagerTest {

    // [utest->req~jabgui.autosaveandbackup.autosave-listens~1]
    @Test
    void startListensToChangesAndShutdownStops() {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        CoarseChangeFilter coarseChangeFilter = mock(CoarseChangeFilter.class);

        AutosaveManager autosaveManager = AutosaveManager.start(databaseContext, coarseChangeFilter);
        verify(coarseChangeFilter).registerListener(autosaveManager);

        AutosaveManager.shutdown(databaseContext);
        verify(coarseChangeFilter).unregisterListener(autosaveManager);
    }
}
