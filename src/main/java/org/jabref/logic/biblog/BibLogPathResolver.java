package org.jabref.logic.biblog;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

public class BibLogPathResolver {
    /**
     * Resolves custom or default .blg path for this library.
     *
     * Priority:
     * 1. User-defined path from MetaData
     * 2. Default: same name as .bib file with .blg extension
     */
    public static Optional<Path> resolve(BibDatabaseContext context) {
        return resolve(context.getMetaData(), context.getDatabasePath());
    }

    /**
     * Test-friendly API (allows direct input of MetaData and Path)
     */
    public static Optional<Path> resolve(MetaData metaData, Optional<Path> databasePath) {
        Optional<Path> userDefined = metaData.getBlgFilePath();
        if (userDefined.isPresent()) {
            return userDefined;
        }

        return databasePath.map(path ->
                path.resolveSibling(
                        path.getFileName().toString().replaceFirst("\\.bib$", ".blg")
                )
        );
    }
}
