---
nav_order: 85
parent: Decision Records
---

# Store custom API keys in one keyring item
`adr~bundle-custom-api-keys~1`

Needs: impl

## Context and Problem Statement

Across macOS, Linux, and Windows, storing each fetcher's custom API key in a separate keyring item requires one password read per saved key.
On macOS, the keychain grants application access separately for each item, so loading saved keys can produce repeated dialogs with the same service name.
Users can mistake the sequence for a permission loop.

How can JabRef load saved custom API keys with one keyring read across supported operating systems?

## Considered Options

* Keep a separate item for each fetcher and load all items at startup.
* Keep separate items and defer access until each fetcher is used.
* Store the custom API keys together as a JSON object in one keyring item.

## Decision Outcome

Chosen option: "Store the custom API keys together as a JSON object in one keyring item", because one shared item avoids repeated permission dialogs when loading saved keys.

This storage format applies to macOS, Linux, and Windows through the existing platform-specific keyring backends.
Store custom API keys together at service `org.jabref.apikeys.bundle`, account `keys`.
The JSON object maps fetcher names to their custom keys.
Blank keys and values equal to built-in defaults are omitted when saving.
The normal preferences contain only the names of stored keys, allowing startup to skip keyring access when no keys are stored or persistence is disabled.

Existing separate entries are read on first use and copied into the bundle.
They are cleared only after the bundle is successfully written.
This migration can still require individual approvals once: changing the storage format cannot bypass existing macOS permissions.
The existing encrypted-entry migration from [ADR 0081](0081-store-keyring-secrets-without-application-encryption.md) remains supported.

### Consequences

* Loading saved keys requires one password read after migration on macOS, Linux, and Windows.
* On macOS, one shared item avoids separate approval dialogs for every fetcher's key.
* Linux and Windows retain their existing credential-store permissions.
* Empty stores can be detected without opening the keyring.
* The complete bundle is rewritten when a custom key changes.
* Older JabRef versions cannot read keys saved only in the bundle.

### Confirmation

`CustomApiKeyBundleTest` covers fourteen keys loaded with one read, empty-store checks, migration, denied access, and failed writes.

<!-- markdownlint-disable-file MD022 -->
