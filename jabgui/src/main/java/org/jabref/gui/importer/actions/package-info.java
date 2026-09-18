/// Actions that run after a library has been read from disk, before the user works with it.
///
/// [OpenDatabaseAction] loads the file and runs every [GUIPostOpenAction] whose `isActionNecessary` returns
/// `true`: [LibraryMigrationAction] (formats of older JabRef versions, see [org.jabref.migrations]),
/// [CheckForNewEntryTypesAction] (custom entry types found in the file), [SearchGroupsMigrationAction]
/// (search group syntax) and [AddGroupImportEntriesAction]. [ImportCommand] handles the "Import" menu entries.
///
/// Reading the file itself is done by [org.jabref.logic.importer.OpenDatabase] in `jablib`.
package org.jabref.gui.importer.actions;
