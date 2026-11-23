package org.jabref.logic.preferences;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.IntStream;

import org.jabref.logic.JabRefException;
import org.jabref.logic.importer.ImporterPreferences;

import com.github.javakeyring.Keyring;
import com.github.javakeyring.KeyringStorageType;
import com.github.javakeyring.PasswordAccessException;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        when(keyring.getKeyringStorageType()).thenReturn(KeyringStorageType.OSX_KEYCHAIN);
        when(keyring.getPassword(anyString(), anyString())).thenAnswer(invocation -> {
            String service = invocation.getArgument(0);
            String account = invocation.getArgument(1);
            List<String> slot = List.of(service, account);
            if (!secrets.containsKey(slot)) {
                String missingMessage = keyring.getKeyringStorageType() == KeyringStorageType.WINDOWS_CREDENTIAL_STORE
                                        ? "Error code 1168"
                                        : "No stored credentials match " + service + " account: " + account;
                throw new PasswordAccessException(missingMessage);
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
                               .filter(invocation -> "getPassword".equals(invocation.getMethod().getName())).count());
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

    @ParameterizedTest
    @EnumSource(names = {"OSX_KEYCHAIN", "GNOME_KEYRING", "WINDOWS_CREDENTIAL_STORE"})
    void deniedIndividualReadDefersMigrationUntilTheNextLoad(KeyringStorageType backend) throws PasswordAccessException {
        configure(List.of("Readable", "Denied", "Missing"));
        when(keyring.getKeyringStorageType()).thenReturn(backend);
        List<String> readableSlot = List.of("org.jabref.apikeys", "Readable");
        List<String> deniedSlot = List.of("org.jabref.apikeys", "Denied");
        secrets.put(readableSlot, "KeyA");
        secrets.put(deniedSlot, "KeyB");
        Mockito.doThrow(new PasswordAccessException("Failed to get credential. Authorization failed"))
               .doReturn("KeyB")
               .when(keyring).getPassword("org.jabref.apikeys", "Denied");

        assertEquals(Set.of(
                new FetcherApiKey("Readable", true, "KeyA"),
                new FetcherApiKey("Denied", true, ""),
                new FetcherApiKey("Missing", true, "")), preferences.getImporterPreferences().getApiKeys());
        assertEquals(Map.of(readableSlot, "KeyA", deniedSlot, "KeyB"), secrets);
        assertFalse(preferences.hasKey("fetcherCustomKeyBundleNames"));

        assertEquals(Set.of(
                new FetcherApiKey("Readable", true, "KeyA"),
                new FetcherApiKey("Denied", true, "KeyB"),
                new FetcherApiKey("Missing", true, "")), new JabRefCliPreferences(node).getImporterPreferences().getApiKeys());
        assertEquals(List.of("Denied", "Readable"), preferences.getStringList("fetcherCustomKeyBundleNames"));
    }

    @Test
    void ambiguousKWalletReadPreservesIndividualKeysAndLeavesMigrationPending() throws PasswordAccessException {
        configure(List.of("Fetcher"));
        List<String> individualSlot = List.of("org.jabref.apikeys", "Fetcher");
        secrets.put(individualSlot, "Key");
        when(keyring.getKeyringStorageType()).thenReturn(KeyringStorageType.KWALLET);
        doThrow(new PasswordAccessException("Password is not in wallet")).when(keyring).getPassword("org.jabref.apikeys.bundle", "keys");

        assertEquals(Set.of(new FetcherApiKey("Fetcher", true, "Key")), preferences.getImporterPreferences().getApiKeys());
        assertEquals(Map.of(individualSlot, "Key"), secrets);
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
    void malformedEstablishedBundleDoesNotOverwriteSecretsOrFallBack() throws PasswordAccessException {
        configure(List.of("Fetcher"));
        preferences.putStringList("fetcherCustomKeyBundleNames", List.of("Fetcher"));
        secrets.put(List.of("org.jabref.apikeys.bundle", "keys"), "{broken");
        preferences.getImporterPreferences();
        assertEquals("{broken", secrets.get(List.of("org.jabref.apikeys.bundle", "keys")));
        verify(keyring, never()).getPassword("org.jabref.apikeys", "Fetcher");
        verify(keyring, never()).setPassword(anyString(), anyString(), anyString());
    }

    @Test
    void malformedUncommittedBundleDoesNotHideIndividualKeys() {
        configure(List.of("Fetcher"));
        Map<List<String>, String> stored = Map.of(
                List.of("org.jabref.apikeys.bundle", "keys"), "{broken",
                List.of("org.jabref.apikeys", "Fetcher"), "Key");
        secrets.putAll(stored);

        assertEquals(Set.of(new FetcherApiKey("Fetcher", true, "Key")), preferences.getImporterPreferences().getApiKeys());
        assertEquals(stored, secrets);
        assertFalse(preferences.hasKey("fetcherCustomKeyBundleNames"));
    }

    @Test
    void switchingToSessionOnlyClearsBundleAndIndividualKeysWithoutBundleMetadata() throws JabRefException {
        configure(List.of("Fetcher"));
        secrets.put(List.of("org.jabref.apikeys.bundle", "keys"), "{\"Fetcher\":\"Key\"}");
        secrets.put(List.of("org.jabref.apikeys", "Fetcher"), "Key");
        Set<FetcherApiKey> keys = preferences.getImporterPreferences().getApiKeys();

        preferences.prepareSessionFetcherApiKeys(keys).writeKeyring().apply();

        assertEquals(Map.of(), secrets);
        assertEquals(List.of(), preferences.getStringList("fetcherCustomKeyBundleNames"));
        assertFalse(preferences.getImporterPreferences().shouldPersistCustomKeys());
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
    void preparedSavePublishesSettingsOnlyAfterTheKeyringWrite() throws JabRefException, PasswordAccessException {
        Set<FetcherApiKey> savedKeys = Set.of(new FetcherApiKey("Previous", true, "saved-key"));
        preferences.prepareSavedFetcherApiKeys(savedKeys).writeKeyring().apply();
        clearInvocations(keyring);

        Set<FetcherApiKey> updatedKeys = Set.of(new FetcherApiKey("Updated", false, "new-key"));
        CliPreferences.CredentialSave save = preferences.prepareSavedFetcherApiKeys(updatedKeys);
        verifyNoInteractions(keyring);

        CliPreferences.CredentialCommit commit = save.writeKeyring();
        verify(keyring).setPassword("org.jabref.apikeys.bundle", "keys", "{\"Updated\":\"new-key\"}");
        assertEquals(savedKeys, preferences.getImporterPreferences().getApiKeys());
        assertEquals(List.of("Previous"), preferences.getStringList("fetcherCustomKeyBundleNames"));

        commit.apply();
        assertEquals(updatedKeys, preferences.getImporterPreferences().getApiKeys());
        assertEquals(List.of("Updated"), preferences.getStringList("fetcherCustomKeyBundleNames"));
    }

    @Test
    void failedSaveKeepsPreviousKeysAndPreferences() throws JabRefException, PasswordAccessException {
        Set<FetcherApiKey> savedKeys = Set.of(new FetcherApiKey("Previous", true, "saved-key"));
        preferences.prepareSavedFetcherApiKeys(savedKeys).writeKeyring().apply();
        clearInvocations(keyring);
        doThrow(new PasswordAccessException("Denied")).when(keyring)
                                                      .setPassword(Mockito.eq("org.jabref.apikeys.bundle"), Mockito.eq("keys"), anyString());

        Set<FetcherApiKey> replacementKeys = Set.of(
                new FetcherApiKey("Previous", false, "changed-key"),
                new FetcherApiKey("New", true, "new-key"));
        assertThrows(JabRefException.class, preferences.prepareSavedFetcherApiKeys(replacementKeys)::writeKeyring);

        assertEquals(savedKeys, preferences.getImporterPreferences().getApiKeys());
        assertEquals(savedKeys, new JabRefCliPreferences(node).getImporterPreferences().getApiKeys());
        assertEquals(List.of("Previous"), preferences.getStringList("fetcherCustomKeyBundleNames"));
        verify(keyring, never()).deletePassword(anyString(), anyString());
    }

    @Test
    void stalePreferencesCannotOverwriteKeysAddedByAnotherInstance() throws JabRefException {
        FetcherApiKey firstKey = new FetcherApiKey("First", true, "first-key");
        preferences.prepareSavedFetcherApiKeys(Set.of(firstKey)).writeKeyring().apply();
        JabRefCliPreferences otherPreferences = new JabRefCliPreferences(node);
        Set<FetcherApiKey> originalKeys = otherPreferences.getImporterPreferences().getApiKeys();
        Set<FetcherApiKey> additionalKeys = Set.of(firstKey, new FetcherApiKey("Second", true, "second-key"));
        preferences.prepareSavedFetcherApiKeys(additionalKeys).writeKeyring().apply();

        Set<FetcherApiKey> staleEdit = Set.of(new FetcherApiKey("First", true, "changed-key"));
        assertThrows(JabRefException.class, otherPreferences.prepareSavedFetcherApiKeys(staleEdit)::writeKeyring);

        assertEquals(Set.of(firstKey), originalKeys);
        assertEquals(additionalKeys, new JabRefCliPreferences(node).getImporterPreferences().getApiKeys());
    }

    @Test
    void importEnablingPersistenceLoadsSavedKeysWithImportedMetadata(@TempDir Path tempDir) throws JabRefException, PasswordAccessException {
        // [utest->req~keyring.api-key-import~1]
        Path exported = tempDir.resolve("preferences.xml");
        configure(List.of("Imported"));
        preferences.putStringList("fetcherCustomKeyUses", List.of("false"));
        preferences.putStringList("fetcherCustomKeyBundleNames", List.of());
        preferences.exportPreferences(exported);
        Map<List<String>, String> savedBundle = Map.of(List.of("org.jabref.apikeys.bundle", "keys"), "{\"Imported\":\"saved-key\"}");
        secrets.putAll(savedBundle);

        configure(List.of("Previous"));
        preferences.putStringList("fetcherCustomKeyBundleNames", List.of("Imported"));
        preferences.putBoolean("fetcherCustomKeyPersist", false);
        ImporterPreferences importer = preferences.getImporterPreferences();

        preferences.importPreferences(exported);

        assertTrue(importer.shouldPersistCustomKeys());
        assertEquals(Set.of(new FetcherApiKey("Imported", false, "saved-key")), importer.getApiKeys());
        assertEquals(List.of("Imported"), preferences.getStringList("fetcherCustomKeyNames"));
        assertEquals(List.of("false"), preferences.getStringList("fetcherCustomKeyUses"));
        assertEquals(savedBundle, secrets);
        verify(keyring, never()).setPassword(anyString(), anyString(), anyString());
        verify(keyring, never()).deletePassword(anyString(), anyString());
    }

    @Test
    void importDisablingPersistenceClearsSavedKeysAndKeepsImportedMetadata(@TempDir Path tempDir) throws JabRefException, PasswordAccessException {
        // [utest->req~keyring.api-key-import~1]
        Path exported = tempDir.resolve("preferences.xml");
        configure(List.of("Imported"));
        preferences.putBoolean("fetcherCustomKeyPersist", false);
        preferences.putStringList("fetcherCustomKeyBundleNames", List.of());
        preferences.exportPreferences(exported);

        configure(List.of("Previous"));
        preferences.putStringList("fetcherCustomKeyBundleNames", List.of("Previous"));
        secrets.put(List.of("org.jabref.apikeys.bundle", "keys"), "{\"Previous\":\"saved-key\"}");
        ImporterPreferences importer = preferences.getImporterPreferences();

        preferences.importPreferences(exported);

        assertFalse(importer.shouldPersistCustomKeys());
        assertEquals(Set.of(new FetcherApiKey("Imported", true, "")), importer.getApiKeys());
        assertEquals(List.of("Imported"), preferences.getStringList("fetcherCustomKeyNames"));
        assertEquals(List.of("true"), preferences.getStringList("fetcherCustomKeyUses"));
        assertEquals(List.of(), preferences.getStringList("fetcherCustomKeyBundleNames"));
        assertEquals(Map.of(), secrets);
        verify(keyring).deletePassword("org.jabref.apikeys.bundle", "keys");
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
