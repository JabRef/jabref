package org.jabref.logic.pdf;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.pdf.FileAnnotation;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileAnnotationCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileAnnotationCache.class);

    private static final int CACHE_SIZE = 1024;

    // the inner list holds the annotations per file, the outer collection maps this to a BibEntry.
    private LoadingCache<BibEntry, Map<Path, List<FileAnnotation>>> annotationCache;

    /// Creates an empty file annotation cache. Required to allow the annotation cache to be injected into views without
    /// hitting the bug <https://github.com/AdamBien/afterburner.fx/issues/71>.
    public FileAnnotationCache() {
    }

    public FileAnnotationCache(BibDatabaseContext context, FilePreferences filePreferences) {
        annotationCache = Caffeine.newBuilder().maximumSize(CACHE_SIZE).build(new CacheLoader<>() {
            @Override
            public Map<Path, List<FileAnnotation>> load(BibEntry entry) {
                return new EntryAnnotationImporter(entry).importAnnotationsFromFiles(context, filePreferences);
            }
        });
    }

    /**
     * Note that entry becomes the most recent entry in the cache
     *
     * @param entry entry for which to get the annotations
     * @return Map containing a list of annotations in a list for each file
     */
    public Map<Path, List<FileAnnotation>> getFromCache(BibEntry entry) {
        LOGGER.debug("Loading BibEntry '{}' from cache.", entry.getCitationKey().orElse(entry.getId()));
        return annotationCache.get(entry);
    }

    public void remove(BibEntry entry) {
        LOGGER.debug("Deleted BibEntry '{}' from cache.", entry.getCitationKey().orElse(entry.getId()));
        annotationCache.invalidate(entry);
    }
}
