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

AI decisions:

- [ADR-0032: Store Chats in a Local User Folder](0032-store-chats-in-local-user-folder.md).
- [ADR-0033: Store Chats in MVStore](0033-store-chats-in-mvstore.md).
- [ADR-0034: Use Citation Key for Grouping Chat Messages](0034-use-citation-key-for-grouping-chat-messages.md).
- [ADR-0035: Generate Embeddings Online](0035-generate-embeddings-online.md).
- [ADR-0036: Use Markdown rendering for Chat Message Content](0036-use-markdown-for-chat-content.md).
- [ADR-0037: RAG Architecture Implementation](0037-rag-architecture-implementation.md).
- [ADR-0042: Use `MarkdownTextFlow` for Summarization Content](0042-use-markdowntextflow-for-summarization-content.md).
- [ADR-0058: Use Deep Java Library for embeddings in AI features](0058-use-djl-for-embeddings.md).
- [ADR-0059: Use CUID2 for `aiLibraryId`](0059-use-cuid2-for-ai-library-id.md).
- [ADR-0060: Use JSON to serialize AI chat messages](0060-use-json-to-serialize-ai-messages.md).
- [ADR-0086: Offer JabRef to external AI agents via an HTTP endpoint with a skill and via MCP](0086-offer-jabref-to-external-ai-agents-via-http-skill-and-mcp.md).
