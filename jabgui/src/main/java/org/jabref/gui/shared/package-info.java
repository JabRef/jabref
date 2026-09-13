/// User interface for shared (SQL) libraries.
///
/// [ConnectToSharedDatabaseCommand] opens [SharedDatabaseLoginDialogView], which hands the connection settings to
/// [SharedDatabaseUIManager]. That manager owns a connected shared library: it opens its tab and reacts to the
/// synchronization events of `org.jabref.logic.shared`. [SharedDatabaseErrorTab] stands in for a library whose
/// reconnection on startup failed.
///
/// The connection, synchronization and persistence of the connection settings live in `org.jabref.logic.shared`.
///
/// See <https://devdocs.jabref.org/code-howtos/remote-storage-sql.html>.
package org.jabref.gui.shared;
