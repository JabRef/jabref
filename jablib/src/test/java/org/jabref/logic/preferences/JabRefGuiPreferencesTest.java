package org.jabref.logic.preferences;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import org.jabref.logic.net.CiteDrivePreferences;
import org.jabref.logic.net.ssl.TrustStoreManager;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;

import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ResourceLock("Preferences")
@ResourceLock("Localization.lang")
class JabRefGuiPreferencesTest {
    private static Stream<Arguments> provideTestData() {
        return Stream.of(
                Arguments.of(List.of("A", "B", "C", "D"), "A;B;C;D"),
                Arguments.of(List.of("A", "B", "C", ""), "A;B;C;"),
                Arguments.of(List.of("a;b", "C:\\Tools\\app.exe"), "a\\;b;C:\\\\Tools\\\\app.exe"),
                Arguments.of(List.of("a\\", "b"), "a\\\\;b")
        );
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    void convertStringToList(List<String> sampleList, String sampleString) {
        assertEquals(sampleList, JabRefCliPreferences.convertStringToList(sampleString));
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    void convertListToString(List<String> sampleList, String sampleString) {
        assertEquals(sampleString, JabRefCliPreferences.convertListToString(sampleList));
    }

    @Test
    void convertStringToListPreservesUnescapedBackslashes() {
        assertEquals(List.of("C:\\Tools\\app.exe"), JabRefCliPreferences.convertStringToList("C:\\Tools\\app.exe"));
    }

    @Test
    void suppliedPreferencesNodesKeepValuesAndChildNodesSeparate() throws BackingStoreException {
        Preferences root = Preferences.userRoot().node("/org/jabref-preferences-test-" + UUID.randomUUID());
        try {
            JabRefCliPreferences first = new JabRefCliPreferences(root.node("first"));
            JabRefCliPreferences second = new JabRefCliPreferences(root.node("second"));
            first.put("isolatedValue", "first");
            second.put("isolatedValue", "second");
            first.addDeclinedCustomEntryTypes(Set.of("article"));

            assertEquals("first", new JabRefCliPreferences(root.node("first")).get("isolatedValue", ""));
            assertEquals("second", second.get("isolatedValue", ""));
            assertEquals(Set.of("article"), first.getDeclinedCustomEntryTypes());
            assertEquals(Set.of(), second.getDeclinedCustomEntryTypes());
        } finally {
            root.removeNode();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void clearingInjectedPreferencesLeavesGlobalStoresIntact(boolean initializeSecrets) throws BackingStoreException, PasswordAccessException {
        Preferences node = Preferences.userRoot().node("/org/jabref-reset-test-" + UUID.randomUUID());
        Map<List<String>, String> secrets = new HashMap<>();
        secrets.put(List.of("org.jabref.proxy", "proxy"), "existing-proxy-password");
        secrets.put(List.of("org.jabref.github", "github"), "existing-github-pat");
        secrets.put(List.of("org.jabref.apikeys", "test-reset"), "existing-fetcher-key");
        secrets.put(List.of("org.jabref", "citedrive"), "existing-refresh-token");
        Keyring keyring = inMemoryKeyring(secrets);
        try (MockedStatic<Keyring> keyringFactory = Mockito.mockStatic(Keyring.class);
             MockedConstruction<TrustStoreManager> trustStores = Mockito.mockConstruction(TrustStoreManager.class);
             MockedConstruction<SharedDatabasePreferences> sharedDatabases = Mockito.mockConstruction(SharedDatabasePreferences.class)) {
            keyringFactory.when(Keyring::create).thenReturn(keyring);
            JabRefCliPreferences preferences = new JabRefCliPreferences(node);
            preferences.put("isolatedValue", "changed");
            if (initializeSecrets) {
                initializePersistedSecrets(preferences);
            }
            Map<List<String>, String> beforeReset = Map.copyOf(secrets);
            Mockito.clearInvocations(keyring);

            preferences.clear();

            assertEquals(false, preferences.hasKey("isolatedValue"));
            assertEquals(false, preferences.getProxyPreferences().shouldPersistPassword());
            assertEquals(false, preferences.getGitPreferences().getPersistPat());
            assertEquals(CiteDrivePreferences.getDefault().shouldPersistRefreshToken(), preferences.getCiteDrivePreferences().shouldPersistRefreshToken());
            assertEquals(beforeReset, secrets);
            assertEquals(List.of(), trustStores.constructed());
            assertEquals(List.of(), sharedDatabases.constructed());
            verifyNoInteractions(keyring);
        } finally {
            node.removeNode();
        }
    }

    @Test
    void clearingDefaultPreferencesStillClearsGlobalStores() throws BackingStoreException, PasswordAccessException {
        Preferences node = Preferences.userRoot().node("/org/jabref-default-reset-test-" + UUID.randomUUID());
        Preferences userRoot = mock(Preferences.class);
        when(userRoot.node("/org/jabref")).thenReturn(node);
        Map<List<String>, String> secrets = new HashMap<>();
        Keyring keyring = inMemoryKeyring(secrets);
        try (MockedStatic<Preferences> preferencesFactory = Mockito.mockStatic(Preferences.class);
             MockedStatic<Keyring> keyringFactory = Mockito.mockStatic(Keyring.class);
             MockedConstruction<TrustStoreManager> trustStores = Mockito.mockConstruction(TrustStoreManager.class);
             MockedConstruction<SharedDatabasePreferences> sharedDatabases = Mockito.mockConstruction(SharedDatabasePreferences.class)) {
            preferencesFactory.when(Preferences::userRoot).thenReturn(userRoot);
            keyringFactory.when(Keyring::create).thenReturn(keyring);
            JabRefCliPreferences preferences = new JabRefCliPreferences();
            initializePersistedSecrets(preferences);
            assertEquals(4, secrets.size());
            assertEquals(List.of("test-reset"), preferences.getStringList("fetcherCustomKeyNames"));

            preferences.clear();

            verify(keyring).deletePassword("org.jabref.apikeys", "test-reset");
            assertEquals(Map.of(), secrets);
            assertEquals(1, trustStores.constructed().size());
            verify(trustStores.constructed().getFirst()).clearCustomCertificates();
            assertEquals(1, sharedDatabases.constructed().size());
            verify(sharedDatabases.constructed().getFirst()).clearAllConnections();
        } finally {
            node.removeNode();
        }
    }

    private static void initializePersistedSecrets(JabRefCliPreferences preferences) {
        preferences.getProxyPreferences().persistPasswordProperty().set(true);
        preferences.getProxyPreferences().passwordProperty().set("proxy-password");
        preferences.getGitPreferences().rememberPatProperty().set(true);
        preferences.getGitPreferences().patProperty().set("github-pat");
        preferences.getImporterPreferences().getApiKeys().clear();
        preferences.getImporterPreferences().setPersistCustomKeys(true);
        preferences.getImporterPreferences().getApiKeys().add(new FetcherApiKey("test-reset", true, "fetcher-key"));
        preferences.getCiteDrivePreferences().persistRefreshTokenProperty().set(true);
        preferences.getCiteDrivePreferences().setRefreshToken(new RefreshToken("refresh-token"));
    }

    private static Keyring inMemoryKeyring(Map<List<String>, String> secrets) throws PasswordAccessException {
        Keyring keyring = mock(Keyring.class);
        when(keyring.getPassword(anyString(), anyString())).thenAnswer(invocation ->
                secrets.getOrDefault(List.of(invocation.getArgument(0), invocation.getArgument(1)), ""));
        doAnswer(invocation -> {
            secrets.put(List.of(invocation.getArgument(0), invocation.getArgument(1)), invocation.getArgument(2));
            return null;
        }).when(keyring).setPassword(anyString(), anyString(), anyString());
        doAnswer(invocation -> {
            secrets.remove(List.of(invocation.getArgument(0), invocation.getArgument(1)));
            return null;
        }).when(keyring).deletePassword(anyString(), anyString());
        return keyring;
    }
}
