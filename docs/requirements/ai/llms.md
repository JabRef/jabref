---
parent: ai
---

# LLMs in AI features
`feat~ai.llms~1`

Provides the core connectivity and abstraction layer for interacting with various Large Language Model backends.

Needs: impl

## Support different LLM providers
`feat~ai.llms.providers~1`

Different providers offer varying trade-offs between cost, performance, privacy, and reasoning capabilities.

Needs: impl, uman

### Support OpenAI LLM provider
`req~ai.llms.providers.openai~1`

OpenAI is a popular and widely used LLM provider.

Needs: impl

### Support HuggingFace LLM provider
`req~ai.llms.providers.huggingface~1`

HuggingFace provides access to a wide variety of open-weight models and community contributions.

Needs: impl

### Support Google Gemini LLM provider
`req~ai.llms.providers.gemini~1`

Google Gemini is a popular and widely used LLM provider.

Needs: impl

### Support Mistral LLM provider
`req~ai.llms.providers.mistral~1`

Mistral is a popular LLM provider.

Needs: impl

## Support local and custom LLM connections
`feat~ai.llms.custom~1`

Allows users to connect to self-hosted models or proxy services, ensuring data privacy and cost control.

Needs: impl, uman

### Add OpenAI-compatible provider
`req~ai.llms.custom.openai-compatible~1`

Many local inference servers (e.g., vLLM, Ollama) use the OpenAI API schema, making this a universal connector for local AI.

Needs: impl

### Add customizable API base URL for OpenAI-compatible provider
`req~ai.llms.custom.base-url~1`

Users need to point the client to their specific local server address (e.g., `localhost:8000`) or a private enterprise proxy.

Needs: impl

<!-- markdownlint-disable-file MD022 -->