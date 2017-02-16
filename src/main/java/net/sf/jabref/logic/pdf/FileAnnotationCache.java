package net.sf.jabref.logic.pdf;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.pdf.FileAnnotation;

import org.apache.commons.collections4.map.LRUMap;

public class FileAnnotationCache {

    //cache size in entries
    final static int CACHE_SIZE = 10;
    //the inner list holds the annotations of a file, the outer list holds such a list for every file attached to an entry
    LRUMap<BibEntry, Map<String, List<FileAnnotation>>> annotationCache;
    BibDatabaseContext bibDatabaseContext;

    public FileAnnotationCache(BibDatabaseContext bibDatabaseContext) {
        this.bibDatabaseContext = bibDatabaseContext;
        annotationCache  = new LRUMap<>(CACHE_SIZE);
    }

    public void addToCache(BibEntry entry, final Map<String, List<FileAnnotation>> annotations){
        annotationCache.put(entry, annotations);
    }

    /**
     * Note that entry becomes the most recent entry in the cache
     * @param entry entry for which to get the annotations
     * @return Map containing a list of annotations in a list for each file
     */
    public Optional<Map<String, List<FileAnnotation>>> getFromCache(Optional<BibEntry> entry) {
        Optional<Map<String, List<FileAnnotation>>> cachedAnnotations = Optional.empty();
        if(entry.isPresent() && annotationCache.containsKey(entry.get())){
            return Optional.of(annotationCache.get(entry.get()));
        } else {
            return cachedAnnotations;
        }
    }
}
