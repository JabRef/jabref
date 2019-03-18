package org.jabref.logic.autosaveandbackup;

import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AutosaveManagerTest
{
    @Test
    public void givenDefaultBibDatabaseContextWhenCreateNewAutosaveManageReturnsAutosaveManager()
    {
        assertNotNull(AutosaveManager.start(new BibDatabaseContext()));
    }
}
