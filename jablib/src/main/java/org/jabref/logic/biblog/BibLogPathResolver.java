package org.jabref.logic.biblog;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

/// Resolves custom or default .blg path for this library.
///
/// Priority:
///
/// 1. User-defined path from MetaData
/// 2. Default: same name as .bib file with .blg extension
public class BibLogPathResolver {

    /**
     * Resolves .blg path using current user context.
     */
    public static Optional<Path> resolve(BibDatabaseContext context) {
        if (context == null) {
            return Optional.empty();
        }

        String user = System.getProperty("user.name");
        return resolve(context.getMetaData(), context.getDatabasePath(), user);
    }

    /**
     * Test-friendly API (allows direct input of MetaData and Path)
     */
    public static Optional<Path> resolve(MetaData metaData, Optional<Path> databasePath, String user) {
        if (metaData == null || user == null) {
            return Optional.empty();
        }

        Optional<Path> userDefined = metaData.getBlgFilePath(user);
        if (userDefined.isPresent() && userDefined != null) {
            return userDefined;
        }

        if (databasePath == null || databasePath.isEmpty()) {
            return Optional.empty();
        }

        return databasePath.map(path ->
                path.resolveSibling(
                        path.getFileName().toString().replaceFirst("\\.bib$", ".blg")
                )
        );
    }
}
