---
parent: Requirements
---
# Focus

## User Interface

### Main Table Focus
`req~maintable.focus~1`

Prevents the main table from losing focus when adding a new library or  when changing tabs.
This provides immediate keyboard interaction capabilities (such as Ctrl+V for pasting operations when changing tabs) without requiring explicit focus via mouse click.

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
