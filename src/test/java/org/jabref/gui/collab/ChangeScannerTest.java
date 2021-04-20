package org.jabref.gui.collab;

import java.util.Collections;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChangeScannerTest {

    @Test
    void noDatabasePathReturnsEmptyList() {
        PreferencesService preferencesService = mock(PreferencesService.class);
        BibDatabaseContext noDatabase = mock(BibDatabaseContext.class);

        when(noDatabase.getDatabasePath()).thenReturn(Optional.empty());

        ChangeScanner changeScanner = new ChangeScanner(noDatabase, preferencesService);

        assertEquals(changeScanner.scanForChanges(), Collections.emptyList());
    }
}
