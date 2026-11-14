---
parent: Requirements
---
# Entry Preview

## Features

### User can view the Available panel through the CSL and Customized tab views
`feat~entry-preview.tabs~1`

The Entry Preview displays two distinct views under the 'Available' Panel. Each view displays the styles corresponding to its respective tab. The CSL tab displays available CSL styles. The Customized tab displays available custom preview layouts.

Needs: impl

### User can manage manage custom preview layouts in the Entry Preview
`feat~entry-preview.create-custom-style~1`

The add field allows the user to create custom preview styles.
The delete button allows the user to remove custom preview styles.
The custom preview layout uses the same syntax as JabRef's custom export filters

For details about the custom export filter syntax and available commands, see the
[JabRef documentation on custom export filters](https://docs.jabref.org/import-export/export/customexports).

Needs: impl

### User can rename custom styles in the Entry Preview
`feat~entry-preview.rename-custom-style~1`

The user is able to rename a custom preview style along of its custom export filter layout. Renaming a style changes only its display name and does not modify the layout used to render the entry preview.

Needs: impl

### User can persist changes to custom styles in the Entry Preview
`feat~entry-preview.persist-custom-style~1`

Entry Preview customized styles are persisted by a CUID, independent of their display name, so renaming a style does not affect whether it round-trips across sessions

Needs: impl

<!-- markdownlint-disable-file MD022 -->
