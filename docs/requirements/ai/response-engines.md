---
parent: ai
---

# Different response engines for AI chat
`feat~ai.answer-engines~1`

Description: response engine is an algorithm that supplies the context for LLM

Rationale: different response engines are suitable for different tasks

Needs: impl

Covers: `feat~ai.chatting~1`

## Allow users to select a default response engine
`req~ai.answer-engines.default~1`

Needs: impl

Covers: `feat~ai.answer-engines~1`

## "Embedding search" AI response engine
`feat~ai.answer-engines.embeddings-search~1`

Rationale: this response engine is suitable when the user wants to perform a semantic search

Reference: <https://arxiv.org/abs/2005.11401>

Needs: impl, dsn

Covers: `feat~ai.answer-engines~1`

### Allow users to customize injection prompt for "embedding search" AI response engine
`req~ai.answer-engines.embeddings-search.prompt~1`

Rationale: different prompts are suited for different tasks and affect the LLM output

Needs: impl

Covers: `feat~ai.answer-engines.embeddings-search~1`, `feat~ai.expert-settings~1`

## "Full document" AI response engine
`feat~ai.answer-engines.full-document~1`

Rationale: this response engine is suitable when the user wants to get information that depends on the full content of a document

Needs: impl

Reference: <https://arxiv.org/abs/2407.16833>

Covers: `feat~ai.answer-engines~1`

### Allow users to customize injection prompt for "full document" AI response engine
`req~ai.answer-engines.full-document.prompt~1`

Rationale: different prompts are suited for different tasks and affect the LLM output

Needs: impl

Covers: `feat~ai.answer-engines.full-document~1`, `feat~ai.expert-settings~1`

<!-- markdownlint-disable-file MD022 -->

