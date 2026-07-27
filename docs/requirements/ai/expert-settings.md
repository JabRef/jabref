---
parent: ai
---

# AI expert settings
`feat~ai.expert-settings~1`

Rationale: to provide advanced configuration options for controlling AI behavior and defaults across the application

Covers: `feat~ai~1`

## Allow modification of AI templates in AI expert settings
`req~ai.expert-settings.templates~1`

Rationale: users need to adjust the underlying prompt structures to refine AI outputs and behavior patterns

Needs: impl

Covers: `feat~ai.expert-settings~1`

## Allow modification of global chat inference parameters in AI expert settings
`req~ai.expert-settings.chat-inference-global~1`

Rationale: users need to adjust the underlying settings of the inference to refine AI outputs and behavior patterns

Needs: impl

Covers: `feat~ai.expert-settings~1`

## Allow modification of global RAG parameters in AI expert settings
`req~ai.expert-settings.rag-global~1`

Rationale: users need to adjust the RAG parameters to refine AI outputs

Needs: impl

Covers: `feat~ai.expert-settings~1`

## Allow modification of local summarization parameters in AI expert settings
`req~ai.expert-settings.summarization-local~1`

Rationale: users need to adjust the underlying prompt structures to refine AI outputs

Needs: impl

Covers: `feat~ai.expert-settings~1`

<!-- markdownlint-disable-file MD022 -->
