---
parent: AI
grand_parent: Requirements
---

# LLMs in AI features
`feat~ai.llms~1`

Provides the core connectivity and abstraction layer for interacting with various Large Language Model backends.

Needs: impl

## User can select from different LLM providers
`feat~ai.llms.providers~1`

Different providers offer varying trade-offs between cost, performance, privacy, and reasoning capabilities.

Needs: impl

### OpenAI LLM provider is supported
`req~ai.llms.providers.openai~1`

OpenAI is a popular and widely used LLM provider.

Needs: impl

### HuggingFace LLM provider is supported
`req~ai.llms.providers.huggingface~1`

HuggingFace provides access to a wide variety of open-weight models and community contributions.

Needs: impl

### Google Gemini LLM provider is supported
`req~ai.llms.providers.gemini~1`

Google Gemini is a popular and widely used LLM provider.

Needs: impl

### Mistral LLM provider is supported
`req~ai.llms.providers.mistral~1`

Mistral is a popular LLM provider.

Needs: impl

## User can connect to local and custom LLMs
`feat~ai.llms.custom~1`

Allows users to connect to self-hosted models or proxy services, ensuring data privacy and cost control.

Needs: impl

### OpenAI-compatible provider is available
`req~ai.llms.custom.openai-compatible~1`

Many local inference servers (e.g., vLLM, Ollama) use the OpenAI API schema, making this a universal connector for local AI.

Needs: impl

### User can configure API base URL for OpenAI-compatible provider
`req~ai.llms.custom.base-url~1`

User needs to point the client to their specific local server address (e.g., `localhost:8000`) or a private enterprise proxy.

Needs: impl

<!-- markdownlint-disable-file MD022 -->