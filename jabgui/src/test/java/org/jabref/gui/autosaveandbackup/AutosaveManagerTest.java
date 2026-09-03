package org.jabref.gui.autosaveandbackup;

import org.jabref.logic.util.CoarseChangeFilter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@NullMarked
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

    @Test
    void minorChangeMarksSavePending() {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        AutosaveManager autosaveManager = AutosaveManager.start(databaseContext, mock(CoarseChangeFilter.class));
        assertFalse(autosaveManager.isSavePending());

        // A single typed character, which the filter marks as minor
        FieldChangedEvent keystroke = new FieldChangedEvent(new BibEntry(), StandardField.TITLE, "T", "");
        keystroke.setFilteredOut(true);
        autosaveManager.listen(keystroke);

        assertTrue(autosaveManager.isSavePending());
        AutosaveManager.shutdown(databaseContext);
    }
}
