package org.jabref.logic.pdf;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.FileDirectoryPreferences;
import org.jabref.model.pdf.FileAnnotation;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileAnnotationCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileAnnotation.class);
    //cache size in entries
    private final static int CACHE_SIZE = 10;

    //the inner list holds the annotations per file, the outer collection maps this to a BibEntry.
    private LoadingCache<BibEntry, Map<Path, List<FileAnnotation>>> annotationCache;

    /**
     * Creates an empty fil annotation cache. Required to allow the annotation cache to be injected into views without
     * hitting the bug https://github.com/AdamBien/afterburner.fx/issues/71 .
     */
    public FileAnnotationCache() {

    }

    public FileAnnotationCache(BibDatabaseContext context, FileDirectoryPreferences fileDirectoryPreferences) {
        annotationCache = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build(new CacheLoader<BibEntry, Map<Path, List<FileAnnotation>>>() {
            @Override
            public Map<Path, List<FileAnnotation>> load(BibEntry entry) throws Exception {
                return new EntryAnnotationImporter(entry).importAnnotationsFromFiles(context, fileDirectoryPreferences);
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
        LOGGER.debug(String.format("Loading Bibentry '%s' from cache.", entry.getCiteKeyOptional().orElse(entry.getId())));
        return annotationCache.getUnchecked(entry);
    }

    public void remove(BibEntry entry) {
        LOGGER.debug(String.format("Deleted Bibentry '%s' from cache.", entry.getCiteKeyOptional().orElse(entry.getId())));
        annotationCache.invalidate(entry);
    }
}
