/// Conversions of data written by older JabRef versions to the current formats.
///
/// Library migrations implement [PostOpenMigration] and are offered to the user after a library was loaded by
/// [org.jabref.gui.importer.actions.LibraryMigrationAction]: [ConvertLegacyExplicitGroups] (group tree of
/// JabRef 3), [ConvertMarkingToGroups] (`__markedentry` field of JabRef 4) and [SpecialFieldsToSeparateFields]
/// (special field values in `keywords`, JabRef 5.2). Preference migrations run once at startup from
/// [PreferencesMigrations].
///
/// Parsing of the legacy group tree itself is in [org.jabref.logic.importer.util.GroupsParser].
package org.jabref.migrations;
