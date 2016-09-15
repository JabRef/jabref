package net.sf.jabref.logic.citationstyle;

import java.util.HashMap;
import java.util.Map;

import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.database.event.EntryRemovedEvent;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.event.EntryChangedEvent;

import com.google.common.eventbus.Subscribe;


/**
 * Caches the generated Citations for quicker access
 * {@link CitationStyleGenerator} generates the citaiton with JavaScript which may take some time
 */
public class CitationStyleCache {

    private CitationStyle citationStyle;
    private Map<BibEntry, String> citationStylesCache = new HashMap<>();


    public CitationStyleCache(BibDatabaseContext bibDatabaseContext) {
        this(bibDatabaseContext, CitationStyle.getDefault());
    }

    public CitationStyleCache(BibDatabaseContext bibDatabaseContext, CitationStyle citationStyle) {
        this.setCitationStyle(citationStyle);
        bibDatabaseContext.getDatabase().registerListener(new BibDatabaseEntryListener());
    }

    /**
     * returns the citation for the given BibEntry and the set CitationStyle
     */
    public String getCitationFor(BibEntry entry) {
        String citation = citationStylesCache.get(entry);
        if (citation == null) {
            citation = CitationStyleGenerator.generateCitation(entry, this.citationStyle.getSource());
            citationStylesCache.put(entry, citation);
        }
        return citation;
    }

    public void setCitationStyle(CitationStyle citationStyle) {
        if (citationStyle == null || this.citationStyle == citationStyle) {
            return;
        }
        if (this.citationStyle == null || !this.citationStyle.getSource().equals(citationStyle.getSource())) {
            this.citationStyle = citationStyle;
            citationStylesCache.clear();
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
                citationStylesCache.remove(entryChangedEvent.getBibEntry());
            }
        }

        /**
         * removes the citation of the removed entry as it's not needed anymore
         */
        @Subscribe
        public void listen(EntryRemovedEvent entryRemovedEvent) {
            if (entryRemovedEvent != null && entryRemovedEvent.getBibEntry() != null) {
                citationStylesCache.remove(entryRemovedEvent.getBibEntry());
            }
        }
    }

}
