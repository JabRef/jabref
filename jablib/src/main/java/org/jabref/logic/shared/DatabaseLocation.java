package org.jabref.logic.shared;

/// This enum represents the location for {@link org.jabref.model.database.BibDatabaseContext}.
public enum DatabaseLocation {
    LOCAL,
    SHARED,
    /// A directory opened as a library: entries live in Hayagriva YAML files next to their PDFs,
    /// there is no `.bib` file, and thus no database path.
    DIRECTORY
}
