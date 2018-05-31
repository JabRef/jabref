package org.jabref.logic.citationstyle;

import java.util.Objects;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.EntryRemovedEvent;
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

    private CitationStyle citationStyle;
    private final LoadingCache<BibEntry, String> citationStyleCache;


    public CitationStyleCache(BibDatabaseContext bibDatabaseContext) {
        this(bibDatabaseContext, CitationStyle.getDefault());
    }

    public CitationStyleCache(BibDatabaseContext bibDatabaseContext, CitationStyle citationStyle) {
        this.citationStyle = Objects.requireNonNull(citationStyle);
        citationStyleCache = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build(new CacheLoader<BibEntry, String>() {
            @Override
            public String load(BibEntry entry) {
                return CitationStyleGenerator.generateCitation(entry, getCitationStyle().getSource(), CitationStyleOutputFormat.HTML);
            }
        });
        bibDatabaseContext.getDatabase().registerListener(new BibDatabaseEntryListener());
    }

    /**
     * returns the citation for the given BibEntry and the set CitationStyle
     */
    public String getCitationFor(BibEntry entry) {
        return citationStyleCache.getUnchecked(entry);
    }

    public void setCitationStyle(CitationStyle citationStyle) {
        Objects.requireNonNull(citationStyle);
        if (!this.citationStyle.equals(citationStyle)) {
            this.citationStyle = citationStyle;
            this.citationStyleCache.invalidateAll();
        }
    }

    public CitationStyle getCitationStyle() {
        return this.citationStyle;
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
         * removes the citation of the removed entry as it's not needed anymore
         */
        @Subscribe
        public void listen(EntryRemovedEvent entryRemovedEvent) {
            citationStyleCache.invalidate(entryRemovedEvent.getBibEntry());
        }
    }
}
