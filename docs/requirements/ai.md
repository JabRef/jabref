---
parent: Requirements
---

# AI

## Features

- [Chatting](./ai/chatting.md)
- [Answer engines](./ai/answer-engines.md)
- [Summarization](./ai/summarization.md)
- [Citation parsing](./ai/citation-parsing.md)
- [Ingestion](./ai/ingestion.md)
- [Expert settings](./ai/expert-settings.md)
- [LLMs](./ai/llms.md)
- [Future features](./ai/future.md)

## How to write AI Requirements

Currently these rules are only applied in the AI features and are a bit experimental:

1. For a "big" AI feature, create a separate file (for example, chatting and summarization).
2. Use `feat` type to group requirements.
3. The requirement title should be a full sentence starting from a verb with all context (rationale: all requirements are displayed by their title in OFT reports, so even if the requirement is "grouped" under some feature using a `Tags:` or `Covers:` fields, they are still displayed separately. Full sentences allow to quickly understand what is this requirement and to which part of JabRef it is related to).
4. At the moment of writing (21-04-2024) OFT does not support linking in FXML files, so for such requirements write a link in the Java file of the FXML controller (which corresponds to the `View` in MVVM).
