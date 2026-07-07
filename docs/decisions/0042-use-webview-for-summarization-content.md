---
nav_order: 0042
parent: Decision Records
status: "superseded by ADR-0066"
---

# Use `WebView` for Summarization Content

> [!NOTE]
> This decision is **superseded by [ADR-0066](0066-use-markdowntextflow-for-summarization-content.md)**.
> `SelectableTextFlow` (see [ADR-0036](0036-use-markdown-for-chat-content.md)) now supports text selection and copying, removing the reason this ADR gave for rendering summaries with a `WebView` rather than reusing the chat message component. The AI summary tab now renders Markdown with `MarkdownTextFlow`, the same component used for chat messages.

## Context and Problem Statement

This decision record concerns the UI component that is used for rendering the content of AI summaries.

## Decision Drivers

Same as in [ADR-0036](./0036-use-markdown-for-chat-content.md).

## Considered Options

Same as in [ADR-0036](./0036-use-markdown-for-chat-content.md).

## Decision Outcome

Chosen option: "Use `WebView`", because it supports selecting and copying text as well as rendering Markdown, and there is only one summary content in the UI so performance is not a concern (see below).

Some of the options does not support selecting and copying of text. Some options do not render Markdown.

However, in contrary to [ADR-0036](./0036-use-markdown-for-chat-content.md), we chose here a `WebView`, instead of `TextArea`, because there is only one summary content in UI (when user switches entries, no new components are added, rather old ones are *rebinding* to new entry). It would hurt the performance if we used `WebView` for messages, as there could be a lot of messages in one chat.

## Pros and Cons of the Options

Same as in [ADR-0036](./0036-use-markdown-for-chat-content.md).

## More Information

This ADR is highly linked to [ADR-0036](./0036-use-markdown-for-chat-content.md).

About the selection and copying, this goes down to fundamental issue from JavaFX.
`Text` and `Label` as a whole or a part [cannot be selected and/or copied](https://bugs.openjdk.org/browse/JDK-8091644).
