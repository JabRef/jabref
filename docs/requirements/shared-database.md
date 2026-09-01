---
parent: Requirements
---
# Shared SQL database

## Live propagation of changes
`req~shared-database.live-propagation~1`

Changes made by one client — entry modifications, groups, and library settings — appear in all other connected clients without any manual action.

Needs: impl

## Change content travels in the notification
`req~shared-database.change-content-in-notification~1`

A field change notification carries the change itself (entry id, field, old and new value, entry version), so receivers apply it directly. Receivers fall back to pulling from the database whenever the notification does not exactly match their local state.

Needs: impl

## Micro-edits are batched
`req~shared-database.micro-edit-batching~1`

Keystroke-level edits are not written per keystroke: they are buffered and flushed on the next major change or when the library is closed. A flush notifies the other clients.

Needs: impl

## Existing databases are migrated
`req~shared-database.migration~1`

Connecting to a database created by an earlier JabRef version copies its content into the current table structure. The old tables are kept, so older JabRef versions continue to work.

Needs: impl

## Connection details can be pasted as a URL
`req~shared-database.connection-url~1`

The login dialog accepts a connection URL as handed out by hosting providers (`postgres://user:password@host:port/database?...`) or a JDBC URL and fills in the connection details from it. Parameters JabRef has no dedicated setting for are passed on to the driver unchanged.

Needs: impl

<!-- markdownlint-disable-file MD022 -->
