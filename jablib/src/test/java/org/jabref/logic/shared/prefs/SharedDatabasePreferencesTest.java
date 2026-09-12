package org.jabref.logic.shared.prefs;

import java.util.List;
import java.util.Optional;
import java.util.prefs.BackingStoreException;

import org.jabref.logic.shared.DBMSConnectionProperties;
import org.jabref.logic.shared.DBMSConnectionPropertiesBuilder;
import org.jabref.logic.shared.DBMSType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SharedDatabasePreferencesTest {

    private static final String ID = "shared-database-preferences-test";
    private static final String OTHER_ID = "shared-database-preferences-test-other";

    private SharedDatabasePreferences preferences;

    @BeforeEach
    void setUp() {
        preferences = new SharedDatabasePreferences(ID);
        preferences.setType(DBMSType.POSTGRESQL.toString());
        preferences.setHost("localhost");
        preferences.setPort("5432");
        preferences.setName("jabref");
        preferences.setUser("alice");
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        for (String sharedDatabaseId : List.of(ID, OTHER_ID)) {
            if (SharedDatabasePreferences.listSavedIds().contains(sharedDatabaseId)) {
                new SharedDatabasePreferences(sharedDatabaseId).remove();
            }
        }
    }

    /// Only the test's own identifiers are asserted on: the preferences tree is shared with the developer's JabRef.
    private List<String> savedTestIds() {
        return SharedDatabasePreferences.listSavedIds().stream()
                                        .filter(sharedDatabaseId -> List.of(ID, OTHER_ID, "default").contains(sharedDatabaseId))
                                        .toList();
    }

    private DBMSConnectionProperties connectionProperties(String host, String database, String user) {
        return new DBMSConnectionPropertiesBuilder()
                .setType(DBMSType.POSTGRESQL)
                .setHost(host)
                .setPort(5432)
                .setDatabase(database)
                .setUser(user)
                .setPassword("secret")
                .setAllowPublicKeyRetrieval(true)
                .createDBMSConnectionProperties();
    }

    @Test
    void savedIdsContainStoredConnectionButNotTheLastUsedOne() {
        new SharedDatabasePreferences().setHost("localhost");

        assertEquals(List.of(ID), savedTestIds());
    }

    @Test
    void removeDropsTheStoredConnection() throws BackingStoreException {
        preferences.remove();

        assertEquals(List.of(), savedTestIds());
    }

    @Test
    void connectionOfTheStoredDatabaseIsFound() {
        assertEquals(Optional.of(ID), SharedDatabasePreferences.findSavedId(connectionProperties("localhost", "jabref", "alice")));
    }

    /// The dialog sets driver options the stored connection does not carry; they must not make the database look new.
    @Test
    void connectionIsFoundAlthoughDriverOptionsDiffer() {
        preferences.setUseSSL(true);

        assertEquals(Optional.of(ID), SharedDatabasePreferences.findSavedId(connectionProperties("LOCALHOST", "jabref", "alice")));
    }

    @Test
    void connectionOfAnotherDatabaseOnTheSameServerIsNotFound() {
        assertEquals(Optional.empty(), SharedDatabasePreferences.findSavedId(connectionProperties("localhost", "other", "alice")));
    }

    @Test
    void connectionOfAnotherUserIsNotFound() {
        assertEquals(Optional.empty(), SharedDatabasePreferences.findSavedId(connectionProperties("localhost", "jabref", "bob")));
    }

    @Test
    void expertModeConnectionIsNotConfusedWithTheSameDatabaseEnteredAsFields() {
        SharedDatabasePreferences expertPreferences = new SharedDatabasePreferences(OTHER_ID);
        expertPreferences.setUser("alice");
        expertPreferences.setExpertMode(true);
        expertPreferences.setJdbcUrl("jdbc:postgresql://localhost:5432/jabref");

        assertEquals(Optional.of(ID), SharedDatabasePreferences.findSavedId(connectionProperties("localhost", "jabref", "alice")));
    }
}
