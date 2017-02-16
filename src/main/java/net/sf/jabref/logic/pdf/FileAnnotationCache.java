package net.sf.jabref.logic.pdf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.pdf.FileAnnotation;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class FileAnnotationCache {

    //cache size in entries
    private final static int CACHE_SIZE = 10;
    //the inner list holds the annotations per file, the outer collection maps this to a BibEntry.
    private LoadingCache<BibEntry, Map<String, List<FileAnnotation>>> annotationCache;

    public FileAnnotationCache() {
        annotationCache = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build(new CacheLoader<BibEntry, Map<String, List<FileAnnotation>>>() {
            @Override
            public Map<String, List<FileAnnotation>> load(BibEntry notUsed) throws Exception {
                // Automated reloading of entries is not supported.
                return new HashMap<>();
            }
        });
    }

    public void addToCache(BibEntry entry, final Map<String, List<FileAnnotation>> annotations) {
        annotationCache.put(entry, annotations);
    }

    /**
     * Note that entry becomes the most recent entry in the cache
     *
     * @param entry entry for which to get the annotations
     * @return Map containing a list of annotations in a list for each file
     */
    public Optional<Map<String, List<FileAnnotation>>> getFromCache(Optional<BibEntry> entry) {
        Optional<Map<String, List<FileAnnotation>>> emptyAnnotation = Optional.empty();
        try {
            if (entry.isPresent() && annotationCache.get(entry.get()).size() > 0) {
                return Optional.of(annotationCache.get(entry.get()));
            }
        } catch (ExecutionException failure) {
            return emptyAnnotation;
        }
        return emptyAnnotation;
    }
}
