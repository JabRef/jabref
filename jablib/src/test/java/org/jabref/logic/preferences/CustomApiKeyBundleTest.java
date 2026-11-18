package org.jabref.logic.preferences;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.IntStream;

import org.jabref.logic.importer.ImporterPreferences;

import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@NullMarked
@ResourceLock("Preferences")
@ResourceLock("Localization.lang")
class CustomApiKeyBundleTest {
    private final Preferences node = Preferences.userRoot().node("/org/jabref-api-key-test-" + UUID.randomUUID());
    private final Map<List<String>, String> secrets = new HashMap<>();
    private final Keyring keyring = mock(Keyring.class);
    private MockedStatic<Keyring> factory;
    private JabRefCliPreferences preferences;

    @BeforeEach
    void setUp() throws PasswordAccessException {
        when(keyring.getPassword(anyString(), anyString())).thenAnswer(invocation -> {
            List<String> slot = List.of(invocation.getArgument(0), invocation.getArgument(1));
            if (!secrets.containsKey(slot)) {
                throw new PasswordAccessException("Not found");
            }
            return secrets.get(slot);
        });
        doAnswer(invocation -> {
            secrets.put(List.of(invocation.getArgument(0), invocation.getArgument(1)), invocation.getArgument(2));
            return null;
        }).when(keyring).setPassword(anyString(), anyString(), anyString());
        doAnswer(invocation -> {
            secrets.remove(List.of(invocation.getArgument(0), invocation.getArgument(1)));
            return null;
        }).when(keyring).deletePassword(anyString(), anyString());
        factory = Mockito.mockStatic(Keyring.class);
        factory.when(Keyring::create).thenReturn(keyring);
        preferences = new JabRefCliPreferences(node);
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        factory.close();
        node.removeNode();
    }

    @Test
    void fourteenKeysAreLoadedWithOnePasswordRead() throws PasswordAccessException {
        // [utest->req~keyring.api-key-bundle~1]
        List<String> names = IntStream.range(0, 14).mapToObj(index -> "Fetcher" + index).toList();
        configure(names);
        preferences.putStringList("fetcherCustomKeyBundleNames", names);
        Map<String, String> keys = new HashMap<>();
        names.forEach(name -> keys.put(name, "key-" + name));
        secrets.put(List.of("org.jabref.apikeys.bundle", "keys"), net.minidev.json.JSONObject.toJSONString(keys));

        Set<FetcherApiKey> expected = names.stream().map(name -> new FetcherApiKey(name, true, "key-" + name))
                                           .collect(java.util.stream.Collectors.toSet());
        assertEquals(expected, preferences.getImporterPreferences().getApiKeys());
        verify(keyring).getPassword("org.jabref.apikeys.bundle", "keys");
        assertEquals(1, Mockito.mockingDetails(keyring).getInvocations().stream()
                               .filter(invocation -> invocation.getMethod().getName().equals("getPassword")).count());
    }

    @Test
    void noConfiguredKeysDoesNotOpenKeyring() {
        configure(List.of());
        preferences.getImporterPreferences();
        factory.verifyNoInteractions();
    }

    @Test
    void disabledPersistenceDoesNotOpenKeyring() {
        configure(List.of("Fetcher"));
        preferences.putBoolean("fetcherCustomKeyPersist", false);
        assertEquals(Set.of(new FetcherApiKey("Fetcher", true, "")), preferences.getImporterPreferences().getApiKeys());
        factory.verifyNoInteractions();
    }

    @Test
    void emptyStoredKeyMetadataDoesNotOpenKeyring() {
        configure(List.of("Fetcher"));
        preferences.putStringList("fetcherCustomKeyBundleNames", List.of());
        assertEquals(Set.of(new FetcherApiKey("Fetcher", true, "")), preferences.getImporterPreferences().getApiKeys());
        factory.verifyNoInteractions();
    }

    @Test
    void migratesSeparateKeysAndUsesBundleOnNextLoad() throws PasswordAccessException {
        configure(List.of("FetcherA", "FetcherB"));
        secrets.put(List.of("org.jabref.apikeys", "FetcherA"), "KeyA");
        secrets.put(List.of("org.jabref.apikeys", "FetcherB"), "KeyB");
        Set<FetcherApiKey> expected = Set.of(new FetcherApiKey("FetcherA", true, "KeyA"), new FetcherApiKey("FetcherB", true, "KeyB"));

        assertEquals(expected, preferences.getImporterPreferences().getApiKeys());
        assertEquals(List.of("FetcherA", "FetcherB"), preferences.getStringList("fetcherCustomKeyBundleNames"));
        assertEquals(List.of("FetcherA", "FetcherB"), preferences.getStringList("fetcherCustomKeyNames"));
        assertEquals(List.of("true", "true"), preferences.getStringList("fetcherCustomKeyUses"));
        clearInvocations(keyring);
        assertEquals(expected, new JabRefCliPreferences(node).getImporterPreferences().getApiKeys());
        verify(keyring, never()).getPassword("org.jabref.apikeys", "FetcherA");
        verify(keyring, never()).getPassword("org.jabref.apikeys", "FetcherB");
    }

    @Test
    void failedBundleWriteKeepsIndividualKeysForRetry() throws PasswordAccessException {
        configure(List.of("Fetcher"));
        secrets.put(List.of("org.jabref.apikeys", "Fetcher"), "Key");
        doThrow(new PasswordAccessException("Denied")).when(keyring)
                                                      .setPassword(Mockito.eq("org.jabref.apikeys.bundle"), Mockito.eq("keys"), anyString());

        assertEquals(Set.of(new FetcherApiKey("Fetcher", true, "Key")), preferences.getImporterPreferences().getApiKeys());
        assertEquals("Key", secrets.get(List.of("org.jabref.apikeys", "Fetcher")));
        assertFalse(preferences.hasKey("fetcherCustomKeyBundleNames"));
    }

    @Test
    void deniedBundleReadDoesNotFallBackToIndividualEntries() throws PasswordAccessException {
        configure(List.of("Fetcher"));
        preferences.putStringList("fetcherCustomKeyBundleNames", List.of("Fetcher"));
        doThrow(new PasswordAccessException("Denied")).when(keyring).getPassword("org.jabref.apikeys.bundle", "keys");

        assertEquals(Set.of(new FetcherApiKey("Fetcher", true, "")), preferences.getImporterPreferences().getApiKeys());
        verify(keyring, never()).getPassword("org.jabref.apikeys", "Fetcher");
        verify(keyring, never()).setPassword(anyString(), anyString(), anyString());
    }

    @Test
    void malformedBundleDoesNotOverwriteSecretsOrFallBack() throws PasswordAccessException {
        configure(List.of("Fetcher"));
        secrets.put(List.of("org.jabref.apikeys.bundle", "keys"), "{broken");
        preferences.getImporterPreferences();
        assertEquals("{broken", secrets.get(List.of("org.jabref.apikeys.bundle", "keys")));
        verify(keyring, never()).getPassword("org.jabref.apikeys", "Fetcher");
        verify(keyring, never()).setPassword(anyString(), anyString(), anyString());
    }

    @Test
    void savingKeysCreatesOneItemAndExcludesBlankAndBuiltInKeys() {
        ImporterPreferences importer = preferences.getImporterPreferences();
        importer.setPersistCustomKeys(true);
        importer.getApiKeys().add(new FetcherApiKey("Fetcher", true, " key "));
        importer.getApiKeys().add(new FetcherApiKey("Empty", false, " "));

        assertEquals(Map.of(List.of("org.jabref.apikeys.bundle", "keys"), "{\"Fetcher\":\"key\"}"), secrets);
        assertEquals(List.of("Fetcher"), preferences.getStringList("fetcherCustomKeyBundleNames"));
        assertEquals("", preferences.get("fetcherCustomKeys", ""));
    }

    @Test
    void clearingKeysPreventsLegacyValuesFromReturning() {
        configure(List.of("Fetcher"));
        preferences.putStringList("fetcherCustomKeyBundleNames", List.of("Fetcher"));
        secrets.put(List.of("org.jabref.apikeys.bundle", "keys"), "{\"Fetcher\":\"Key\"}");
        secrets.put(List.of("org.jabref.apikeys", "Fetcher"), "stale");
        preferences.getImporterPreferences().getApiKeys().clear();

        clearInvocations(keyring);
        new JabRefCliPreferences(node).getImporterPreferences();
        verifyNoInteractions(keyring);
        assertEquals(List.of(), preferences.getStringList("fetcherCustomKeyBundleNames"));
    }

    private void configure(List<String> names) {
        preferences.putBoolean("fetcherCustomKeyPersist", true);
        preferences.putStringList("fetcherCustomKeyNames", names);
        preferences.putStringList("fetcherCustomKeyUses", names.stream().map(_ -> "true").toList());
    }
}
