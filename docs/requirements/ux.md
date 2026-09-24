---
parent: Requirements
---
# UX

This page collects general UX requirements.

## Inactive actions must be displayed as disabled rather than hidden
`req~ux.disabled-vs-hidden~1`

When there is functionality generally available but cannot be executed at the present time, it is shown as disabled.

Example: Button to open a link. If there is no link, the button should be shown but not enabled.

Needs: impl

## Confirmation dialogs must label confirm button with action name
`req~ui.dialogs.confirmation.naming~1`

In confirmation dialogs, the confirm button must be labeled with the specific action name (e.g., "Download full text documents") rather than a generic label such as "OK" or "Yes".

Rationale:

Action-specific labels eliminate ambiguity about the consequence of the button press and reduce the risk of accidental confirmation.

Needs: impl

## Merge entries dialog must close automatically when single source remains
`req~ux.auto-close.merge-entries~1`

The merge entries dialog collects and merges data from multiple sources.
In case there is only one source, it should not be shown.
Since some data fetchers take time, we need to open the dialog and wait until all sources are available.
[As soon as only one source is available, the dialog should be closed to speed up the user's workflow](https://github.com/JabRef/jabref/issues/13262).

Needs: impl

## Main table must maintain focus when switching tabs or adding libraries
`req~maintable.focus~1`

The main table maintains focus when adding a new library or switching between library tabs.

Rationale:

Retaining focus enables immediate keyboard interaction (such as Ctrl+V paste shortcuts) without requiring the user to click with the mouse first.

Needs: impl

## Main table column headers must display user-friendly titles
`req~maintable.column-headers.user-friendly~1`

Column headers of the main table show readable names, not raw BibTeX field names.
JabRef-internal fields such as the entry type and the citation key are shown as "Entry Type" and "Citation Key".
Headers use Title Case, as an exception to the sentence-case rule for UI text, so they read like the other headers ("Author/Editor").

Needs: impl

## GUI must display error dialog upon critical startup failure
`req~ux.startup.critical-error-dialog~1`

If a critical error occurs before the main window is fully constructed, JabRef displays a visible error dialog in addition to logging the error.

Rationale:

Digging through log files is not accessible or obvious to non-technical users ([#14967](https://github.com/JabRef/jabref/issues/14967)).

Needs: impl

## JabRef must load citation style sources on demand
`req~ux.citation-styles.lazy-source-loading~1`

JabRef must load built-in citation-style metadata at startup without retaining every CSL source in memory. A style's source is loaded only when that style is used.

Needs: impl, utest

## Merge entries dialog must allow selecting empty field values
`req~ux.merge-entries.select-empty-field~1`

When the merge entries dialog shows a field that is missing in one of the source entries, the user must be able to explicitly select that empty value so the merged entry is cleared for that field.

Needs: impl

## Entry update workflow must apply confirmed merge result atomically
`req~ux.update-entry-web-info.apply-merge-result~1`

When a user chooses `Update with bibliographic information via entry data` and confirms the merge dialog, the selected merged values must be written back to the original entry as one undoable update.

Needs: impl

## Library activation must keep entry preview rendering responsive
`req~ux.active-library.preview-responsiveness~1`

When a user activates a large library, automatic group construction and group-count evaluation must not delay rendering the selected entry preview.

Needs: impl

## Group creation must allow populating new explicit group from selection
`req~ux.groups.create-explicit-from-selection~1`

When a user creates a new explicit group, JabRef should allow reusing the currently selected entries for that group and should keep the newly created group selected afterwards.

Needs: impl

## Open combo box popup must close without closing enclosing dialog on Escape
`req~ux.combobox.escape-closes-popup-only~1`

When a `combobox` or drop-down list (such as a `CheckComboBox`, `ComboBox`, or `ChoiceBox`) is open within a dialog and the user presses Escape, only the drop-down popup must be closed.
The enclosing dialog must remain open.

## Library save must maintain filesystem change detection after completion
`req~ux.external-library-changes.after-save~1`

When JabRef saves a library, it must keep observing filesystem changes, defer change detection until the save has finished, and then inspect the resulting file for external changes that require conflict resolution.
Since inspecting a library file means parsing it completely, the inspection is skipped when the file's size and modification time show that it has not changed since the last state known to match the in-memory library.

Needs: impl

## Bulk entry deletion must keep main table responsive
`req~ux.large-library.bulk-entry-removal~1`

When a user deletes many entries from a large library, JabRef must keep the main table responsive.

Needs: impl

## Text input dialogs must focus primary text field upon opening
`req~ux.textdialogs.focus~1`

When a dialog with text input as a main component is opened, the text field should be focused.

Needs: impl

## Textual list filtering must be case-insensitive and separator-aware
`req~ux.text-filtering.case-insensitive-separators~1`

When users filter textual lists, matching must ignore case and treat punctuation or whitespace separators as equivalent.
For example, a search for `Springer lecture` or `SPRINGER LECTURE` or `springer lecture` should match `Springer - Lecture Notes in Computer Science`.

Needs: impl

## Save before closing dialog must offer diff view of unsaved changes
`req~ux.close.show-diff~1`

When closing a modified library, the "Save before closing" dialog should offer to show the unsaved changes compared to the file on disk, so the user can decide between saving and discarding on an informed basis.

Needs: impl

## Text input dialogs must auto-paste relevant clipboard content on open
`req~ux.textdialogs.autopaste~1`

When a dialog with text input as a main component is opened, and it is expected that while working with it, the user will paste from clipboard, JabRef should already automatically paste it.

Example: new entry dialog by ID. It is expected that user would copy some paper ID (from browser, PDF, etc.), and then paste it in the dialog. As said above, JabRef automatically pastes the ID into the text field.

Needs: impl

### New entry dialog must auto-detect clipboard identifier and focus field
`req~newentry.clipboard.autofocus~1`

When the "New Entry" dialog is opened:

- If the clipboard contains a valid identifier (e.g., DOI, ISBN, ArXiv, RFC):

  - The dialog automatically switches to the "Enter Identifier" tab.
  - The identifier input field is automatically filled with the clipboard content.
  - The field receives keyboard focus and its content is selected.
  - The corresponding fetcher (e.g., DOI, ISBN) is automatically selected based on the detected identifier type.

Rationale:

Pre-filling and auto-focusing the detected identifier eliminates repetitive mouse clicks and tab switching when creating entries from copied DOIs or ISBNs.

Needs: impl

## JabRef must bundle dual-scheme community themes out of the box
`req~ux.themes.bundled-community-themes~1`

The themes from <https://themes.jabref.org/> that cover both color schemes are bundled with JabRef and appear in the theme selection next to the built-in themes, without the user having to download a CSS file.

Needs: impl

## Library tabs must display icon indicating library storage format
`req~ux.tabs.library-kind-icon~1`

Every library tab carries an icon: one for a BibTeX library, one for a BibLaTeX library, and one for a shared database. A shared database shows the database icon regardless of its mode.

Needs: impl, utest

## Preferences walkthroughs must open preferences dialog directly
`req~ux.walkthrough.preferences-direct~1`

Walkthroughs that require preferences must open the preferences dialog directly, so they can start regardless of the platform menu presentation.

Needs: impl

## Groups walkthrough must prepare example library and groups UI state
`req~ux.walkthrough.groups-preparation~1`

The groups walkthrough must open its bundled example library, display the Groups pane, and clear the current search before guiding the user.

Needs: impl

<!-- markdownlint-disable-file MD022 -->
