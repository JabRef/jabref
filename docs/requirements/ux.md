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

## Critical startup failures show an error dialog
`req~ux.startup.critical-error-dialog~1`

If a critical error occurs before the main window is fully constructed, it must not fail silently.
The user needs a visible error dialog, in addition to the log entry, since [digging through log files is not accessible to most users](https://github.com/JabRef/jabref/issues/14967).

Needs: impl

## Merge entries dialog allows selecting empty field values
`req~ux.merge-entries.select-empty-field~1`

When the merge entries dialog shows a field that is missing in one of the source entries, the user must be able to explicitly select that empty value so the merged entry is cleared for that field.

Needs: impl

## Updating an entry via entry data applies the confirmed merge result
`req~ux.update-entry-web-info.apply-merge-result~1`

When a user chooses `Update with bibliographic information via entry data` and confirms the merge dialog, the selected merged values must be written back to the original entry as one undoable update.

Needs: impl

## GitHub personal access token verification
`req~ux.git-share.personal-access-token-verification~1`

The GitHub sharing dialog must allow users to verify that their personal access token has push access to the configured GitHub repository before sharing a library.

Needs: impl

## Git pull with unrelated histories
`req~ux.git-pull.unrelated-histories~1`

Git pull must support a local library and its configured remote when their commit histories have no common ancestor.

Needs: impl

## Git push to an empty remote
`req~ux.git-push.empty-remote~1`

Git push must publish the current branch and configure its upstream when the configured remote has no branches.

Needs: impl

## Git push rejection reporting
`req~ux.git-push.rejected-update-reporting~1`

Git push must report a rejected remote update to the user.

Needs: impl

### Activating large libraries keeps entry previews responsive
`req~ux.active-library.preview-responsiveness~1`

When a user activates a large library, automatic group construction and group-count evaluation must not delay rendering the selected entry preview.

Needs: impl

### Creating a new explicit group can reuse the current selection
`req~ux.groups.create-explicit-from-selection~1`

When a user creates a new explicit group, JabRef should allow reusing the currently selected entries for that group and should keep the newly created group selected afterwards.

Needs: impl

### Saving keeps external change detection active
`req~ux.external-library-changes.after-save~1`

When JabRef saves a library, it must keep observing filesystem changes, defer change detection until the save has finished, and then inspect the resulting file for external changes that require conflict resolution.
Since inspecting a library file means parsing it completely, the inspection is skipped when the file's size and modification time show that it has not changed since the last state known to match the in-memory library.

Needs: impl

### Committing a library that is not under version control
`req~ux.git-commit.initialize-repository~1`

When a user commits a library that is not inside a Git repository, JabRef must offer to initialize a repository in the library's directory and commit the library file there.
Only the library file and the generated `.gitignore` are committed, so unrelated files in that directory stay untracked.
Declining the offer must leave the directory unchanged, because the user may want to clone an existing repository into it instead.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
