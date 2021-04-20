package org.jabref.gui.collab;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;
import org.junit.jupiter.api.Test;
import org.reactfx.Change;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
