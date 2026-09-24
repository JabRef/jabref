---
parent: AI
grand_parent: Requirements
---

# Citation parsing with LLMs
`feat~ai.citation-parsing~1`

Enables the automatic extraction and identification of references within text using AI capabilities.

Needs: impl, pp, feat, req

## User can customize system prompt for LLM citation parsing
`feat~ai.citation-parsing.system-prompt-config~1`

Different citation styles or strictness levels require adjusting the baseline instructions (system prompt) given to the AI.

Needs: impl

Covers:

- feat~ai.citation-parsing~1

## LLM citation parsing must run in background without blocking new entry dialog
`req~ai.citation-parsing.background~1`

An LLM can take long to answer. The "New Entry" dialog closes immediately and the parsed entries are added to the library once the answer arrives.

Needs: impl

Covers:

- feat~ai.citation-parsing~1

<!-- markdownlint-disable-file MD022 -->
