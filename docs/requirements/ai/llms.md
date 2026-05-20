---
parent: ai
---

# LLMs in AI features
`feat~ai.llms~1`

Rationale: to provide the core connectivity and abstraction layer for interacting with various Large Language Model backends

Needs: impl

Covers: `feat~ai~1`

## Support different LLM providers
`feat~ai.llms.providers~1`

Rationale: different providers offer varying trade-offs between cost, performance, privacy, and reasoning capabilities

Needs: impl, uman

Covers: `feat~ai.llms~1`

### Support OpenAI LLM provider
`req~ai.llms.providers.openai~1`

Rationale: popular and widely used LLM provider

Needs: impl

Covers: `feat~ai.llms.providers~1`

### Support HuggingFace LLM provider
`req~ai.llms.providers.huggingface~1`

Rationale: access to a wide variety of open-weight models and community contributions

Needs: impl

Covers: `feat~ai.llms.providers~1`

### Support Google Gemini LLM provider
`req~ai.llms.providers.gemini~1`

Rationale: popular and widely used LLM provider

Needs: impl

Covers: `feat~ai.llms.providers~1`

### Support Mistral LLM provider
`req~ai.llms.providers.mistral~1`

Rationale: popular LLM provider

Needs: impl

Covers: `feat~ai.llms.providers~1`

## Support local and custom LLM connections
`feat~ai.llms.custom~1`

Rationale: allows users to connect to self-hosted models or proxy services, ensuring data privacy and cost control

Needs: impl, uman

Covers: `feat~ai.llms~1`, `feat~ai.expert-settings~1`

### Add OpenAI-compatible provider
`req~ai.llms.custom.openai-compatible~1`

Rationale: many local inference servers (e.g., vLLM, Ollama) use the OpenAI API schema, making this a universal connector for local AI

Needs: impl

Covers: `feat~ai.llms.custom~1`

### Add customizable API base URL for OpenAI-compatible provider
`req~ai.llms.custom.base-url~1`

Rationale: users need to point the client to their specific local server address (e.g., `localhost:8000`) or a private enterprise proxy

Needs: impl

Covers: `feat~ai.llms.custom~1`

<!-- markdownlint-disable-file MD022 -->