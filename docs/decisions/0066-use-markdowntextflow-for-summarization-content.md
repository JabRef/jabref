---
nav_order: 0066
parent: Decision Records
status: "accepted; supersedes ADR-0042"
---

# Use `MarkdownTextFlow` for Summarization Content

## Context and Problem Statement

This decision record concerns the UI component that is used for rendering the content of AI summaries.

[ADR-0042](0042-use-webview-for-summarization-content.md) chose a `WebView` for this purpose.
At that time, a `WebView` was the only considered option that could both render Markdown and let the user select and copy text.
Chat messages, in contrast, are rendered with a Markdown parser whose AST is turned into JavaFX `TextFlow` nodes ([ADR-0036](0036-use-markdown-for-chat-content.md)).

Since then, `SelectableTextFlow` gained text selection and copying, and the AST-to-`TextFlow` renderer used for chat messages was extracted into the reusable `MarkdownTextFlow` component.
The premise that summaries need a different, heavier rendering path than chat messages therefore no longer holds.

Which component should render the summary content?

## Decision Drivers

Same as in [ADR-0036](0036-use-markdown-for-chat-content.md):

* Looks good (renders Markdown)
* User can select and copy text
* Has good performance

In addition:

* Consistency with how chat messages are rendered

## Considered Options

Same as in [ADR-0036](0036-use-markdown-for-chat-content.md).

## Decision Outcome

Chosen option: "Use `MarkdownTextFlow`", the same Markdown-to-`TextFlow` component already used for chat messages.

`SelectableTextFlow` now supports selecting and copying text, which was the sole reason [ADR-0042](0042-use-webview-for-summarization-content.md) preferred a `WebView` over the chat rendering approach.
With that gap closed, reusing `MarkdownTextFlow` renders summaries and chat messages through a single code path, giving identical formatting and selection behavior across both surfaces and removing the summary-specific HTML rendering path.

The performance argument that [ADR-0036](0036-use-markdown-for-chat-content.md) raised against `WebView` never applied to summaries: there is only one summary content pane in the UI, and switching entries rebinds that single pane instead of adding components. A single `MarkdownTextFlow` is therefore unproblematic here as well.

### Consequences

* Good, because summary and chat content render and behave consistently, using one component.
* Good, because no separate Markdown-to-HTML rendering path has to be maintained for summaries.
* Neutral, because the summary pane is a single instance, so the per-message performance concerns from [ADR-0036](0036-use-markdown-for-chat-content.md) do not arise.

## Pros and Cons of the Options

Same as in [ADR-0036](0036-use-markdown-for-chat-content.md).

## More Information

This ADR supersedes [ADR-0042](0042-use-webview-for-summarization-content.md) and is highly linked to [ADR-0036](0036-use-markdown-for-chat-content.md).
