---
parent: Requirements
---

# AI

## Features

- [General](./ai/general.md)
- [Chatting](./ai/chatting.md)
- [Response engines](./ai/response-engines.md)
- [Summarization](./ai/summarization.md)
- [Citation parsing](./ai/citation-parsing.md)
- [Ingestion](./ai/ingestion.md)
- [Expert settings](./ai/expert-settings.md)
- [LLMs](./ai/llms.md)
- [Future features](./ai/future.md)

## How to write AI Requirements

AI requirements follow the general guidelines in [Requirements Guide](../code-howtos/requirements.md):

1. For a major AI feature area, create a separate document in `docs/requirements/ai/` (such as `chatting.md` or `summarization.md`).
2. Group requirements logically using Markdown headings.
3. Formulate titles according to the artifact type:
   - For `req~`, use **"Subject must verb"** (always modal verb `must`, subject first, avoiding passive voice and nominalizations).
   - For `feat~`, use **"User can verb"** to describe user capabilities.
   Titles are displayed prominently in OFT reports and should be clear and descriptive even in isolation.
4. Link detailed requirements back to their parent feature using the `Covers:` keyword:

   ```markdown
   Covers:

   - feat~ai.chatting~1
   ```

5. Because OFT cannot link directly inside FXML files, place linking comments for UI views in the corresponding Java controller (`View` in MVVM).

<!-- markdownlint-disable-file MD022 -->
