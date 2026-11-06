package org.jabref.logic.shared.prefs;

import java.util.List;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.jabref.logic.shared.DBMSConnectionProperties;
import org.jabref.logic.shared.DBMSConnectionPropertiesBuilder;
import org.jabref.logic.shared.DBMSType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Works on a node of its own, so neither the developer's stored connections nor the system keyring are touched.
class SharedDatabasePreferencesTest {

    private static final String ID = "stored";
    private static final String OTHER_ID = "other";

    private Preferences connectionsRoot;
    private SharedDatabasePreferences preferences;

    @BeforeEach
    void setUp() {
        connectionsRoot = Preferences.userRoot().node("/org/jabref-shared-test-" + System.nanoTime());
        preferences = storeFieldConnection(ID, "localhost", "5432", "jabref", "alice");
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        connectionsRoot.removeNode();
    }

    private SharedDatabasePreferences storeFieldConnection(String id, String host, String port, String database, String user) {
        SharedDatabasePreferences stored = new SharedDatabasePreferences(connectionsRoot, id);
        stored.setType(DBMSType.POSTGRESQL.toString());
        stored.setHost(host);
        stored.setPort(port);
        stored.setName(database);
        stored.setUser(user);
        return stored;
    }

    private SharedDatabasePreferences storeExpertConnection(String id, String jdbcUrl) {
        SharedDatabasePreferences stored = new SharedDatabasePreferences(connectionsRoot, id);
        stored.setUser("alice");
        stored.setExpertMode(true);
        stored.setJdbcUrl(jdbcUrl);
        return stored;
    }

    private DBMSConnectionProperties connectionProperties(String host, int port, String database, String user) {
        return new DBMSConnectionPropertiesBuilder()
                .setType(DBMSType.POSTGRESQL)
                .setHost(host)
                .setPort(port)
                .setDatabase(database)
                .setUser(user)
                .setPassword("secret")
                .setAllowPublicKeyRetrieval(true)
                .createDBMSConnectionProperties();
    }

    private DBMSConnectionProperties expertConnectionProperties(String jdbcUrl) {
        return new DBMSConnectionPropertiesBuilder()
                .setType(DBMSType.POSTGRESQL)
                .setUser("alice")
                .setPassword("secret")
                .setExpertMode(true)
                .setJdbcUrl(jdbcUrl)
                .createDBMSConnectionProperties();
    }

    // [utest->req~shared-database.saved-connections-offered~1]
    @Test
    void savedIdsContainStoredConnectionButNotTheLastUsedOne() {
        new SharedDatabasePreferences(connectionsRoot, "default").setHost("localhost");

        assertEquals(List.of(ID), SharedDatabasePreferences.listSavedIds(connectionsRoot));
    }

    @Test
    void nodeWithoutConnectionIsNotListed() {
        new SharedDatabasePreferences(connectionsRoot, OTHER_ID).getHost();

        assertEquals(List.of(ID), SharedDatabasePreferences.listSavedIds(connectionsRoot));
    }

    @Test
    void expertModeConnectionIsListed() {
        storeExpertConnection(OTHER_ID, "jdbc:postgresql://localhost:5432/jabref");

        assertEquals(List.of(OTHER_ID, ID), SharedDatabasePreferences.listSavedIds(connectionsRoot).stream().sorted().toList());
    }

    // [utest->req~shared-database.saved-connections-unique~1]
    @Test
    void duplicateOfTheSameDatabaseIsListedOnce() {
        storeFieldConnection(OTHER_ID, "LOCALHOST", "5432", "jabref", "alice");

        assertEquals(1, SharedDatabasePreferences.listSavedIds(connectionsRoot).size());
    }

    // [utest->req~shared-database.saved-connections-deletable~1]
    @Test
    void idsOfSameDatabaseIncludeDuplicates() {
        storeFieldConnection(OTHER_ID, "localhost", "5432", "jabref", "alice");
        storeFieldConnection("unrelated", "localhost", "5432", "other", "alice");

        assertEquals(List.of(OTHER_ID, ID), preferences.listIdsOfSameDatabase().stream().sorted().toList());
    }

    // [utest->req~shared-database.saved-connections-deletable~1]
    @Test
    void removedConnectionIsNotListed() throws BackingStoreException {
        preferences.removeSettings();

        assertEquals(List.of(), SharedDatabasePreferences.listSavedIds(connectionsRoot));
    }

    // [utest->req~shared-database.saved-connections-unique~1]
    @Test
    void connectionOfTheStoredDatabaseIsFound() {
        assertEquals(Optional.of(ID), SharedDatabasePreferences.findSavedId(connectionsRoot, connectionProperties("localhost", 5432, "jabref", "alice")));
    }

    /// The dialog sets driver options the stored connection does not carry; they must not make the database look new.
    @Test
    void connectionIsFoundAlthoughDriverOptionsDiffer() {
        preferences.setUseSSL(true);

        assertEquals(Optional.of(ID), SharedDatabasePreferences.findSavedId(connectionsRoot, connectionProperties("LOCALHOST", 5432, "jabref", "alice")));
    }

    @Test
    void connectionOnAnotherPortIsNotFound() {
        assertEquals(Optional.empty(), SharedDatabasePreferences.findSavedId(connectionsRoot, connectionProperties("localhost", 5433, "jabref", "alice")));
    }

    @Test
    void connectionOfAnotherDatabaseOnTheSameServerIsNotFound() {
        assertEquals(Optional.empty(), SharedDatabasePreferences.findSavedId(connectionsRoot, connectionProperties("localhost", 5432, "other", "alice")));
    }

    @Test
    void connectionOfAnotherUserIsNotFound() {
        assertEquals(Optional.empty(), SharedDatabasePreferences.findSavedId(connectionsRoot, connectionProperties("localhost", 5432, "jabref", "bob")));
    }

    @Test
    void expertModeConnectionIsFoundIgnoringSurroundingWhitespace() {
        storeExpertConnection(OTHER_ID, "jdbc:postgresql://db.example.org:5432/jabref ");

        assertEquals(Optional.of(OTHER_ID), SharedDatabasePreferences.findSavedId(connectionsRoot, expertConnectionProperties("jdbc:postgresql://db.example.org:5432/jabref")));
    }

    @Test
    void expertModeConnectionIsNotConfusedWithTheSameDatabaseEnteredAsFields() {
        storeExpertConnection(OTHER_ID, "jdbc:postgresql://localhost:5432/jabref");

        assertEquals(Optional.of(ID), SharedDatabasePreferences.findSavedId(connectionsRoot, connectionProperties("localhost", 5432, "jabref", "alice")));
    }

    @Test
    void displayLabelShowsTheConnectionDetails() {
        assertEquals("alice@localhost:5432/jabref", preferences.getDisplayLabel());
    }
}
