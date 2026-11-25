package org.jabref.logic.shared.prefs;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.jabref.logic.shared.DBMSConnectionProperties;
import org.jabref.logic.shared.DBMSConnectionPropertiesBuilder;
import org.jabref.logic.shared.DBMSType;

import com.github.javakeyring.BackendNotSupportedException;
import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/// Unlike the tests of classes that use [SharedDatabasePreferences], this one cannot mock it: it is the class under test.
/// Its lookups (finding, listing, and clearing stored connections) read the connections that the helpers store in the
/// preferences tree, so the stored connections have to be real as well.
///
/// What the class depends on is replaced instead: it works on a preferences node and a keyring of its own, so neither
/// the developer's stored connections nor their passwords are touched.
class SharedDatabasePreferencesTest {

    private static final String ID = "stored";
    private static final String OTHER_ID = "other";

    private final Keyring keyring = mock(Keyring.class);
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
        SharedDatabasePreferences stored = new SharedDatabasePreferences(connectionsRoot, id, () -> keyring);
        stored.setType(DBMSType.POSTGRESQL.toString());
        stored.setHost(host);
        stored.setPort(port);
        stored.setName(database);
        stored.setUser(user);
        return stored;
    }

    private SharedDatabasePreferences storeExpertConnection(String id, String jdbcUrl) {
        SharedDatabasePreferences stored = new SharedDatabasePreferences(connectionsRoot, id, () -> keyring);
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
        new SharedDatabasePreferences(connectionsRoot, "default", () -> keyring).setHost("localhost");

        assertEquals(List.of(ID), preferences.listSavedIds());
    }

    @Test
    void nodeWithoutConnectionIsNotListed() {
        new SharedDatabasePreferences(connectionsRoot, OTHER_ID, () -> keyring).getHost();

        assertEquals(List.of(ID), preferences.listSavedIds());
    }

    @Test
    void expertModeConnectionIsListed() {
        storeExpertConnection(OTHER_ID, "jdbc:postgresql://localhost:5432/jabref");

        assertEquals(List.of(OTHER_ID, ID), preferences.listSavedIds().stream().sorted().toList());
    }

    // [utest->req~shared-database.saved-connections-unique~1]
    @Test
    void duplicateOfTheSameDatabaseIsListedOnce() {
        storeFieldConnection(OTHER_ID, "LOCALHOST", "5432", "jabref", "alice");

        assertEquals(1, preferences.listSavedIds().size());
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
        preferences.remove();

        assertEquals(List.of(), preferences.listSavedIds());
    }

    // [utest->req~shared-database.saved-connections-deletable~1]
    @Test
    void removingConnectionDeletesItsPassword() throws BackingStoreException, PasswordAccessException {
        preferences.remove();

        verify(keyring).deletePassword("org.jabref.shareddatabase", ID);
    }

    // [utest->req~shared-database.saved-connections-unique~1]
    @Test
    void connectionOfTheStoredDatabaseIsFound() {
        assertEquals(Optional.of(ID), preferences.findSavedId(connectionProperties("localhost", 5432, "jabref", "alice")));
    }

    /// The dialog sets driver options the stored connection does not carry; they must not make the database look new.
    @Test
    void connectionIsFoundAlthoughDriverOptionsDiffer() {
        preferences.setUseSSL(true);

        assertEquals(Optional.of(ID), preferences.findSavedId(connectionProperties("LOCALHOST", 5432, "jabref", "alice")));
    }

    @Test
    void connectionOnAnotherPortIsNotFound() {
        assertEquals(Optional.empty(), preferences.findSavedId(connectionProperties("localhost", 5433, "jabref", "alice")));
    }

    @Test
    void connectionOfAnotherDatabaseOnTheSameServerIsNotFound() {
        assertEquals(Optional.empty(), preferences.findSavedId(connectionProperties("localhost", 5432, "other", "alice")));
    }

    @Test
    void connectionOfAnotherUserIsNotFound() {
        assertEquals(Optional.empty(), preferences.findSavedId(connectionProperties("localhost", 5432, "jabref", "bob")));
    }

    @Test
    void expertModeConnectionIsFoundIgnoringSurroundingWhitespace() {
        storeExpertConnection(OTHER_ID, "jdbc:postgresql://db.example.org:5432/jabref ");

        assertEquals(Optional.of(OTHER_ID), preferences.findSavedId(expertConnectionProperties("jdbc:postgresql://db.example.org:5432/jabref")));
    }

    @Test
    void expertModeConnectionIsNotConfusedWithTheSameDatabaseEnteredAsFields() {
        storeExpertConnection(OTHER_ID, "jdbc:postgresql://localhost:5432/jabref");

        assertEquals(Optional.of(ID), preferences.findSavedId(connectionProperties("localhost", 5432, "jabref", "alice")));
    }

    @Test
    void expertModeConnectionIsFoundWhenTheDefaultPortIsWrittenOut() {
        storeExpertConnection(OTHER_ID, "jdbc:postgresql://db.example.org/jabref");

        assertEquals(Optional.of(OTHER_ID), preferences.findSavedId(expertConnectionProperties("jdbc:postgresql://DB.example.org:5432/jabref")));
    }

    @Test
    void expertModeConnectionToAnotherDatabaseIsNotFound() {
        storeExpertConnection(OTHER_ID, "jdbc:postgresql://db.example.org/jabref");

        assertEquals(Optional.empty(), preferences.findSavedId(expertConnectionProperties("jdbc:postgresql://db.example.org/other")));
    }

    @Test
    void displayLabelShowsTheConnectionDetails() {
        assertEquals("alice@localhost:5432/jabref", preferences.getDisplayLabel());
    }

    @Test
    void displayLabelOfExpertModeConnectionHidesThePassword() {
        SharedDatabasePreferences expertPreferences = storeExpertConnection(OTHER_ID, "jdbc:postgresql://db.example.org/jabref?password=secret&user=alice");

        assertEquals("jdbc:postgresql://db.example.org:5432/jabref (alice)", expertPreferences.getDisplayLabel());
    }

    @Test
    void displayLabelOfExpertModeConnectionNamesTheUser() {
        SharedDatabasePreferences expertPreferences = storeExpertConnection(OTHER_ID, "jdbc:postgresql://db.example.org/jabref");

        assertEquals("jdbc:postgresql://db.example.org:5432/jabref (alice)", expertPreferences.getDisplayLabel());
    }

    /// Saving one library must not erase the password of another library because of the dialog's last choice
    @Test
    void connectionKeepsItsOwnRememberPasswordChoice() {
        preferences.setRememberPassword(true);
        new SharedDatabasePreferences(connectionsRoot, "default", () -> keyring).setRememberPassword(false);

        assertTrue(preferences.shouldRememberPassword());
    }

    @Test
    void newConnectionFollowsTheLastRememberPasswordChoice() {
        new SharedDatabasePreferences(connectionsRoot, "default", () -> keyring).setRememberPassword(true);

        assertTrue(preferences.shouldRememberPassword());
    }

    /// Removing its settings would leave the password behind under an identifier nothing refers to anymore
    @Test
    void clearingAllConnectionsKeepsConnectionWhosePasswordCannotBeRemoved() throws BackingStoreException {
        SharedDatabasePreferences lastUsedWithoutKeyring = new SharedDatabasePreferences(connectionsRoot, "default", () -> {
            throw new IOException("Credential store locked");
        });

        lastUsedWithoutKeyring.clearAllConnections();

        assertEquals(List.of(ID), lastUsedWithoutKeyring.listSavedIds());
    }

    /// Without a keyring backend no password can have been stored, so none can be left behind
    @Test
    void clearingAllConnectionsWithoutKeyringBackendRemovesThem() throws BackingStoreException {
        SharedDatabasePreferences lastUsedWithoutBackend = new SharedDatabasePreferences(connectionsRoot, "default", () -> {
            throw new BackendNotSupportedException("No keyring backend");
        });

        lastUsedWithoutBackend.clearAllConnections();

        assertEquals(List.of(), lastUsedWithoutBackend.listSavedIds());
    }

    @Test
    void clearingAllConnectionsRemovesEveryStoredConnection() throws BackingStoreException {
        storeFieldConnection(OTHER_ID, "localhost", "5432", "other", "alice");
        SharedDatabasePreferences lastUsed = new SharedDatabasePreferences(connectionsRoot, "default", () -> keyring);
        lastUsed.setHost("localhost");

        lastUsed.clearAllConnections();

        assertEquals(List.of(), lastUsed.listSavedIds());
        assertEquals(Optional.empty(), lastUsed.getHost());
    }
}
