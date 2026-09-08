package org.jabref.logic.shared.prefs;

import java.util.List;
import java.util.Set;
import java.util.prefs.BackingStoreException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SharedDatabasePreferencesTest {

    private static final String ID = "shared-database-preferences-test";

    @AfterEach
    void tearDown() throws BackingStoreException {
        if (SharedDatabasePreferences.listSavedIds().contains(ID)) {
            new SharedDatabasePreferences(ID).remove();
        }
    }

    @Test
    void savedIdsContainStoredConnectionButNotTheLastUsedOne() {
        new SharedDatabasePreferences(ID).setHost("localhost");
        new SharedDatabasePreferences().setHost("localhost");

        List<String> savedIds = SharedDatabasePreferences.listSavedIds();

        assertEquals(List.of(ID), savedIds.stream().filter(id -> Set.of(ID, "default").contains(id)).toList());
    }

    @Test
    void removeDropsTheStoredConnection() throws BackingStoreException {
        new SharedDatabasePreferences(ID).setHost("localhost");

        new SharedDatabasePreferences(ID).remove();

        assertFalse(SharedDatabasePreferences.listSavedIds().contains(ID));
    }
}
