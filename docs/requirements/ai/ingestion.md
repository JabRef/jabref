---
parent: ai
---

# Ingestion
`feat~ai.ingestion~1`

Rationale: to process and index document content into a format suitable for retrieval and AI context generation

Covers: `feat~ai~1`

## Support handling of PDF files during ingestion
`req~ai.ingestion.pdf-handling~1`

Rationale: PDF is a de-facto standard for academic documents

Needs: impl

Covers: `feat~ai.ingestion~1`

## Trigger ingestion of files on demand
`req~ai.ingestion.trigger-on-demand~1`

Rationale: when a person chats with an entry or group, the system must ensure the linked files are processed immediately to provide up-to-date context

Needs: impl, pp

Covers: `feat~ai.ingestion~1`

## Add automatic ingestion of files
`req~ai.ingestion.automatic-trigger~1`

Rationale: users may prefer files to be indexed in the background immediately upon upload to reduce wait times during chat interactions

Needs: impl

Covers: `feat~ai.ingestion~1`

## Allow clearing of the embedding cache
`req~ai.ingestion.clear-cache~1`

Rationale: users need to force a re-ingestion of documents if parsing logic changes or to free up storage space

Needs: impl

Covers: `feat~ai.ingestion~1`

<!-- markdownlint-disable-file MD022 -->