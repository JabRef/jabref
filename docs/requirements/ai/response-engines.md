---
parent: AI
grand_parent: Requirements
---

# Different response engines for AI chat
`feat~ai.response-engines~1`

Response engine is an algorithm that supplies the context for LLM.

Different response engines are suitable for different tasks.

Needs: impl, feat

## User can select default AI response engine
`feat~ai.response-engines.default~1`

Needs: impl

Covers:

- feat~ai.response-engines~1

## User can query AI chat using embedding search response engine
`feat~ai.response-engines.embeddings-search~1`

This response engine is suitable when the user wants to perform a semantic search.

Reference: <https://arxiv.org/abs/2005.11401>

Needs: impl, dsn, feat

Covers:

- feat~ai.response-engines~1

### User can customize injection prompt for embedding search AI response engine
`feat~ai.response-engines.embeddings-search.prompt~1`

Different prompts are suited for different tasks and affect the LLM output.

Needs: impl

Covers:

- feat~ai.response-engines.embeddings-search~1

## User can query AI chat using full document response engine
`feat~ai.response-engines.full-document~1`

This response engine is suitable when the user wants to get information that depends on the full content of a document.

Needs: impl, feat

Reference: <https://arxiv.org/abs/2407.16833>

Covers:

- feat~ai.response-engines~1

### User can customize injection prompt for full document AI response engine
`feat~ai.response-engines.full-document.prompt~1`

Different prompts are suited for different tasks and affect the LLM output.

Needs: impl

Covers:

- feat~ai.response-engines.full-document~1

<!-- markdownlint-disable-file MD022 -->
