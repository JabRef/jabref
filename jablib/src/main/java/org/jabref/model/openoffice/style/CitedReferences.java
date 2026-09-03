package org.jabref.model.openoffice.style;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class CitedReferences {

    /// Order-preserving map from citation keys to cited references.
    private LinkedHashMap<String, CitedReference> citedReferenceByCitationKey;

    CitedReferences(LinkedHashMap<String, CitedReference> citedReferenceByCitationKey) {
        this.citedReferenceByCitationKey = citedReferenceByCitationKey;
    }

    /// The cited references in their current order.
    public List<CitedReference> values() {
        return new ArrayList<>(citedReferenceByCitationKey.values());
    }

    public CitedReference get(String citationKey) {
        return citedReferenceByCitationKey.get(citationKey);
    }

    /// Sort entries for the bibliography.
    void sortByComparator(Comparator<BibEntry> entryComparator) {
        List<CitedReference> citedReferences = new ArrayList<>(citedReferenceByCitationKey.values());
        citedReferences.sort(new CompareCitedReference(entryComparator, true));
        LinkedHashMap<String, CitedReference> sortedCitedReferenceByCitationKey = new LinkedHashMap<>();
        for (CitedReference citedReference : citedReferences) {
            sortedCitedReferenceByCitationKey.put(citedReference.citationKey, citedReference);
        }
        citedReferenceByCitationKey = sortedCitedReferenceByCitationKey;
    }

    void numberCitedReferencesInCurrentOrder() {
        int index = 1;
        for (CitedReference citedReference : citedReferenceByCitationKey.values()) {
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
        for (CitedReference citedReference : this.citedReferenceByCitationKey.values()) {
            citedReference.lookupInDatabases(databases);
        }
    }

    void distributeLookupResults(CitationGroups citationGroups) {
        for (CitedReference citedReference : this.citedReferenceByCitationKey.values()) {
            citedReference.distributeLookupResult(citationGroups);
        }
    }

    void distributeNumbers(CitationGroups citationGroups) {
        for (CitedReference citedReference : this.citedReferenceByCitationKey.values()) {
            citedReference.distributeNumber(citationGroups);
        }
    }

    public void distributeUniqueLetters(CitationGroups citationGroups) {
        for (CitedReference citedReference : this.citedReferenceByCitationKey.values()) {
            citedReference.distributeUniqueLetter(citationGroups);
        }
    }
}
