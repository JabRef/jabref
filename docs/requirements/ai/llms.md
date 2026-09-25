---
parent: AI
grand_parent: Requirements
---

# LLMs in AI features
`feat~ai.llms~1`

Provides the core connectivity and abstraction layer for interacting with various Large Language Model backends.

Needs: impl, feat

## User can select from different LLM providers
`feat~ai.llms.providers~1`

Different providers offer varying trade-offs between cost, performance, privacy, and reasoning capabilities.

Needs: impl, req

Covers:

- feat~ai.llms~1

### JabRef must support OpenAI LLM provider
`req~ai.llms.providers.openai~1`

OpenAI is a popular and widely used LLM provider.

Needs: impl

Covers:

- feat~ai.llms.providers~1

### JabRef must support HuggingFace LLM provider
`req~ai.llms.providers.huggingface~1`

HuggingFace provides access to a wide variety of open-weight models and community contributions.

Needs: impl

Covers:

- feat~ai.llms.providers~1

### JabRef must support Google Gemini LLM provider
`req~ai.llms.providers.gemini~1`

Google Gemini is a popular and widely used LLM provider.

Needs: impl

Covers:

- feat~ai.llms.providers~1

### JabRef must support Mistral LLM provider
`req~ai.llms.providers.mistral~1`

Mistral is a popular LLM provider.

Needs: impl

Covers:

- feat~ai.llms.providers~1

## User can connect to local and custom LLMs
`feat~ai.llms.custom~1`

Allows users to connect to self-hosted models or proxy services, ensuring data privacy and cost control.

Needs: impl, req

Covers:

- feat~ai.llms~1

### JabRef must support OpenAI-compatible LLM provider
`req~ai.llms.custom.openai-compatible~1`

Many local inference servers (e.g., vLLM, Ollama) use the OpenAI API schema, making this a universal connector for local AI.

Needs: impl

Covers:

- feat~ai.llms.custom~1

### OpenAI-compatible LLM provider must allow configuring API base URL
`req~ai.llms.custom.base-url~1`

User needs to point the client to their specific local server address (e.g., `localhost:8000`) or a private enterprise proxy.

Needs: impl

Covers:

- feat~ai.llms.custom~1

### AI preferences must allow testing connection to configured provider
`req~ai.llms.test-connection~1`

User can check provider, chat model, API key, and API base URL entered in the preferences with a minimal chat request before saving them.

Needs: impl

Covers:

- feat~ai.llms.custom~1

### Base URL of LLM provider must be redacted in logs and dialogs
`req~ai.llms.base-url-redacted~1`

Base URL might contain secrets (username and password for connection), so they need to be removed in logs and dialogs.

Needs: impl, utest

Covers:

- feat~ai.llms.custom~1

<!-- markdownlint-disable-file MD022 -->
