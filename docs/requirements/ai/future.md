---
parent: ai
---

<!-- oft:off -->

# Future features
`feat~ai.future~1`

Rationale: to capture upcoming enhancements and architectural refactoring for the AI system

Needs: impl

Covers: `feat~ai~1`

## Allow modification of the LLM in AI chat
`req~ai.chatting.llm-selection~1`

Rationale: users may prefer specific models for conversation based on cost, speed, or reasoning capability

Needs: impl

Covers: `feat~ai.chatting~1`

## Allow modification of the LLM in AI summary
`req~ai.summarization.llm-selection~1`

Rationale: summarization tasks may require different model strengths or token limits compared to interactive chat

Needs: impl

Covers: `feat~ai.summarization~1`

## Support editing of user messages in AI chat
`req~ai.chatting.user-message-editing~1`

Rationale: users need to correct typos or refine their queries without restarting the entire conversation context

Needs: impl

Covers: `feat~ai.chatting~1`

## Introduce AI profiles
`req~ai.chatting.ai-profiles~1`

Rationale: currently it is hard to test other chat model in an AI chat, because the model setting is global and only one.

Needs: impl, dsn, utest

Covers: `feat~ai.chatting~1`

## Allow modification of local RAG parameters in AI expert settings
`req~ai.expert-settings.rag-local~1`

Rationale: users need to adjust the RAG parameters to refine AI outputs

Needs: impl

Covers: `feat~ai.expert-settings~1`

<!-- oft:on -->

<!-- markdownlint-disable-file MD022 -->