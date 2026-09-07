package org.jabref.migrations;

import org.jabref.logic.importer.ParserResult;

/// A conversion of a library written by an older JabRef version to the current data format.
///
/// Implementations are offered to the user by [org.jabref.gui.importer.actions.LibraryMigrationAction]
/// right after a library has been loaded.
public interface PostOpenMigration {

    /// Checks, without modifying anything, whether [#performMigration] would change the library.
    boolean isMigrationNecessary(ParserResult parserResult);

    /// Explains to the user what the migration changes.
    String getDescription();

    /// `false` when JabRef no longer writes the old format, so skipping the migration would lose data on save.
    /// Such migrations are shown to the user but cannot be disabled.
    default boolean isOptional() {
        return true;
    }

    void performMigration(ParserResult parserResult);
}
