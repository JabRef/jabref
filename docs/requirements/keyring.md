---
parent: Requirements
---
# Secrets in the system keyring

## Secrets must be stored in the system keyring without additional encryption
`req~keyring.plain-text-storage~1`

The proxy password, the GitHub personal access token, and custom API keys are stored as plain text in the system keyring.
Each secret has its own location (`org.jabref.proxy`, `org.jabref.github`, `org.jabref.apikeys`).
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

<!-- markdownlint-disable-file MD022 -->
