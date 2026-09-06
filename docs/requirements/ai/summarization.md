---
parent: AI
grand_parent: Requirements
---

# Summarization with LLMs
`feat~ai.summarization~1`

Provides capabilities for distilling large amounts of text into concise summaries using LLMs.

Needs: model

## General AI summarization requirements

Basic functional requirements that apply to all summarization activities regardless of the specific algorithm.

### Documents of any size can be summarized
`req~ai.summarization.general.unlimited-size~1`

Users upload documents of varying lengths, from single pages to books, and the system must process them without hitting context window limits.

Needs: impl

### User can export AI summaries
`feat~ai.summarization.general.export~1`

User would want to access a summary offline, or use it in some other program.

Needs: impl

### AI summaries are preserved
`req~ai.summarization.general.storage~1`

Needs: impl, utest, dsn

## AI summarization of entries
`feat~ai.summarization.entries~1`

Specific functionality related to the summarization of database entries or document records.

Needs: impl, pp

### User can enable automatic AI summarization of new entries
`feat~ai.summarization.entries.auto~1`

User may wish to automatically generate the summaries for new entries in a library.

Needs: impl, pp

## AI summarization algorithms
`feat~ai.summarization.algorithms~1`

Distinct strategies for processing text, necessary because different document lengths require different architectural approaches (e.g. single pass vs map-reduce).

Needs: impl

### User can select default AI summarization algorithm
`feat~ai.summarization.algorithm.default~1`

Needs: impl

### "Chunked" AI summarization algorithm
`feat~ai.summarization.algorithms.chunked~1`

A strategy for large documents that splits text into pieces, summarizes them individually, and then combines the results.

Needs: impl

Reference: simplified version of the algorithm described in <https://arxiv.org/abs/2109.10862>

#### User can customize system prompt for chunking in AI summarization
`feat~ai.summarization.algorithms.chunked.system-prompt-chunk~1`

User needs to adjust the underlying prompt structures to refine AI outputs.

Needs: impl

#### User can customize system prompt for combining in AI summarization
`feat~ai.summarization.algorithms.chunked.system-prompt-combine~1`

User needs to adjust the underlying prompt structures to refine AI outputs.

Needs: impl

### "Full document" AI summarization algorithm
`feat~ai.summarization.algorithms.full~1`

A strategy for short documents that fit entirely within the LLM's context window, allowing for a single-pass summary.

Needs: impl

Reference: <https://arxiv.org/abs/2307.03172>

#### User can customize system prompt for full document AI summarization
`feat~ai.summarization.algorithms.full.system-prompt~1`

User needs to adjust the underlying prompt structures to refine AI outputs.

Needs: impl

<!-- markdownlint-disable-file MD022 -->