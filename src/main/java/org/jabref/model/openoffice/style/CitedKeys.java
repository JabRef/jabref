package org.jabref.model.openoffice.style;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

public class CitedKeys {

    /**
     * Order-preserving map from citation keys to associated data.
     */
    private LinkedHashMap<String, CitedKey> data;

    CitedKeys(LinkedHashMap<String, CitedKey> data) {
        this.data = data;
    }

    /**
     *  The cited keys in their current order.
     */
    public List<CitedKey> values() {
        return new ArrayList<>(data.values());
    }

    public CitedKey get(String citationKey) {
        return data.get(citationKey);
    }

    /**
     * Sort entries for the bibliography.
     */
    void sortByComparator(Comparator<BibEntry> entryComparator) {
        List<CitedKey> cks = new ArrayList<>(data.values());
        cks.sort(new CompareCitedKey(entryComparator, true));
        LinkedHashMap<String, CitedKey> newData = new LinkedHashMap<>();
        for (CitedKey ck : cks) {
            newData.put(ck.citationKey, ck);
        }
        data = newData;
    }

    void numberCitedKeysInCurrentOrder() {
        int index = 1;
        for (CitedKey ck : data.values()) {
            if (ck.getLookupResult().isPresent()) {
                ck.setNumber(Optional.of(index));
                index++;
            } else {
                // Unresolved citations do not get a number.
                ck.setNumber(Optional.empty());
            }
        }
    }

    public void lookupInDatabases(List<BibDatabase> databases) {
        for (CitedKey ck : this.data.values()) {
            ck.lookupInDatabases(databases);
        }
    }

    void distributeLookupResults(CitationGroups cgs) {
        for (CitedKey ck : this.data.values()) {
            ck.distributeLookupResult(cgs);
        }
    }

    void distributeNumbers(CitationGroups cgs) {
        for (CitedKey ck : this.data.values()) {
            ck.distributeNumber(cgs);
        }
    }

    public void distributeUniqueLetters(CitationGroups cgs) {
        for (CitedKey ck : this.data.values()) {
            ck.distributeUniqueLetter(cgs);
        }
    }

}
