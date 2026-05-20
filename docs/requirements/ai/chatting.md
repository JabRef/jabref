---
parent: ai
---

# Chat with AI
`feat~ai.chatting~1`

Description: this feature represents the AI chat, which can be a chat with an entry or a group.

Needs: impl

Covers: `feat~ai~1`

## General AI chat requirements
`feat~ai.chat.general~1`

Rationale: common functionalities are required across all chat modes (single entry or group) to ensure a standard user experience

Covers: `feat~ai.chatting~1`

### Support deletion of messages in AI chat
`req~ai.chat.delete-messages~1`

Rationale: users should be able to remove specific messages to clean up the conversation or correct context

Needs: impl, utest

Covers: `feat~ai.chat.general~1`

### Support regeneration of AI responses in AI chat
`req~ai.chat.regenerate-response~1`

Rationale: users may want a different answer if the previous one was unsatisfactory or hallucinated

Needs: impl, utest

Covers: `feat~ai.chat.general~1`

### Provide a smart prompt input field in AI chat
`req~ai.chat.smart-prompt-field~1`

Rationale: the input field should support multi-line input, auto-resizing, keyboard shortcuts, and history

Needs: impl

Covers: `feat~ai.chat.general~1`

### Support clearing of chat history in AI chat
`req~ai.chat.clear-history~1`

Rationale: allows the user to reset the context completely and start a fresh conversation without previous biases

Needs: impl, guard, utest

### Display the status of ingested files in AI chat
`req~ai.chat.ingestion-status~1`

Rationale: the user needs to know if the context files are fully indexed/embedded

Needs: impl

Covers: `feat~ai.chat.general~1`

### Display the currently used AI model in AI chat
`req~ai.chat.model-visibility~1`

Rationale: provides transparency regarding which LLM is generating the text

Needs: impl

Covers: `feat~ai.chat.general~1`

### Allow user to cancel AI response generation in AI chat
`req~ai.chat.cancel-generation~1`

Rationale: saves resources/tokens and time if the user realizes the prompt was incorrect while the answer is streaming

Needs: impl

Covers: `feat~ai.chat.general~1`

### Display errors in AI chat
`req~ai.chat.show-errors~1`

Rationale: feedback must be provided within the chat interface if the API fails, the network drops, or rate limits are hit

Needs: impl

Covers: `feat~ai.chat.general~1`

### Support retry of AI response generation after error in AI chat
`req~ai.chat.retry-error~1`

Rationale: provides a quick way to re-attempt the request without re-typing the prompt if the failure was transient

Needs: impl

Covers: `feat~ai.chat.general~1`

### Allow user to cancel AI response generation after an error in AI chat
`req~ai.chat.cancel-error-state~1`

Rationale: allows the user to dismiss the error state or stop a retry loop to regain control of the interface

Needs: impl

Covers: `feat~ai.chat.general~1`

### Support customization of the system prompt in AI chat
`req~ai.chat.customize-system-prompt~1`

Rationale: users should be able to modify the AI behavior by changing the system prompt to better suit their needs

Needs: impl

Covers: `feat~ai.chat.general~1`, `feat~ai.expert-settings~1`

### Ensure that an answer engine is used in AI chat
`req~ai.chat.uses-answer-engine~1`

Rationale: this requirement ensures that the AI has context to answer a question

Needs: impl

Covers: `feat~ai-answer-engines~1`

## AI chat with entries
`feat~ai.chatting.entries~1`

Rationale: specific requirements for chatting with a single bibliography entry

Needs: impl, pp

Covers: `feat~ai.chatting~1`

### Support hiding of the AI chat tab
`req~ai.chat.entries.hide-tab~1`

Rationale: users who do not use AI features should be able to declutter their interface

Needs: impl

Covers: `feat~ai.chatting.entries~1`

### Persist AI chat history for AI chat with entries
`req~ai.chat.entries.history-storage~1`

Rationale: history must be persisted per entry, so the user can resume the conversation later

Needs: dsn, model, impl, utest

Covers: `feat~ai.chatting.entries~1`

## AI chat with groups
`feat~ai.chatting.groups~1`

Rationale: specific requirements for chatting with a collection/group of entries simultaneously

Needs: impl, pp

Covers: `feat~ai.chatting~1`

### Support hiding of the context menu entry for AI chat with group
`req~ai.chat.groups.hide-context-menu~1`

Rationale: allows customization of the context menu to remove "Chat with group" if the user does not use it

Needs: impl

Covers: `feat~ai.chatting.groups~1`

### Persist AI chat history for AI chat with groups
`req~ai.chat.groups.history-storage~1`

Rationale: history must be persisted per group, so the conversation context is preserved across sessions

Needs: dsn, model, impl, utest

Covers: `feat~ai.chatting.groups~1`

### Display library name and group name in AI group chat
`req~ai.chat.groups.display-names~1`

Rationale: essential for user orientation, ensuring that users can distinguish between different chats of a group that has the same name in different libraries

Needs: impl

Covers: `feat~ai.chatting.groups~1`

<!-- markdownlint-disable-file MD022 -->