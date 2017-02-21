package org.jabref.logic.pdf;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.pdf.FileAnnotation;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;


public class FileAnnotationCache {

    //cache size in entries
    private final static int CACHE_SIZE = 10;
    //the inner list holds the annotations per file, the outer collection maps this to a BibEntry.
    private LoadingCache<BibEntry, Map<String, List<FileAnnotation>>> annotationCache;

    public FileAnnotationCache(BibDatabaseContext context) {
        annotationCache = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build(new CacheLoader<BibEntry, Map<String, List<FileAnnotation>>>() {
            @Override
            public Map<String, List<FileAnnotation>> load(BibEntry entry) throws Exception {
                return new EntryAnnotationImporter(entry).importAnnotationsFromFiles(context);
            }
        });
    }

    /**
     * Note that entry becomes the most recent entry in the cache
     *
     * @param entry entry for which to get the annotations
     * @return Map containing a list of annotations in a list for each file
     */
    public Map<String, List<FileAnnotation>> getFromCache(BibEntry entry) throws ExecutionException {
        return annotationCache.get(entry);
    }
}
