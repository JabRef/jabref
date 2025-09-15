package org.jabref.logic.git.model;

import java.nio.file.Path;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;

public record MergeContext(
        Path repoRoot,
        Path bibPath,
        BibDatabaseContext localContext,
        ImportFormatPreferences importPrefs
) { }
