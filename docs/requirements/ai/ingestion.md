---
parent: ai
---

# Ingestion

Processes and indexes document content into a format suitable for retrieval and AI context generation.

## Support handling of PDF files during ingestion
`req~ai.ingestion.pdf-handling~1`

PDF is a de-facto standard for academic documents.

Needs: impl

## Trigger ingestion of files on demand
`req~ai.ingestion.trigger-on-demand~1`

When a person chats with an entry or group, the system must ensure the linked files are processed immediately to provide up-to-date context.

Needs: impl, pp

## Add automatic ingestion of files
`req~ai.ingestion.automatic-trigger~1`

Users may prefer files to be indexed in the background immediately upon upload to reduce wait times during chat interactions.

Needs: impl

## Allow clearing of the embedding cache
`req~ai.ingestion.clear-cache~1`

Users need to force a re-ingestion of documents if parsing logic changes or to free up storage space.

Needs: impl

<!-- markdownlint-disable-file MD022 -->