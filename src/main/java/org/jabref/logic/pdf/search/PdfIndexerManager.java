package org.jabref.logic.pdf.search;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.FilePreferences;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A PdfIndexer takes a long time to build. Caching it speeds up.
 * <p>
 * The PdfIndexer is related to the BibDatabaseContext and the FilePreferences. If the user changes the path of the library
 * or the file preferences, we need to create a new PdfIndexer. Otherwise, we can reuse the existing one.
 * <p>
 * This manager implements a <a href="https://www.neatcode.org/object-pool/">Object Pool pattern</a> for {@link PdfIndexer}.
 */
public class PdfIndexerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfIndexerManager.class);

    // Map from the path of the library index to the respective indexer
    private static Map<Path, PdfIndexer> indexerMap = new HashMap<>();

    // We store the file preferences for each path, so that we can update the indexer when the preferences change
    private static Map<Path, FilePreferences> pathFilePreferencesMap = new HashMap<>();

    public static @NonNull PdfIndexer getIndexer(BibDatabaseContext context, FilePreferences filePreferences) throws IOException {
        Path fulltextIndexPath = context.getFulltextIndexPath();
        PdfIndexer indexer = indexerMap.get(fulltextIndexPath);
        if (indexer != null) {
            // Check if the file preferences have changed
            FilePreferences storedFilePreferences = pathFilePreferencesMap.get(fulltextIndexPath);
            if (storedFilePreferences.equals(filePreferences)) {
                LOGGER.trace("Found existing indexer for context {}", context);
                return indexer;
            }
            LOGGER.debug("File preferences have changed, updating indexer");
            indexer.close();
            indexer = PdfIndexer.of(context, filePreferences);
            indexerMap.put(fulltextIndexPath, indexer);
            pathFilePreferencesMap.put(fulltextIndexPath, filePreferences);
            return indexer;
        }
        LOGGER.debug("No indexer found for context {}, creating new one", context);
        indexer = PdfIndexer.of(context, filePreferences);
        indexerMap.put(fulltextIndexPath, indexer);
        pathFilePreferencesMap.put(fulltextIndexPath, filePreferences);
        return indexer;
    }

    public static void shutdownAllIndexers() {
        indexerMap.values().forEach(indexer -> {
            try {
                indexer.close();
            } catch (Exception e) {
                LOGGER.debug("Problem closing PDF indexer", e);
            }
        });
        indexerMap.clear();
        pathFilePreferencesMap.clear();
    }

    public static void shutdownIndexer(BibDatabaseContext context) {
        Path fulltextIndexPath = context.getFulltextIndexPath();
        PdfIndexer indexer = indexerMap.remove(fulltextIndexPath);
        if (indexer != null) {
            try {
                indexer.close();
            } catch (IOException e) {
                LOGGER.debug("Could not close indexer", e);
            }
            pathFilePreferencesMap.remove(fulltextIndexPath);
        } else {
            LOGGER.debug("No indexer found for context {}", context);
        }
    }
}
