---
parent: Requirements
---
# UX

This page collects general UX requirements.

## Generally available buttons and menu items are disabled instead of not shown
`req~ux.disabled-vs-hidden~1`

When there is functionality generally available but cannot be executed at the present time, it is shown as disabled.

Example: Button to open a link. If there is no link, the button should be shown but not enabled.

Needs: impl

## Confirmation dialogs use the action name as the confirm button label
`req~ui.dialogs.confirmation.naming~1`

In confirmation dialogs, the confirm button must be labeled with the specific action name (e.g., "Download full text documents") rather than a generic label such as "OK" or "Yes".
This makes the intended action unambiguous and reduces the risk of accidental confirmation.

Needs: impl

## Auto close of merge entries dialog
`req~ux.auto-close.merge-entries~1`

The merge entries dialog collects and merges data from multiple sources.
In case there is only one source, it should not be shown.
Since some data fetchers take time, we need to open the dialog and wait until all sources are available.
[As soon as only one source is available, the dialog should be closed to speed up the user's workflow](https://github.com/JabRef/jabref/issues/13262).

Needs: impl

<!-- markdownlint-disable-file MD022 -->
