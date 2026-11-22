/// Actions that run after a library has been read from disk, before the user works with it.
///
/// [OpenDatabaseAction] loads the file. [org.jabref.gui.LibraryTab] runs [LibraryMigrationAction] before
/// installing the library, then offers the custom entry types via [CheckForNewEntryTypesAction].
/// [ImportCommand] handles the "Import" menu entries, and [AddGroupImportEntriesAction] supports importing entries.
///
/// Reading the file itself is done by [org.jabref.logic.importer.OpenDatabase] in `jablib`.
package org.jabref.gui.importer.actions;
