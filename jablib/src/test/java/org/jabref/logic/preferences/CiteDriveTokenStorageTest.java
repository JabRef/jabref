package org.jabref.logic.preferences;

import java.util.Optional;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.jabref.logic.JabRefException;
import org.jabref.logic.net.CiteDrivePreferences;
import org.jabref.logic.preferences.CliPreferences.CredentialPersistence;

import com.github.javakeyring.Keyring;
import com.github.javakeyring.KeyringStorageType;
import com.github.javakeyring.PasswordAccessException;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
@ResourceLock("Preferences")
        // [utest->req~citedrive.credentials.server~1]
class CiteDriveTokenStorageTest {

    @Test
    void preparedSavePublishesCiteDrivePersistenceAfterTheWrite() throws BackingStoreException, JabRefException, PasswordAccessException {
        Preferences node = Preferences.userRoot().node("/org/jabref-citedrive-test-" + UUID.randomUUID());
        try (MockedStatic<Keyring> factory = mockStatic(Keyring.class)) {
            Keyring keyring = mock(Keyring.class);
            factory.when(Keyring::create).thenReturn(keyring);
            JabRefCliPreferences preferences = new JabRefCliPreferences(node);
            preferences.putBoolean("citeDrivePersistToken", false);
            CiteDrivePreferences citeDrive = preferences.getCiteDrivePreferences();
            RefreshToken token = new RefreshToken("test-token");
            citeDrive.setRefreshToken(token);
            CliPreferences.CredentialSave save = preferences.prepareCiteDriveTokenSave(citeDrive.getApiBaseUrl(), CredentialPersistence.SAVE).orElseThrow();

            CliPreferences.CredentialCommit commit = save.writeKeyring();
            assertEquals(false, citeDrive.shouldPersistRefreshToken());
            commit.apply();

            assertEquals(true, citeDrive.shouldPersistRefreshToken());
            verify(keyring).setPassword("org.jabref", "citedrive",
                    JabRefCliPreferences.serializeCiteDriveToken(token, citeDrive.getApiBaseUrl()));
        } finally {
            node.removeNode();
        }
    }

    @Test
    void refreshingCiteDriveTokenBeforeCommitRetriesTheKeyringWrite() throws BackingStoreException, JabRefException, PasswordAccessException {
        Preferences node = Preferences.userRoot().node("/org/jabref-citedrive-test-" + UUID.randomUUID());
        try (MockedStatic<Keyring> factory = mockStatic(Keyring.class)) {
            Keyring keyring = mock(Keyring.class);
            factory.when(Keyring::create).thenReturn(keyring);
            JabRefCliPreferences preferences = new JabRefCliPreferences(node);
            preferences.putBoolean("citeDrivePersistToken", false);
            CiteDrivePreferences citeDrive = preferences.getCiteDrivePreferences();
            citeDrive.setRefreshToken(new RefreshToken("previous"));
            CliPreferences.CredentialSave save = preferences.prepareCiteDriveTokenSave(citeDrive.getApiBaseUrl(), CredentialPersistence.SAVE).orElseThrow();
            CliPreferences.CredentialCommit commit = save.writeKeyring();

            RefreshToken refreshed = new RefreshToken("refreshed");
            citeDrive.setRefreshToken(refreshed);
            CliPreferences.CredentialSave retry = commit.apply().orElseThrow();

            assertEquals(false, citeDrive.shouldPersistRefreshToken());
            assertEquals(Optional.empty(), retry.writeKeyring().apply());
            assertEquals(true, citeDrive.shouldPersistRefreshToken());
            verify(keyring).setPassword("org.jabref", "citedrive",
                    JabRefCliPreferences.serializeCiteDriveToken(refreshed, citeDrive.getApiBaseUrl()));
        } finally {
            node.removeNode();
        }
    }

    @Test
    void deniedDeletionOfUnreadableCiteDriveTokenLeavesPersistenceEnabled() throws BackingStoreException, PasswordAccessException {
        Preferences node = Preferences.userRoot().node("/org/jabref-citedrive-test-" + UUID.randomUUID());
        try (MockedStatic<Keyring> factory = mockStatic(Keyring.class)) {
            Keyring keyring = mock(Keyring.class);
            factory.when(Keyring::create).thenReturn(keyring);
            when(keyring.getKeyringStorageType()).thenReturn(KeyringStorageType.OSX_KEYCHAIN);
            when(keyring.getPassword("org.jabref", "citedrive")).thenThrow(new PasswordAccessException("Denied"));
            doThrow(new PasswordAccessException("Denied")).when(keyring).deletePassword("org.jabref", "citedrive");
            JabRefCliPreferences preferences = new JabRefCliPreferences(node);
            preferences.putBoolean("citeDrivePersistToken", true);
            CiteDrivePreferences citeDrive = preferences.getCiteDrivePreferences();
            CliPreferences.CredentialSave save = preferences.prepareCiteDriveTokenSave(citeDrive.getApiBaseUrl(), CredentialPersistence.SESSION_ONLY).orElseThrow();

            assertThrows(JabRefException.class, save::writeKeyring);
            assertEquals(true, citeDrive.shouldPersistRefreshToken());
        } finally {
            node.removeNode();
        }
    }

    @Test
    void changingCiteDriveServerClearsTheOldTokenBeforePublishingTheNewServer() throws BackingStoreException, JabRefException, PasswordAccessException {
        Preferences node = Preferences.userRoot().node("/org/jabref-citedrive-test-" + UUID.randomUUID());
        try (MockedStatic<Keyring> factory = mockStatic(Keyring.class)) {
            Keyring keyring = mock(Keyring.class);
            factory.when(Keyring::create).thenReturn(keyring);
            JabRefCliPreferences preferences = new JabRefCliPreferences(node);
            RefreshToken token = new RefreshToken("test-token");
            String previousServer = "https://original.example/";
            String newServer = "https://replacement.example/";
            preferences.put("citeDriveApiBaseUrl", previousServer);
            preferences.putBoolean("citeDrivePersistToken", true);
            when(keyring.getPassword("org.jabref", "citedrive"))
                    .thenReturn(JabRefCliPreferences.serializeCiteDriveToken(token, previousServer));
            CiteDrivePreferences citeDrive = preferences.getCiteDrivePreferences();
            CliPreferences.CredentialSave save = preferences.prepareCiteDriveTokenSave(newServer, CredentialPersistence.SAVE).orElseThrow();

            CliPreferences.CredentialCommit commit = save.writeKeyring();
            assertEquals(previousServer, citeDrive.getApiBaseUrl());
            assertEquals(token, citeDrive.getRefreshToken());
            commit.apply();

            assertEquals(newServer, citeDrive.getApiBaseUrl());
            assertEquals(Optional.empty(), Optional.ofNullable(citeDrive.getRefreshToken()));
            verify(keyring).deletePassword("org.jabref", "citedrive");
        } finally {
            node.removeNode();
        }
    }

    @Test
    void storedTokenCanOnlyBeLoadedForItsOriginalServer() {
        RefreshToken token = new RefreshToken("stored");
        String json = JabRefCliPreferences.serializeCiteDriveToken(token, "https://original.example/");

        assertEquals(token, JabRefCliPreferences.parseCiteDriveToken(json, "https://original.example/"));
        assertEquals(Optional.empty(), Optional.ofNullable(JabRefCliPreferences.parseCiteDriveToken(json, "https://other.example/")));
    }

    @Test
    void legacyTokenWithoutServerBindingRequiresAnotherLogin() {
        String json = new RefreshToken("legacy").toJSONObject().toJSONString();

        assertEquals(Optional.empty(), Optional.ofNullable(JabRefCliPreferences.parseCiteDriveToken(json, "https://original.example/")));
    }
}
