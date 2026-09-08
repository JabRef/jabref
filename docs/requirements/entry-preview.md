---
parent: Requirements
---
# Entry Preview

## Features

### Entry Preview shows the Available Panel split into CSL and Customized tabs
`req~entry-preview.tabs~1`

The Entry Preview should display two distinct views under the 'Available' Panel. Each view displays the styles corresponding to its respective tab. The CSL tab displays available CSL styles. The Customized tab displays available custom preview layouts.

Needs: impl

### Entry Preview allows the user to manage custom preview layouts
`req~entry-preview.create-custom-style~1`

The add/delete buttons should allow the user to create and remove custom preview styles.
The custom preview layout uses the same syntax as JabRef's custom export filters

For details about the custom export filter syntax and available commands, see the
[JabRef documentation on custom export filters](https://docs.jabref.org/import-export/export/customexports).

Needs: impl

### Entry Preview should allow the user to rename custom styles
`req~entry-preview.rename-custom-style~1`

The user should be able to rename a custom preview style along of its custom export filter layout. Renaming a style changes only its display name and does not modify the layout used to render the entry preview.

Needs: impl

### Entry Preview should persist changes to custom styles
`req~entry-preview.persist-custom-style~1`

Entry Preview customized styles are persisted by a CUID, independent of their display name, so renaming a style does not affect whether it round-trips across sessions

Needs: impl

<!-- markdownlint-disable-file MD022 -->
