---
parent: AI
grand_parent: Requirements
---

# Ingestion

Processes and indexes document content into a format suitable for retrieval and AI context generation.

## PDF files can be ingested
`req~ai.ingestion.pdf-handling~1`

PDF is a de-facto standard for academic documents.

Needs: impl

## User can trigger file ingestion on demand
`feat~ai.ingestion.trigger-on-demand~1`

When a person chats with an entry or group, the system must ensure the linked files are processed immediately to provide up-to-date context.

Needs: impl, pp

## User can enable automatic file ingestion
`feat~ai.ingestion.automatic-trigger~1`

User may prefer files to be indexed in the background immediately upon upload to reduce wait times during AI chat interactions.

Needs: impl

## User can clear embedding cache
`feat~ai.ingestion.clear-cache~1`

User needs to force a re-ingestion of documents if parsing logic changes or to free up storage space.

Needs: impl

<!-- markdownlint-disable-file MD022 -->