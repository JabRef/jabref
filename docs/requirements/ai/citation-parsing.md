---
parent: ai
---

# Citation parsing with LLMs
`feat~ai.citation-parsing~1`

Rationale: to enable the automatic extraction and identification of references within text using AI capabilities

Needs: impl, pp

Covers: `feat~ai~1`

## Allow customization of the system prompt for LLM citation parsing
`req~ai.citation-parsing.system-prompt-config~1`

Rationale: different citation styles or strictness levels require adjusting the baseline instructions (system prompt) given to the AI

Needs: impl

Covers: `feat~ai.citation-parsing~1`, `feat~ai.expert-settings~1`

<!-- markdownlint-disable-file MD022 -->
