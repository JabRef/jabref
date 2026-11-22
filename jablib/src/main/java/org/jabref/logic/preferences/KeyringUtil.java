/*
 * > [!IMPORTANT]
 * > This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
 * >
 * > Read and follow:
 * > • [AGENTS.md](./AGENTS.md)
 * > • [CONTRIBUTING.md](./CONTRIBUTING.md)
 */
package org.jabref.logic.preferences;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.jabref.logic.shared.security.Password;
import org.jabref.logic.util.strings.StringUtil;

import com.github.javakeyring.BackendNotSupportedException;
import com.github.javakeyring.Keyring;
import com.github.javakeyring.KeyringStorageType;
import com.github.javakeyring.PasswordAccessException;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
final class KeyringUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyringUtil.class);
    private static final String CITE_DRIVE_SERVICE = "org.jabref";
    private static final String CITE_DRIVE_ACCOUNT = "citedrive";

    private final Supplier<String> legacyPasswordKey;

    KeyringUtil(Supplier<String> legacyPasswordKey) {
        this.legacyPasswordKey = legacyPasswordKey;
    }

    /// Identifies a single secret in the system keyring by its `service` and `account`.
    ///
    /// The secret is stored as plain text under `service`. Older versions stored it encrypted with [Password] under
    /// `legacyService` (same `account`); such values are migrated when they are read.
    ///
    /// See [ADR 0081](https://devdocs.jabref.org/decisions/0081-store-keyring-secrets-without-application-encryption.html) for why no extra encryption is used.
    record KeyringSlot(String service, String account, String legacyService) {
        // [impl->adr~keyring.no-application-encryption~1]
        static final KeyringSlot CUSTOM_API_KEYS = new KeyringSlot("org.jabref.apikeys.bundle", "keys", "org.jabref.apikeys.bundle");
        static final KeyringSlot PROXY_PASSWORD = new KeyringSlot("org.jabref.proxy", "proxy", "org.jabref");
        static final KeyringSlot GITHUB_PAT = new KeyringSlot("org.jabref.github", "github", "org.jabref");

        /// Slot holding a custom API key for the fetcher with the given name.
        static KeyringSlot customApiKey(String fetcherName) {
            return new KeyringSlot("org.jabref.apikeys", fetcherName, "org.jabref.customapikeys");
        }
    }

    /// Individual entries may be cleared after the bundle is written when migration is ready.
    record FetcherKeyRead(Map<String, String> keys, List<String> individualEntriesToClear, boolean readyToMigrate) {
    }

    /// Reads a nonblank secret, migrating an encrypted entry when needed.
    Optional<String> read(KeyringSlot slot) {
        try (Keyring keyring = Keyring.create()) {
            return Optional.ofNullable(readSecret(keyring, slot)).filter(StringUtil::isNotBlank);
        } catch (Exception ex) {
            LOGGER.warn("Could not open keyring", ex);
            return Optional.empty();
        }
    }

    void write(KeyringSlot slot, String secret) throws PasswordAccessException {
        Map<KeyringSlot, String> single = new HashMap<>();
        single.put(slot, secret);
        writeKeyringEntries(single);
    }

    Map<String, String> readFetcherKeyBundle() {
        return read(KeyringSlot.CUSTOM_API_KEYS).map(this::parseFetcherKeys).orElseGet(Map::of);
    }

    void writeFetcherKeyBundle(Map<String, String> keys) throws PasswordAccessException {
        write(KeyringSlot.CUSTOM_API_KEYS, keys.isEmpty() ? "" : JSONObject.toJSONString(keys));
    }

    static String readCiteDriveToken() throws Exception {
        try (Keyring keyring = Keyring.create()) {
            return keyring.getPassword(CITE_DRIVE_SERVICE, CITE_DRIVE_ACCOUNT);
        }
    }

    static void writeCiteDriveToken(String secret) throws Exception {
        try (Keyring keyring = Keyring.create()) {
            keyring.setPassword(CITE_DRIVE_SERVICE, CITE_DRIVE_ACCOUNT, secret);
        }
    }

    static void clearCiteDriveToken() throws Exception {
        try (Keyring keyring = Keyring.create()) {
            keyring.deletePassword(CITE_DRIVE_SERVICE, CITE_DRIVE_ACCOUNT);
        }
    }

    /// Reads the plain-text secret of a slot, migrating a legacy encrypted value first if needed.
    ///
    /// @return the secret, or an empty string if no usable secret could be read
    private String readSecret(Keyring keyring, KeyringSlot slot) {
        try {
            return keyring.getPassword(slot.service(), slot.account());
        } catch (PasswordAccessException _) {
            if (slot.equals(KeyringSlot.CUSTOM_API_KEYS)) {
                return "";
            }
            return migrateLegacySecret(keyring, slot);
        }
    }

    /// Moves a secret stored encrypted by older JabRef versions to the slot's current location as plain text.
    ///
    /// The legacy entry is removed only after the new one was written, so a failed write is retried on the next read.
    /// A legacy value that cannot be decrypted (e.g., because the owner or the host name changed) is discarded.
    ///
    /// @return the decrypted secret, or "" if there is no legacy value or it cannot be decrypted
    private String migrateLegacySecret(Keyring keyring, KeyringSlot slot) {
        // [impl->req~keyring.legacy-migration~1]
        String encrypted;
        try {
            encrypted = keyring.getPassword(slot.legacyService(), slot.account());
        } catch (PasswordAccessException _) {
            LOGGER.debug("No secret stored in keyring for {}/{}", slot.service(), slot.account());
            return "";
        }

        String secret;
        try {
            secret = decryptLegacySecret(encrypted);
        } catch (GeneralSecurityException | UnsupportedEncodingException | IllegalArgumentException e) {
            LOGGER.warn("Could not decrypt the stored secret for {}/{}. It is discarded and has to be entered again.", slot.legacyService(), slot.account(), e);
            deleteFromKeyring(keyring, slot.legacyService(), slot.account());
            return "";
        }

        try {
            keyring.setPassword(slot.service(), slot.account(), secret);
        } catch (PasswordAccessException e) {
            LOGGER.warn("Could not move the secret for {}/{} to {}", slot.legacyService(), slot.account(), slot.service(), e);
            return secret;
        }
        deleteFromKeyring(keyring, slot.legacyService(), slot.account());
        return secret;
    }

    private String decryptLegacySecret(String encrypted) throws GeneralSecurityException, UnsupportedEncodingException {
        return new Password(encrypted, legacyPasswordKey.get()).decrypt();
    }

    /// Deletes an entry from the keyring.
    ///
    /// @return `false` if deletion could not be confirmed
    private static boolean deleteFromKeyring(Keyring keyring, String service, String account) {
        try {
            keyring.deletePassword(service, account);
            return true;
        } catch (PasswordAccessException e) {
            LOGGER.debug("Could not delete keyring entry for {}/{}", service, account, e);
            return false;
        }
    }

    /// @throws PasswordAccessException if the keyring entries could not be updated
    private void writeKeyringEntries(Map<KeyringSlot, String> secrets) throws PasswordAccessException {
        Keyring keyring;
        try {
            keyring = Keyring.create();
        } catch (BackendNotSupportedException | RuntimeException ex) {
            throw new PasswordAccessException("Could not open the keyring", ex);
        }
        // [impl->req~keyring.plain-text-storage~1]
        // [impl->req~keyring.legacy-migration~1]
        try {
            for (Map.Entry<KeyringSlot, String> entry : secrets.entrySet()) {
                KeyringSlot slot = entry.getKey();
                if (slot.equals(KeyringSlot.CUSTOM_API_KEYS)) {
                    if (StringUtil.isNotBlank(entry.getValue())) {
                        keyring.setPassword(slot.service(), slot.account(), entry.getValue());
                    } else {
                        keyring.deletePassword(slot.service(), slot.account());
                    }
                } else if (StringUtil.isNotBlank(entry.getValue())) {
                    keyring.setPassword(slot.service(), slot.account(), entry.getValue().trim());
                    deleteFromKeyring(keyring, slot.legacyService(), slot.account());
                } else {
                    // A nonempty blank value suppresses migration and works with keyrings that reject empty values.
                    keyring.setPassword(slot.service(), slot.account(), " ");
                    if (deleteFromKeyring(keyring, slot.legacyService(), slot.account())) {
                        deleteFromKeyring(keyring, slot.service(), slot.account());
                    }
                }
            }
        } catch (RuntimeException ex) {
            throw new PasswordAccessException("Could not update keyring entries", ex);
        } finally {
            try {
                keyring.close();
            } catch (Exception ex) {
                LOGGER.warn("Could not close the keyring", ex);
            }
        }
    }

    FetcherKeyRead readFetcherKeysForMigration(List<String> names) {
        Map<String, String> keys = new HashMap<>();
        List<String> individualEntries = new ArrayList<>();
        boolean complete = true;
        try (Keyring keyring = Keyring.create()) {
            try {
                String json = keyring.getPassword(KeyringSlot.CUSTOM_API_KEYS.service(), KeyringSlot.CUSTOM_API_KEYS.account());
                if (StringUtil.isNotBlank(json)) {
                    return new FetcherKeyRead(parseFetcherKeys(json), List.of(), false);
                }
            } catch (PasswordAccessException ex) {
                complete = isKeyringEntryMissing(keyring, KeyringSlot.CUSTOM_API_KEYS.service(), KeyringSlot.CUSTOM_API_KEYS.account(), ex);
                if (!complete) {
                    LOGGER.debug("Could not confirm that the custom API key bundle is absent", ex);
                }
            }

            for (String name : names) {
                KeyringSlot slot = KeyringSlot.customApiKey(name);
                try {
                    readIndividualFetcherKey(keyring, slot, individualEntries)
                            .filter(StringUtil::isNotBlank)
                            .ifPresent(secret -> keys.put(name, secret));
                } catch (PasswordAccessException ex) {
                    LOGGER.debug("Could not read the individual API key for {}", name, ex);
                    complete = false;
                    // Preserve access to encrypted keys on backends that cannot confirm a missing plain-text entry.
                    // Leave both locations untouched while the plain-text entry's status is unknown.
                    if (keyring.getKeyringStorageType() == KeyringStorageType.KWALLET
                            || keyring.getKeyringStorageType() == KeyringStorageType.LEGACY_OSX_KEYCHAIN) {
                        try {
                            readEncryptedFetcherKey(keyring, slot, individualEntries)
                                    .filter(StringUtil::isNotBlank)
                                    .ifPresent(secret -> keys.put(name, secret));
                        } catch (PasswordAccessException legacyFailure) {
                            LOGGER.debug("Could not read the encrypted API key for {}", name, legacyFailure);
                        }
                    }
                } catch (RuntimeException ex) {
                    LOGGER.debug("Could not read the individual API key for {}", name, ex);
                    complete = false;
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Could not access the keyring for API key migration", ex);
            return new FetcherKeyRead(keys, List.of(), false);
        }
        return new FetcherKeyRead(keys, complete ? individualEntries : List.of(), complete);
    }

    private Optional<String> readIndividualFetcherKey(Keyring keyring, KeyringSlot slot, List<String> individualEntries) throws PasswordAccessException {
        try {
            String secret = Optional.ofNullable(keyring.getPassword(slot.service(), slot.account()))
                                    .orElseThrow(() -> new PasswordAccessException("Unconfirmed keyring response"));
            individualEntries.add(slot.account());
            return Optional.of(secret);
        } catch (PasswordAccessException ex) {
            if (!isKeyringEntryMissing(keyring, slot.service(), slot.account(), ex)) {
                throw ex;
            }
        }

        return readEncryptedFetcherKey(keyring, slot, individualEntries);
    }

    private Optional<String> readEncryptedFetcherKey(Keyring keyring, KeyringSlot slot, List<String> individualEntries) throws PasswordAccessException {
        String encrypted;
        try {
            encrypted = Optional.ofNullable(keyring.getPassword(slot.legacyService(), slot.account()))
                                .orElseThrow(() -> new PasswordAccessException("Unconfirmed keyring response"));
        } catch (PasswordAccessException ex) {
            if (isKeyringEntryMissing(keyring, slot.legacyService(), slot.account(), ex)) {
                return Optional.empty();
            }
            throw ex;
        }
        individualEntries.add(slot.account());
        try {
            return Optional.of(decryptLegacySecret(encrypted));
        } catch (GeneralSecurityException | UnsupportedEncodingException | IllegalArgumentException ex) {
            LOGGER.warn("Could not decrypt the stored API key for {}. It has to be entered again.", slot.account(), ex);
            return Optional.empty();
        }
    }

    private static boolean isKeyringEntryMissing(Keyring keyring, String service, String account, PasswordAccessException failure) {
        KeyringStorageType backend = keyring.getKeyringStorageType();
        // java-keyring 1.0.4 exposes missing entries through these backend-specific messages.
        // KWallet can report a missing entry after opening the wallet failed. Legacy macOS discards the status code.
        return switch (backend) {
            case OSX_KEYCHAIN,
                 GNOME_KEYRING ->
                    ("No stored credentials match " + service + " account: " + account).equals(failure.getMessage());
            case WINDOWS_CREDENTIAL_STORE ->
                    "Error code 1168".equals(failure.getMessage());
            case KWALLET,
                 LEGACY_OSX_KEYCHAIN ->
                    false;
        };
    }

    private Map<String, String> parseFetcherKeys(String json) {
        try {
            Object parsed = new JSONParser(JSONParser.MODE_RFC4627).parse(json);
            if (parsed instanceof Map<?, ?> values && values.entrySet().stream()
                                                            .allMatch(entry -> entry.getKey() instanceof String && entry.getValue() instanceof String)) {
                Map<String, String> keys = new HashMap<>();
                values.forEach((name, secret) -> keys.put((String) name, (String) secret));
                return keys;
            }
            LOGGER.warn("Invalid custom API key bundle");
        } catch (net.minidev.json.parser.ParseException _) {
            // Parser exceptions can contain secrets from the input.
            LOGGER.warn("Could not parse custom API key bundle");
        }
        return Map.of();
    }

    void clearIndividualFetcherKeys(List<String> names) throws PasswordAccessException {
        Map<KeyringSlot, String> cleared = new HashMap<>();
        for (String name : names) {
            cleared.put(KeyringSlot.customApiKey(name), "");
        }
        if (!cleared.isEmpty()) {
            writeKeyringEntries(cleared);
        }
    }
}
