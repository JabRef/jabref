package org.jabref.logic.citationstyle;

import java.util.Objects;

import org.jabref.logic.preview.PreviewLayout;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.EntriesRemovedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntryChangedEvent;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.Subscribe;

/**
 * Caches the generated Citations for quicker access
 * {@link CitationStyleGenerator} generates the citation with JavaScript which may take some time
 */
public class CitationStyleCache {

    private static final int CACHE_SIZE = 1024;

    private PreviewLayout citationStyle;
    private final LoadingCache<BibEntry, String> citationStyleCache;

    public CitationStyleCache(BibDatabaseContext database) {
        citationStyleCache = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build(new CacheLoader<BibEntry, String>() {
            @Override
            public String load(BibEntry entry) {
                if (citationStyle != null) {
                    return citationStyle.generatePreview(entry, database.getDatabase());
                } else {
                    return "";
                }
            }
        });
        database.getDatabase().registerListener(new BibDatabaseEntryListener());
    }

    /**
     * Returns the citation for the given entry.
     */
    public String getCitationFor(BibEntry entry) {
        return citationStyleCache.getUnchecked(entry);
    }

    public void setCitationStyle(PreviewLayout citationStyle) {
        Objects.requireNonNull(citationStyle);
        if (!this.citationStyle.equals(citationStyle)) {
            this.citationStyle = citationStyle;
            this.citationStyleCache.invalidateAll();
        }
    }

    private class BibDatabaseEntryListener {
        /**
         * removes the outdated citation of the changed entry
         */
        @Subscribe
        public void listen(EntryChangedEvent entryChangedEvent) {
            citationStyleCache.invalidate(entryChangedEvent.getBibEntry());
        }

        /**
         * removes the citation of the removed entries as they are not needed anymore
         */
        @Subscribe
        public void listen(EntriesRemovedEvent entriesRemovedEvent) {
            for (BibEntry entry : entriesRemovedEvent.getBibEntries()) {
                citationStyleCache.invalidate(entry);
            }
        }
    }
}
