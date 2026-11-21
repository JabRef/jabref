---
parent: Requirements
---
# Secrets in the system keyring

## Secrets must be stored in the system keyring without additional encryption
`req~keyring.plain-text-storage~1`

The proxy password, the GitHub personal access token, and custom API keys are stored as plain text in the system keyring.
Proxy passwords and GitHub tokens have their own locations (`org.jabref.proxy`, `org.jabref.github`).
Custom API keys share one item at `org.jabref.apikeys.bundle`.
Clearing a secret removes it from the keyring.

Rationale: Older JabRef versions encrypted these secrets before storing them in the keyring.
The key was derived from the user and host name, and the initialization vector was fixed, so anyone with access to the keyring entry could decrypt it.
The keyring already protects its entries (e.g., with the login password on macOS), so the extra encryption added no protection ([#13687](https://github.com/JabRef/jabref/issues/13687)).
A new location is used for each secret so that plain-text values cannot be confused with encrypted values from older versions.

Needs: impl, utest

## Secrets stored by older versions must be migrated
`req~keyring.legacy-migration~1`

When a secret is read and only an encrypted value from an older JabRef version exists at the legacy location (`org.jabref` or `org.jabref.customapikeys`), it is decrypted, written to the new location, and the legacy entry is deleted.
The user does not need to do anything after upgrading.
A legacy value that cannot be decrypted (e.g., because the owner or the host name changed) is discarded and has to be entered again.
A cleared secret must not come back from its legacy location, even if the legacy entry cannot be deleted.

Rationale: Migrating on read keeps existing secrets working without a separate migration step.

Needs: impl, utest

## Custom API keys must be loaded from one keyring item
`req~keyring.api-key-bundle~1`

Saved custom API keys share one keyring item, so loading them requires one password read after migration.
Normal preferences record only which keys are stored; startup must skip the keyring when no keys are stored or persistence is disabled.
Blank keys and built-in defaults must not be stored as custom secrets.
Existing separate entries must be preserved until their values have been written successfully to the shared item.
Migration can still require one approval per existing protected item.

Needs: impl, utest

## Preference imports must load custom API keys before applying persistence changes
`req~keyring.api-key-import~1`

An import with persistence enabled loads keys from the local bundle using the imported fetcher metadata.
Replaying the metadata leaves an existing bundle intact.
If the imported settings disable persistence, the local keys are cleared after replay.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
