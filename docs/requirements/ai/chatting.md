---
parent: AI
grand_parent: Requirements
---

# Chat with AI
`feat~ai.chatting~1`

This feature represents the AI chat, which can be a chat with an entry or a group.

Needs: impl, feat, req

## General AI chat requirements

Common functionalities required across all chat modes (single entry or group) to ensure a standard user experience.

### User can delete messages in AI chat
`feat~ai.chat.delete-messages~1`

User can remove specific messages to clean up the conversation or correct context.

Needs: impl, utest

Covers:

- feat~ai.chatting~1

### User can regenerate AI responses in AI chat
`feat~ai.chat.regenerate-response~1`

User may want a different answer if the previous one was unsatisfactory or hallucinated.

Needs: impl, utest

Covers:

- feat~ai.chatting~1

### User can use smart prompt input field in AI chat
`feat~ai.chat.smart-prompt-field~1`

Input field supports multi-line input, auto-resizing, keyboard shortcuts, and history.

Needs: impl

Covers:

- feat~ai.chatting~1

### User can clear AI chat history
`feat~ai.chat.clear-history~1`

User can reset the context completely and start a fresh conversation without previous biases.

Needs: impl, guard, utest

Covers:

- feat~ai.chatting~1

### User can see ingestion status in AI chat
`feat~ai.chat.ingestion-status~1`

User needs to know if the context files are fully indexed/embedded.

Needs: impl

Covers:

- feat~ai.chatting~1

### AI chat must display current AI model
`req~ai.chat.model-visibility~1`

Provides transparency regarding which LLM is generating the text.

Needs: impl

Covers:

- feat~ai.chatting~1

### User can cancel AI response generation in AI chat
`feat~ai.chat.cancel-generation~1`

Saves resources/tokens and time if the user realizes the prompt was incorrect while the answer is streaming.

Needs: impl

Covers:

- feat~ai.chatting~1

### User can see errors in AI chat
`feat~ai.chat.show-errors~1`

Feedback must be provided within the AI chat interface if the API fails, the network drops, or rate limits are hit.

Needs: impl

Covers:

- feat~ai.chatting~1

### User can retry AI response generation after error
`feat~ai.chat.retry-error~1`

Provides a quick way to re-attempt the request without re-typing the prompt if the failure was transient.

Needs: impl

Covers:

- feat~ai.chatting~1

### User can cancel AI response generation after error
`feat~ai.chat.cancel-error-state~1`

User can dismiss the error state or stop a retry loop to regain control of the interface.

Needs: impl

Covers:

- feat~ai.chatting~1

### User can customize system prompt in AI chat
`feat~ai.chat.customize-system-prompt~1`

User can modify the AI behavior by changing the system prompt to better suit their needs.

Needs: impl

Covers:

- feat~ai.chatting~1

### AI chat must use a response engine for query context
`req~ai.chat.uses-response-engine~1`

This requirement ensures that the AI has context to answer a question.

Needs: impl

Covers:

- feat~ai.chatting~1

### User can jump to entry PDF from citation links in AI chat
`feat~ai.chat.jump-to-entry-pdf~1`

Clicking a citation link in the format `[{key}](entries/{key}#page={page})` jumps to the entry with the citation key `{key}` and opens the internal PDF viewer at page `{page}`.

The link is the REST API path of the entry relative to the library (`/libraries/{id}/entries/{key}`), so the absolute form `jabref://libraries/{id}/entries/{key}` addresses an entry in any open library.
An optional `/files/{n}` segment selects the `{n}`-th linked file (1-based); without it, the first PDF is opened.
The page is given as a `#page={page}` fragment, following the PDF Open Parameters known from Acrobat.

Needs: impl, utest

Covers:

- feat~ai.chatting~1

## AI chat with entries
`feat~ai.chatting.entries~1`

Specific requirements for chatting with a single bibliography entry.

Needs: impl, pp, feat, req

Covers:

- feat~ai.chatting~1

### User can hide AI chat tab
`feat~ai.chat.entries.hide-tab~1`

User can declutter their interface if they do not use AI features.

Needs: impl

Covers:

- feat~ai.chatting.entries~1

### Entry AI chat history must be persisted across sessions
`req~ai.chat.entries.history-storage~1`

History must be persisted per entry, so the user can resume the conversation later.

Needs: dsn, model, impl, utest

Covers:

- feat~ai.chatting.entries~1

## AI chat with groups
`feat~ai.chatting.groups~1`

Specific requirements for chatting with a collection/group of entries simultaneously.

Needs: impl, pp, feat, req

Covers:

- feat~ai.chatting~1

### User can hide group AI chat context menu entry
`feat~ai.chat.groups.hide-context-menu~1`

User can customize the context menu to remove "Chat with group" if they do not use it.

Needs: impl

Covers:

- feat~ai.chatting.groups~1

### Group AI chat history must be persisted across sessions
`req~ai.chat.groups.history-storage~1`

History must be persisted per group, so the conversation context is preserved across sessions.

Needs: dsn, model, impl, utest

Covers:

- feat~ai.chatting.groups~1

### AI group chat dialog title must display library and group names
`req~ai.chat.groups.display-names~1`

Essential for user orientation, ensuring that users can distinguish between different AI chats of a group that has the same name in different libraries.

Needs: impl

Covers:

- feat~ai.chatting.groups~1

<!-- markdownlint-disable-file MD022 -->
