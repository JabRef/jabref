---
nav_order: 0042
parent: Decision Records
status: "accepted"
---

# Use `MarkdownTextFlow` for Summarization Content

## Context and Problem Statement

This decision record concerns the UI component that is used for rendering the content of AI summaries.

This ADR previously chose a `WebView` for this purpose.
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

Chosen option: "Use `MarkdownTextFlow`", because it renders summaries through the same Markdown-to-`TextFlow` component already used for chat messages, giving both surfaces a single code path.

`SelectableTextFlow` now supports selecting and copying text, which was the sole reason this ADR originally preferred a `WebView` over the chat rendering approach.
With that gap closed, reusing `MarkdownTextFlow` gives identical formatting and selection behavior across summaries and chat messages and removes the summary-specific HTML rendering path.

The performance argument that [ADR-0036](0036-use-markdown-for-chat-content.md) raised against `WebView` never applied to summaries: there is only one summary content pane in the UI, and switching entries rebinds that single pane instead of adding components. A single `MarkdownTextFlow` is therefore unproblematic here as well.

### Consequences

* Good, because summary and chat content render and behave consistently, using one component.
* Good, because no separate Markdown-to-HTML rendering path has to be maintained for summaries.
* Neutral, because the summary pane is a single instance, so the per-message performance concerns from [ADR-0036](0036-use-markdown-for-chat-content.md) do not arise.

## Pros and Cons of the Options

Same as in [ADR-0036](0036-use-markdown-for-chat-content.md).

## More Information

This ADR is highly linked to [ADR-0036](0036-use-markdown-for-chat-content.md).
