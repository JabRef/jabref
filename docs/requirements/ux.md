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

## Main Table Focus
`req~maintable.focus~1`

Prevents the main table from losing focus when adding a new library or  when changing tabs.
This provides immediate keyboard interaction capabilities (such as Ctrl+V for pasting operations when changing tabs) without requiring explicit focus via mouse click.

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

## Activating large libraries keeps entry previews responsive
`req~ux.active-library.preview-responsiveness~1`

When a user activates a large library, automatic group construction and group-count evaluation must not delay rendering the selected entry preview.

Needs: impl

## Creating a new explicit group can reuse the current selection
`req~ux.groups.create-explicit-from-selection~1`

When a user creates a new explicit group, JabRef should allow reusing the currently selected entries for that group and should keep the newly created group selected afterwards.

Needs: impl

## Pressing Escape when a combo box popup is open closes only the combo box
`req~ux.combobox.escape-closes-popup-only~1`

When a `combobox` or drop-down list (such as a `CheckComboBox`, `ComboBox`, or `ChoiceBox`) is open within a dialog and the user presses Escape, only the drop-down popup must be closed.
The enclosing dialog must remain open.

## Saving keeps external change detection active
`req~ux.external-library-changes.after-save~1`

When JabRef saves a library, it must keep observing filesystem changes, defer change detection until the save has finished, and then inspect the resulting file for external changes that require conflict resolution.
Since inspecting a library file means parsing it completely, the inspection is skipped when the file's size and modification time show that it has not changed since the last state known to match the in-memory library.

Needs: impl

## Deleting many entries keeps the main table responsive
`req~ux.large-library.bulk-entry-removal~1`

When a user deletes many entries from a large library, JabRef must keep the main table responsive.

Needs: impl

## Focus the text field in text dialogs
`req~ux.textdialogs.focus~1`

When a dialog with text input as a main component is opened, the text field should be focused.

Needs: impl

## Text filtering is case-insensitive and separator-aware
`req~ux.text-filtering.case-insensitive-separators~1`

When users filter textual lists, matching must ignore case and treat punctuation or whitespace separators as equivalent.
For example, a search for `Springer lecture` or `SPRINGER LECTURE` or `springer lecture` should match `Springer - Lecture Notes in Computer Science`.

Needs: impl

## Show unsaved changes before closing a library
`req~ux.close.show-diff~1`

When closing a modified library, the "Save before closing" dialog should offer to show the unsaved changes compared to the file on disk, so the user can decide between saving and discarding on an informed basis.

Needs: impl

## Automatically paste clipboard content when useful
`req~ux.textdialogs.autopaste~1`

When a dialog with text input as a main component is opened, and it is expected that while working with it, the user will paste from clipboard, JabRef should already automatically paste it.

Example: new entry dialog by ID. It is expected that user would copy some paper ID (from browser, PDF, etc.), and then paste it in the dialog. As said above, JabRef automatically pastes the ID into the text field.

Needs: impl

### Automatic Identifier Detection and Focus in New Entry Dialog
`req~newentry.clipboard.autofocus~1`

When the "New Entry" dialog is opened:

- If the clipboard contains a valid identifier (e.g., DOI, ISBN, ArXiv, RFC):

  - The dialog automatically switches to the "Enter Identifier" tab.
  - The identifier input field is automatically filled with the clipboard content.
  - The field receives keyboard focus and its content is selected.
  - The corresponding fetcher (e.g., DOI, ISBN) is automatically selected based on the detected identifier type.

This behavior streamlines the process of creating new entries by allowing users to copy an identifier and open the dialog, without needing to manually select the input field, switch tabs, or choose a fetcher manually.

Needs: impl

<!-- markdownlint-disable-file MD022 -->
