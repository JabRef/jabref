---
parent: ai
---

# Chat with AI
`feat~ai.chatting~1`

This feature represents the AI chat, which can be a chat with an entry or a group.

Needs: impl

## General AI chat requirements

Common functionalities required across all chat modes (single entry or group) to ensure a standard user experience.

### Support deletion of messages in AI chat
`req~ai.chat.delete-messages~1`

Users should be able to remove specific messages to clean up the conversation or correct context

Needs: impl, utest

### Support regeneration of AI responses in AI chat
`req~ai.chat.regenerate-response~1`

Users may want a different answer if the previous one was unsatisfactory or hallucinated.

Needs: impl, utest

### Provide a smart prompt input field in AI chat
`req~ai.chat.smart-prompt-field~1`

The input field should support multi-line input, auto-resizing, keyboard shortcuts, and history.

Needs: impl

### Support clearing of chat history in AI chat
`req~ai.chat.clear-history~1`

Allows the user to reset the context completely and start a fresh conversation without previous biases.

Needs: impl, guard, utest

### Display the status of ingested files in AI chat
`req~ai.chat.ingestion-status~1`

The user needs to know if the context files are fully indexed/embedded.

Needs: impl

### Display the currently used AI model in AI chat
`req~ai.chat.model-visibility~1`

Provides transparency regarding which LLM is generating the text.

Needs: impl

### Allow user to cancel AI response generation in AI chat
`req~ai.chat.cancel-generation~1`

Saves resources/tokens and time if the user realizes the prompt was incorrect while the answer is streaming.

Needs: impl

### Display errors in AI chat
`req~ai.chat.show-errors~1`

Feedback must be provided within the chat interface if the API fails, the network drops, or rate limits are hit.

Needs: impl

### Support retry of AI response generation after error in AI chat
`req~ai.chat.retry-error~1`

Provides a quick way to re-attempt the request without re-typing the prompt if the failure was transient.

Needs: impl

### Allow user to cancel AI response generation after an error in AI chat
`req~ai.chat.cancel-error-state~1`

Allows the user to dismiss the error state or stop a retry loop to regain control of the interface.

Needs: impl

### Support customization of the system prompt in AI chat
`req~ai.chat.customize-system-prompt~1`

Users should be able to modify the AI behavior by changing the system prompt to better suit their needs.

Needs: impl

### Ensure that a response engine is used in AI chat
`req~ai.chat.uses-response-engine~1`

This requirement ensures that the AI has context to answer a question.

Needs: impl

## AI chat with entries
`feat~ai.chatting.entries~1`

Specific requirements for chatting with a single bibliography entry.

Needs: impl, pp

### Support hiding of the AI chat tab
`req~ai.chat.entries.hide-tab~1`

Users who do not use AI features should be able to declutter their interface.

Needs: impl

### Persist AI chat history for AI chat with entries
`req~ai.chat.entries.history-storage~1`

History must be persisted per entry, so the user can resume the conversation later.

Needs: dsn, model, impl, utest

## AI chat with groups
`feat~ai.chatting.groups~1`

Specific requirements for chatting with a collection/group of entries simultaneously.

Needs: impl, pp

### Support hiding of the context menu entry for AI chat with group
`req~ai.chat.groups.hide-context-menu~1`

Allows customization of the context menu to remove "Chat with group" if the user does not use it.

Needs: impl

### Persist AI chat history for AI chat with groups
`req~ai.chat.groups.history-storage~1`

History must be persisted per group, so the conversation context is preserved across sessions.

Needs: dsn, model, impl, utest

### Display library name and group name in AI group chat
`req~ai.chat.groups.display-names~1`

Essential for user orientation, ensuring that users can distinguish between different chats of a group that has the same name in different libraries.

Needs: impl

<!-- markdownlint-disable-file MD022 -->
