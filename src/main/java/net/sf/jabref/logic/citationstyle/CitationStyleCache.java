package net.sf.jabref.logic.citationstyle;

import java.util.HashMap;
import java.util.Map;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.event.EntryChangedEvent;
import net.sf.jabref.model.event.EntryRemovedEvent;

import com.google.common.eventbus.Subscribe;


public class CitationStyleCache {

    private CitationStyle citationStyle;
    private Map<String, Map<BibEntry, String>> citationStylesCache = new HashMap<>();


    public CitationStyleCache(BibDatabaseContext bibDatabaseContext) {
        this(bibDatabaseContext, CitationStyle.getDefault());
    }

    public CitationStyleCache(BibDatabaseContext bibDatabaseContext, CitationStyle citationStyle) {
        this.setCitationStyle(citationStyle);
        bibDatabaseContext.getDatabase().registerListener(new BibDatabaseEntryListener());
    }

    public String getCitationFor(BibEntry entry) {
        Map<BibEntry, String> cache = citationStylesCache.get(citationStyle.getSource());
        if (cache == null){
            cache = new HashMap<>();
            citationStylesCache.put(citationStyle.getSource(), cache);
        }

        String citation = cache.get(entry);
        if (citation == null) {
            citation = CitationStyleGenerator.generateCitation(entry, this.citationStyle.getSource());
            cache.put(entry, citation);
        }
        return citation;
    }

    public void setCitationStyle(CitationStyle citationStyle) {
        if (citationStyle == null || this.citationStyle == citationStyle) {
            return;
        }
        if (this.citationStyle == null || !this.citationStyle.getSource().equals(citationStyle.getSource())) {
            this.citationStyle = citationStyle;
        }
    }

    public CitationStyle getCitationStyle() {
        return citationStyle;
    }


    private class BibDatabaseEntryListener {
        /**
         * removes the outdated citation of the changed entry
         */
        @Subscribe
        public void listen(EntryChangedEvent entryChangedEvent) {
            if (entryChangedEvent != null && entryChangedEvent.getBibEntry() != null){
                citationStylesCache.get(citationStyle.getSource()).remove(entryChangedEvent.getBibEntry());
            }
        }

        /**
         * removes the citation of the removed entry as it's not needed anymore
         */
        @Subscribe
        public void listen(EntryRemovedEvent entryRemovedEvent) {
            if (entryRemovedEvent != null && entryRemovedEvent.getBibEntry() != null) {
                citationStylesCache.get(citationStyle.getSource()).remove(entryRemovedEvent.getBibEntry());
            }
        }
    }

}
