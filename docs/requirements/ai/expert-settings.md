---
parent: AI
grand_parent: Requirements
---

# AI expert settings

Provides advanced configuration options for controlling AI behavior and defaults across the application.

## User can modify AI templates in expert settings
`feat~ai.expert-settings.templates~1`

User needs to adjust the underlying prompt structures to refine AI outputs and behavior patterns.

Needs: impl

## User can modify global AI chat inference parameters
`feat~ai.expert-settings.chat-inference-global~1`

User needs to adjust the underlying settings of the inference to refine AI outputs and behavior patterns.

Needs: impl

## User can modify global RAG parameters
`feat~ai.expert-settings.rag-global~1`

User needs to adjust the RAG parameters to refine AI outputs.

Needs: impl

## User can modify local AI summarization parameters
`feat~ai.expert-settings.summarization-local~1`

User needs to adjust the underlying prompt structures to refine AI outputs.

Needs: impl

## Dynamic discovery of embedding model download size
`feat~ai.expert-settings.embedding-model-size~1`

The download size of an embedding model is discovered dynamically at runtime.

Needs: impl

## Dynamic discovery of embedding model maximum snippet length
`feat~ai.expert-settings.embedding-model-token-limit~1`

The maximum snippet length in tokens for an embedding model is discovered dynamically at runtime.

Needs: impl

## Default local embedding model covers a whole document piece
`req~ai.expert-settings.default-embedding-model~1`

The default local embedding model reads at least as many tokens as the default document piece size, so no text of a piece is cut off before embedding. `BAAI/bge-small-en-v1.5` reads 512 tokens, the default piece size is 300.

Needs: impl

<!-- markdownlint-disable-file MD022 -->
