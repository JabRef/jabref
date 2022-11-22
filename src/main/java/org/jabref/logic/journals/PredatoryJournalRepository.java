package org.jabref.logic.journals;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

/**
 * A repository for all predatory journals and publishers, including add and find methods.
 */
public class PredatoryJournalRepository {

    private final MVMap<String, List<String>> predatoryJournals;

    public PredatoryJournalRepository() {
        // MVStore store = new MVStore.Builder().readOnly().fileName(journalList.toAbsolutePath().toString()).open();
        MVStore store = MVStore.open(null);     // fileName is null gives in-memory map
        this.predatoryJournals = store.openMap("predatoryJournals");
    }

    /**
     * Returns true if the given journal name is contained in the list either in its full form
     */
    public boolean isKnownName(String journalName) {
        String journal = journalName.trim().replaceAll(Matcher.quoteReplacement("\\&"), "&");

        return predatoryJournals.containsKey(journal);
    }

    public Set<String> getFullNames() {
        return predatoryJournals.keySet();
    }

    public void addToPredatoryJournals(String name, String abbr, String url) {
        // computeIfAbsent -- more efficient if key is already present as list only created if absent
        // predatoryJournals.computeIfAbsent(decode(name), (k, v) -> new ArrayList<String>()).addAll(List.of(decode(abbr), url));

        predatoryJournals.put(decode(name), List.of(decode(abbr), url));
    }

    public MVMap getMap() { return predatoryJournals; }

    private String decode(String s) {
        if (s == null) return "";

        return s.replace(",", "")
                .replace("&amp;", "")
                .replace("&#8217;", "'")
                .replace("&#8211;", "-");
    }
}
