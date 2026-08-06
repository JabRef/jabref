package org.jabref.model.openoffice.style;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

public class CitedReferences {

    /// Order-preserving map from citation keys to associated data.
    private LinkedHashMap<String, CitedReference> data;

    CitedReferences(LinkedHashMap<String, CitedReference> data) {
        this.data = data;
    }

    /// The cited references in their current order.
    public List<CitedReference> values() {
        return new ArrayList<>(data.values());
    }

    public CitedReference get(String citationKey) {
        return data.get(citationKey);
    }

    /// Sort entries for the bibliography.
    void sortByComparator(Comparator<BibEntry> entryComparator) {
        List<CitedReference> citedReferences = new ArrayList<>(data.values());
        citedReferences.sort(new CompareCitedReference(entryComparator, true));
        LinkedHashMap<String, CitedReference> newData = new LinkedHashMap<>();
        for (CitedReference citedReference : citedReferences) {
            newData.put(citedReference.citationKey, citedReference);
        }
        data = newData;
    }

    void numberCitedReferencesInCurrentOrder() {
        int index = 1;
        for (CitedReference citedReference : data.values()) {
            if (citedReference.getLookupResult().isPresent()) {
                citedReference.setNumber(Optional.of(index));
                index++;
            } else {
                // Unresolved citations do not get a number.
                citedReference.setNumber(Optional.empty());
            }
        }
    }

    public void lookupInDatabases(List<BibDatabase> databases) {
        for (CitedReference citedReference : this.data.values()) {
            citedReference.lookupInDatabases(databases);
        }
    }

    void distributeLookupResults(CitationGroups citationGroups) {
        for (CitedReference citedReference : this.data.values()) {
            citedReference.distributeLookupResult(citationGroups);
        }
    }

    void distributeNumbers(CitationGroups citationGroups) {
        for (CitedReference citedReference : this.data.values()) {
            citedReference.distributeNumber(citationGroups);
        }
    }

    public void distributeUniqueLetters(CitationGroups citationGroups) {
        for (CitedReference citedReference : this.data.values()) {
            citedReference.distributeUniqueLetter(citationGroups);
        }
    }
}
