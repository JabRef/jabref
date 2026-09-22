---
parent: AI
grand_parent: Requirements
---

# Citation parsing with LLMs
`feat~ai.citation-parsing~1`

Enables the automatic extraction and identification of references within text using AI capabilities.

Needs: impl, pp

## User can customize system prompt for LLM citation parsing
`feat~ai.citation-parsing.system-prompt-config~1`

Different citation styles or strictness levels require adjusting the baseline instructions (system prompt) given to the AI.

Needs: impl

## New entry dialog does not wait for LLM citation parsing
`req~ai.citation-parsing.background~1`

An LLM can take long to answer. The "New Entry" dialog closes immediately and the parsed entries are added to the library once the answer arrives.

Needs: impl

<!-- markdownlint-disable-file MD022 -->
