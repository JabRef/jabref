package org.jabref.logic.preferences;

import org.jabref.logic.InternalPreferences;
import org.jabref.model.metadata.UserHostInfo;

import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Tests the migration of keyring secrets from the legacy encrypted location to the new plain-text location,
/// using the proxy password as the example secret.
@NullMarked
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("Preferences")
class KeyringMigrationTest {
    private static final String PROXY_PERSIST_PASSWORD = "persistPassword";
    private static final String NEW_SERVICE = "org.jabref.proxy";
    private static final String LEGACY_SERVICE = "org.jabref";
    private static final String ACCOUNT = "proxy";

    // Ciphertext of "Password" encrypted with the key "someKey" by an older JabRef version (see PasswordTest)
    private static final String LEGACY_CIPHERTEXT = "xvrYKSvZj1U33vnvKk6b/A==";
    private static final String LEGACY_KEY = "someKey";
    private static final String PLAIN_TEXT = "Password";

    private final Keyring keyring = mock(Keyring.class);
    private boolean hadPersistPasswordValue;
    private boolean persistPasswordValue;

    @BeforeEach
    void setUp() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        hadPersistPasswordValue = preferences.hasKey(PROXY_PERSIST_PASSWORD);
        persistPasswordValue = preferences.getBoolean(PROXY_PERSIST_PASSWORD, false);
        preferences.putBoolean(PROXY_PERSIST_PASSWORD, true);
    }

    @AfterEach
    void tearDown() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        if (hadPersistPasswordValue) {
            preferences.putBoolean(PROXY_PERSIST_PASSWORD, persistPasswordValue);
        } else {
            preferences.remove(PROXY_PERSIST_PASSWORD);
        }
    }

    @Test
    void readsValueFromNewLocation() throws PasswordAccessException {
        when(keyring.getPassword(NEW_SERVICE, ACCOUNT)).thenReturn(PLAIN_TEXT);

        try (MockedStatic<Keyring> keyringFactory = Mockito.mockStatic(Keyring.class)) {
            keyringFactory.when(Keyring::create).thenReturn(keyring);

            assertEquals(PLAIN_TEXT, createPreferences(LEGACY_KEY).getProxyPreferences().getPassword());
            verify(keyring, never()).getPassword(LEGACY_SERVICE, ACCOUNT);
            verify(keyring, never()).setPassword(anyString(), anyString(), anyString());
        }
    }

    @Test
    void migratesLegacyValueToNewLocation() throws PasswordAccessException {
        when(keyring.getPassword(NEW_SERVICE, ACCOUNT)).thenThrow(new PasswordAccessException("Not found"));
        when(keyring.getPassword(LEGACY_SERVICE, ACCOUNT)).thenReturn(LEGACY_CIPHERTEXT);

        try (MockedStatic<Keyring> keyringFactory = Mockito.mockStatic(Keyring.class)) {
            keyringFactory.when(Keyring::create).thenReturn(keyring);

            assertEquals(PLAIN_TEXT, createPreferences(LEGACY_KEY).getProxyPreferences().getPassword());
            verify(keyring).setPassword(NEW_SERVICE, ACCOUNT, PLAIN_TEXT);
            verify(keyring).deletePassword(LEGACY_SERVICE, ACCOUNT);
        }
    }

    @Test
    void returnsEmptyWhenNeitherLocationHasValue() throws PasswordAccessException {
        when(keyring.getPassword(NEW_SERVICE, ACCOUNT)).thenThrow(new PasswordAccessException("Not found"));
        when(keyring.getPassword(LEGACY_SERVICE, ACCOUNT)).thenThrow(new PasswordAccessException("Not found"));

        try (MockedStatic<Keyring> keyringFactory = Mockito.mockStatic(Keyring.class)) {
            keyringFactory.when(Keyring::create).thenReturn(keyring);

            assertEquals("", createPreferences(LEGACY_KEY).getProxyPreferences().getPassword());
            verify(keyring, never()).setPassword(anyString(), anyString(), anyString());
            verify(keyring, never()).deletePassword(anyString(), anyString());
        }
    }

    @Test
    void discardsLegacyValueThatCannotBeDecrypted() throws PasswordAccessException {
        when(keyring.getPassword(NEW_SERVICE, ACCOUNT)).thenThrow(new PasswordAccessException("Not found"));
        when(keyring.getPassword(LEGACY_SERVICE, ACCOUNT)).thenReturn(LEGACY_CIPHERTEXT);

        try (MockedStatic<Keyring> keyringFactory = Mockito.mockStatic(Keyring.class)) {
            keyringFactory.when(Keyring::create).thenReturn(keyring);

            // A different key, as after the Owner setting or the computer name changed
            assertEquals("", createPreferences("otherKey").getProxyPreferences().getPassword());
            verify(keyring, never()).setPassword(anyString(), anyString(), anyString());
            verify(keyring).deletePassword(LEGACY_SERVICE, ACCOUNT);
        }
    }

    @Test
    void clearingValueDeletesBothLocations() throws PasswordAccessException {
        when(keyring.getPassword(NEW_SERVICE, ACCOUNT)).thenReturn(PLAIN_TEXT);

        try (MockedStatic<Keyring> keyringFactory = Mockito.mockStatic(Keyring.class)) {
            keyringFactory.when(Keyring::create).thenReturn(keyring);

            createPreferences(LEGACY_KEY).getProxyPreferences().setPassword("");

            verify(keyring).deletePassword(NEW_SERVICE, ACCOUNT);
            verify(keyring).deletePassword(LEGACY_SERVICE, ACCOUNT);
            verify(keyring, never()).setPassword(anyString(), anyString(), anyString());
        }
    }

    /// Creates preferences whose legacy decryption key is `key`, so the test does not depend on the computer name.
    private JabRefCliPreferences createPreferences(String key) {
        InternalPreferences internalPreferences = mock(InternalPreferences.class);
        when(internalPreferences.getUserHostInfo()).thenReturn(new UserHostInfo(key, ""));

        JabRefCliPreferences preferences = spy(new JabRefCliPreferences());
        doReturn(internalPreferences).when(preferences).getInternalPreferences();
        return preferences;
    }
}
