---
nav_order: 4
has_children: true
---
# Decision Records

Below, all "architectural decision records" for JabRef are listed.
This list uses the TOC functionality of the [Just the Docs Jekyll template](https://just-the-docs.github.io/just-the-docs/).

For new ADRs, please use [adr-template.md](adr-template.md) as basis.
More information on MADR is available at <https://adr.github.io/madr/>.
General information about architectural decision records is available at <https://adr.github.io/>.

Writer integration decisions:

- [ADR-0067: Use Pandoc to convert BST output](0067-use-pandoc-for-bst-conversion.md) — conversion rationale and current BST pipeline.
- [ADR-0068: Use a preference to handle Zotero reference marks](0068-use-a-preference-for-Zotero-compatibility-mode.md) — compatibility preference and historical encoding, partially superseded by ADR-0084.
- [ADR-0084: Use canonical Writer citation marks across style families](0084-canonical-writer-citations.md) — current identity, rendering, migration, and conversion policy.

Keyring decisions:

- [ADR-0085: Store custom API keys in one keyring item](0085-bundle-custom-api-keys.md).
