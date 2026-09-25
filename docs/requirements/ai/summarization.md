---
parent: AI
grand_parent: Requirements
---

# Summarization with LLMs
`feat~ai.summarization~1`

Provides capabilities for distilling large amounts of text into concise summaries using LLMs.

Needs: model, feat, req

## General AI summarization requirements

Basic functional requirements that apply to all summarization activities regardless of the specific algorithm.

### AI summarization must handle documents of any size
`req~ai.summarization.general.unlimited-size~1`

Users upload documents of varying lengths, from single pages to books, and the system must process them without hitting context window limits.

Needs: impl

Covers:

- feat~ai.summarization~1

### User can export AI summaries
`feat~ai.summarization.general.export~1`

User would want to access a summary offline, or use it in some other program.

Needs: impl

Covers:

- feat~ai.summarization~1

### AI summaries must be preserved
`req~ai.summarization.general.storage~1`

Needs: impl, utest, dsn

Covers:

- feat~ai.summarization~1

## AI summarization of entries
`feat~ai.summarization.entries~1`

Specific functionality related to the summarization of database entries or document records.

Needs: impl, pp, feat

Covers:

- feat~ai.summarization~1

### User can enable automatic AI summarization of new entries
`feat~ai.summarization.entries.auto~1`

User may wish to automatically generate the summaries for new entries in a library.

Needs: impl, pp

Covers:

- feat~ai.summarization.entries~1

## AI summarization algorithms
`feat~ai.summarization.algorithms~1`

Distinct strategies for processing text, necessary because different document lengths require different architectural approaches (e.g. single pass vs map-reduce).

Needs: impl, feat

Covers:

- feat~ai.summarization~1

### User can select default AI summarization algorithm
`feat~ai.summarization.algorithm.default~1`

Needs: impl

Covers:

- feat~ai.summarization.algorithms~1

### User can summarize large documents using chunked algorithm
`feat~ai.summarization.algorithms.chunked~1`

A strategy for large documents that splits text into pieces, summarizes them individually, and then combines the results.

Needs: impl, feat

Reference: simplified version of the algorithm described in <https://arxiv.org/abs/2109.10862>

Covers:

- feat~ai.summarization.algorithms~1

#### User can customize system prompt for chunking in AI summarization
`feat~ai.summarization.algorithms.chunked.system-prompt-chunk~1`

User needs to adjust the underlying prompt structures to refine AI outputs.

Needs: impl

Covers:

- feat~ai.summarization.algorithms.chunked~1

#### User can customize system prompt for combining in AI summarization
`feat~ai.summarization.algorithms.chunked.system-prompt-combine~1`

User needs to adjust the underlying prompt structures to refine AI outputs.

Needs: impl

Covers:

- feat~ai.summarization.algorithms.chunked~1

### User can summarize short documents using full document algorithm
`feat~ai.summarization.algorithms.full~1`

A strategy for short documents that fit entirely within the LLM's context window, allowing for a single-pass summary.

Needs: impl, feat

Reference: <https://arxiv.org/abs/2307.03172>

Covers:

- feat~ai.summarization.algorithms~1

#### User can customize system prompt for full document AI summarization
`feat~ai.summarization.algorithms.full.system-prompt~1`

User needs to adjust the underlying prompt structures to refine AI outputs.

Needs: impl

Covers:

- feat~ai.summarization.algorithms.full~1

<!-- markdownlint-disable-file MD022 -->