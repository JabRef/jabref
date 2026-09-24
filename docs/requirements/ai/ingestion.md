---
parent: AI
grand_parent: Requirements
---

# Ingestion
`feat~ai.ingestion~1`

Processes and indexes document content into a format suitable for retrieval and AI context generation.

Needs: feat, req

## JabRef must support ingestion of PDF files
`req~ai.ingestion.pdf-handling~1`

JabRef extracts and processes text from linked PDF files during ingestion.

Rationale:

PDF is the standard format for academic research papers.

Needs: impl

Covers:

- feat~ai.ingestion~1

## User can trigger file ingestion on demand
`feat~ai.ingestion.trigger-on-demand~1`

When a person chats with an entry or group, the system must ensure the linked files are processed immediately to provide up-to-date context.

Needs: impl, pp

Covers:

- feat~ai.ingestion~1

## User can enable automatic file ingestion
`feat~ai.ingestion.automatic-trigger~1`

User may prefer files to be indexed in the background immediately upon upload to reduce wait times during AI chat interactions.

Needs: impl

Covers:

- feat~ai.ingestion~1

## User can clear embedding cache
`feat~ai.ingestion.clear-cache~1`

User needs to force a re-ingestion of documents if parsing logic changes or to free up storage space.

Needs: impl

Covers:

- feat~ai.ingestion~1

## Stored embeddings must be invalidated and regenerated when embedding model changes
`req~ai.ingestion.model-change-invalidation~1`

When the effective embedding model differs from the one the stored embeddings were generated with (e.g., after an update changed the default model, or after toggling expert settings), the stored embeddings are removed so that files are ingested again.

Rationale:

Embeddings of different models are not comparable and cannot be mixed in the same vector index.

Needs: impl, utest

Covers:

- feat~ai.ingestion~1

<!-- markdownlint-disable-file MD022 -->