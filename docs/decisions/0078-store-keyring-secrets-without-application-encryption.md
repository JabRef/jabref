---
nav_order: 78
parent: Decision Records
---

# Store keyring secrets without application-level encryption

`adr~keyring.no-application-encryption~1`

Needs: impl

## Context and Problem Statement

JabRef stores the proxy password, the GitHub personal access token, and custom API keys in the system keyring.
Older versions encrypted these values with `Password` (AES/CBC) before storing them.
The key was derived from the user and host name, and the initialization vector was fixed ([#13687](https://github.com/JabRef/jabref/issues/13687)).
Anyone who can read the keyring entry can therefore also compute the key and decrypt the value.

How should JabRef store these secrets, and what happens to values stored by older versions?

## Decision Drivers

* The secrets must stay protected at least as well as before.
* Users must not have to enter their secrets again after upgrading.
* Values written by the new version must never be confused with encrypted values from older versions.
* The code should not suggest a protection that does not exist.

## Considered Options

* Keep the application-level encryption
* Improve the application-level encryption (random IV, stronger key)
* Store plain text in the keyring at the old location
* Store plain text in the keyring at a new location and migrate legacy values on read

## Decision Outcome

Chosen option: "Store plain text in the keyring at a new location and migrate legacy values on read", because the keyring already protects its entries (e.g., with the login password on macOS), and a new location makes it clear whether a value is plain text or a legacy encrypted value.

The maintainers decided to remove the extra encryption in [#13687](https://github.com/JabRef/jabref/issues/13687).

### Consequences

* Good, because there is no encryption that only looks like protection.
* Good, because existing secrets keep working without any action by the user.
* Good, because the new locations (`org.jabref.proxy`, `org.jabref.github`, `org.jabref.apikeys`) never contain encrypted values, so no format detection is needed.
* Bad, because `Password` has to stay until the legacy values are expected to be migrated (e.g., in JabRef 7, as noted in the issue).
* Bad, because a legacy value that cannot be decrypted (e.g., after the owner or the host name changed) is lost and has to be entered again.

### Confirmation

`KeyringSlot` in `JabRefCliPreferences` carries the tag `[impl->adr~keyring.no-application-encryption~1]`.
The behaviour is described in `req~keyring.plain-text-storage~1` and `req~keyring.legacy-migration~1` and covered by `KeyringMigrationTest`.

## Pros and Cons of the Options

### Keep the application-level encryption

* Good, because nothing changes.
* Bad, because the key can be computed by anyone who can read the entry, so the encryption adds no protection.

### Improve the application-level encryption (random IV, stronger key)

* Good, because the encryption itself would no longer be weak.
* Bad, because JabRef would still need to store or derive the key somewhere on the same machine, which the keyring already protects.
* Bad, because it adds code that has to be maintained without adding real protection.

### Store plain text in the keyring at the old location

* Good, because no new locations are needed.
* Bad, because a stored value cannot be reliably identified as plain text or as an encrypted legacy value.
* Bad, because an older JabRef version would try to decrypt a plain-text value after a downgrade.

### Store plain text in the keyring at a new location and migrate legacy values on read

* Good, because plain-text and legacy values are clearly separated.
* Good, because migration happens automatically the first time a secret is read.
* Neutral, because the legacy location is still checked when the new location is empty.
