---
nav_order: 83
parent: Decision Records
---

# Store custom API keys in one keyring item
`adr~bundle-custom-api-keys~1`

Needs: impl

> [!IMPORTANT]
> This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
>
> Read and follow:
> • [AGENTS.md](./AGENTS.md)
> • [CONTRIBUTING.md](./CONTRIBUTING.md)

## Context and Problem Statement

macOS grants application access separately for each keychain item.
Storing every fetcher's API key separately produces repeated dialogs that display the same service name.
Users can mistake the sequence for a permission loop.

How can JabRef load saved custom API keys with one keychain read?

## Considered Options

* Keep a separate item for each fetcher and load all items at startup.
* Keep separate items and defer access until each fetcher is used.
* Store the custom API keys together as a JSON object in one keyring item.

## Decision Outcome

Store custom API keys together at service `org.jabref.apikeys.bundle`, account `keys`.
The JSON object maps fetcher names to their custom keys.
Blank keys and values equal to built-in defaults are omitted when saving.
The normal preferences contain only the names of stored keys, allowing startup to skip keyring access when no keys are stored or persistence is disabled.

Existing separate entries are read on first use and copied into the bundle.
They are cleared only after the bundle is successfully written.
This migration can still require individual approvals once: changing the storage format cannot bypass existing macOS permissions.
The existing encrypted-entry migration from [ADR 0081](0081-store-keyring-secrets-without-application-encryption.md) remains supported.

### Consequences

* Loading saved keys requires one password read after migration.
* Empty stores can be detected without opening the keyring.
* The complete bundle is rewritten when a custom key changes.
* Older JabRef versions cannot read keys saved only in the bundle.

### Confirmation

`CustomApiKeyBundleTest` covers fourteen keys loaded with one read, empty-store checks, migration, denied access, and failed writes.

<!-- markdownlint-disable-file MD022 -->
