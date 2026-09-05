---
parent: AI
grand_parent: Requirements
---

<!-- oft:off -->

# Future features
`feat~ai.future~1`

Captures upcoming enhancements and architectural refactoring for the AI system.

Needs: impl

Status: draft

## User can change LLM in AI chat
`feat~ai.chatting.llm-selection~1`

User may prefer specific models for conversation based on cost, speed, or reasoning capability.

Needs: impl

Status: draft

## User can change LLM for AI summarization
`feat~ai.summarization.llm-selection~1`

Summarization tasks may require different model strengths or token limits compared to interactive AI chat.

Needs: impl

Status: draft

## User can edit messages in AI chat
`feat~ai.chatting.user-message-editing~1`

User needs to correct typos or refine their queries without restarting the entire conversation context.

Needs: impl

Status: draft

## User can create AI profiles
`feat~ai.chatting.ai-profiles~1`

Currently it is hard to test other AI chat model in an AI chat, because the model setting is global and only one.

Needs: impl, dsn, utest

Status: draft

## User can modify local RAG parameters
`feat~ai.expert-settings.rag-local~1`

User needs to adjust the RAG parameters to refine AI outputs.

Needs: impl

Status: draft

<!-- oft:on -->

<!-- markdownlint-disable-file MD022 -->