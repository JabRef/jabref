package org.jabref.model.openoffice.style;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.openoffice.util.OOListUtil;
import org.jabref.model.openoffice.util.OOPair;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// CitationGroups : the set of citation groups in the document.
///
/// This is the main input (as well as output) for creating citation markers and bibliography.
public class CitationGroups {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationGroups.class);

    private final Map<CitationGroupId, CitationGroup> citationGroupsUnordered;

    /// Provides order of appearance for the citation groups.
    private Optional<List<CitationGroupId>> globalOrder;

    /// This is going to be the bibliography
    private Optional<CitedReferences> bibliography;

    /// Constructor
    public CitationGroups(Map<CitationGroupId, CitationGroup> citationGroups) {
        this.citationGroupsUnordered = citationGroups;

        this.globalOrder = Optional.empty();
        this.bibliography = Optional.empty();
    }

    public int numberOfCitationGroups() {
        return citationGroupsUnordered.size();
    }

    /// For each citation in `where` call `fun.accept(new Pair(citation, value));`
    public <T> void distributeToCitations(List<CitationPath> where,
                                          Consumer<OOPair<Citation, T>> fun,
                                          T value) {

        for (CitationPath p : where) {
            CitationGroup group = citationGroupsUnordered.get(p.group);
            if (group == null) {
                LOGGER.warn("CitationGroups.distributeToCitations: group missing");
                continue;
            }
            Citation cit = group.citationsInStorageOrder.get(p.storageIndexInGroup);
            fun.accept(new OOPair<>(cit, value));
        }
    }

    /*
     * Look up each Citation in databases.
     */
    public void lookupCitations(List<BibDatabase> databases) {
        /*
         * It is not clear which of the two solutions below is better.
         */

        // (1) collect-lookup-distribute
        //
        // CitationDatabaseLookupResult for the same citation key is the same object. Until we
        // insert a new citation from the GUI.
        CitedReferences citedReferences = getCitedReferencesUnordered();
        citedReferences.lookupInDatabases(databases);
        citedReferences.distributeLookupResults(this);

        // (2) lookup each citation directly
        //
        // CitationDatabaseLookupResult for the same citation key may be a different object:
        // CitedReference.addPath has to use equals, so CitationDatabaseLookupResult has to override
        // Object.equals, which depends on BibEntry.equals and BibDatabase.equals doing the
        // right thing. Seems to work. But what we gained from avoiding collect-and-distribute
        // may be lost in more complicated consistency checking in addPath.
        //
        ///            for (CitationGroup group : getCitationGroupsUnordered()) {
        ///                for (Citation cit : group.citationsInStorageOrder) {
        ///                    cit.lookupInDatabases(databases);
        ///                }
        ///            }
    }

    public List<CitationGroup> getCitationGroupsUnordered() {
        return new ArrayList<>(citationGroupsUnordered.values());
    }

    /// Citation groups in `globalOrder`
    public List<CitationGroup> getCitationGroupsInGlobalOrder() {
        if (globalOrder.isEmpty()) {
            throw new IllegalStateException("getCitationGroupsInGlobalOrder: not ordered yet");
        }
        return OOListUtil.map(globalOrder.get(), citationGroupsUnordered::get);
    }

    /// Impose an order of citation groups by providing the order of their citation group idendifiers.
    ///
    /// Also set indexInGlobalOrder for each citation group.
    public void setGlobalOrder(@NonNull List<CitationGroupId> globalOrder) {
        if (globalOrder.size() != numberOfCitationGroups()) {
            throw new IllegalStateException("setGlobalOrder: globalOrder.size() != numberOfCitationGroups()");
        }
        this.globalOrder = Optional.of(globalOrder);

        // Propagate to each CitationGroup
        for (int i = 0; i < globalOrder.size(); i++) {
            CitationGroupId groupId = globalOrder.get(i);
            citationGroupsUnordered.get(groupId).setIndexInGlobalOrder(Optional.of(i));
        }
    }

    public boolean hasGlobalOrder() {
        return globalOrder.isPresent();
    }

    /// Impose an order for citations within each group.
    public void imposeLocalOrder(Comparator<BibEntry> entryComparator) {
        for (CitationGroup group : citationGroupsUnordered.values()) {
            group.imposeLocalOrder(entryComparator);
        }
    }

    /// Collect citations into a list of cited sources using neither CitationGroup.globalOrder or Citation.localOrder
    public CitedReferences getCitedReferencesUnordered() {
        LinkedHashMap<String, CitedReference> citedReferenceByCitationKey = new LinkedHashMap<>();
        for (CitationGroup group : citationGroupsUnordered.values()) {
            int storageIndexInGroup = 0;
            for (Citation citation : group.citationsInStorageOrder) {
                String citationKey = citation.citationKey;
                CitationPath path = new CitationPath(group.groupId, storageIndexInGroup);
                if (citedReferenceByCitationKey.containsKey(citationKey)) {
                    citedReferenceByCitationKey.get(citationKey).addPath(path, citation);
                } else {
                    citedReferenceByCitationKey.put(citationKey, new CitedReference(citationKey, path, citation));
                }
                storageIndexInGroup++;
            }
        }
        return new CitedReferences(citedReferenceByCitationKey);
    }

    /// CitedReferences created iterating citations in (globalOrder,localOrder)
    public CitedReferences getCitedReferencesSortedInOrderOfAppearance() {
        if (!hasGlobalOrder()) {
            throw new IllegalStateException("getSortedCitedReferences: no globalOrder");
        }
        LinkedHashMap<String, CitedReference> citedReferenceByCitationKey = new LinkedHashMap<>();
        for (CitationGroup group : getCitationGroupsInGlobalOrder()) {
            for (int i : group.getLocalOrder()) {
                Citation citation = group.citationsInStorageOrder.get(i);
                String citationKey = citation.citationKey;
                CitationPath path = new CitationPath(group.groupId, i);
                if (citedReferenceByCitationKey.containsKey(citationKey)) {
                    citedReferenceByCitationKey.get(citationKey).addPath(path, citation);
                } else {
                    citedReferenceByCitationKey.put(citationKey, new CitedReference(citationKey, path, citation));
                }
            }
        }
        return new CitedReferences(citedReferenceByCitationKey);
    }

    public Optional<CitedReferences> getBibliography() {
        return bibliography;
    }

    /// @return Citation keys where lookupCitations() failed.
    public List<String> getUnresolvedKeys() {
        CitedReferences bibliography = getBibliography().orElse(getCitedReferencesUnordered());

        List<String> unresolvedKeys = new ArrayList<>();
        for (CitedReference citedReference : bibliography.values()) {
            if (citedReference.getLookupResult().isEmpty()) {
                unresolvedKeys.add(citedReference.citationKey);
            }
        }
        return unresolvedKeys;
    }

    public void createNumberedBibliographySortedInOrderOfAppearance() {
        if (bibliography.isPresent()) {
            throw new IllegalStateException("createNumberedBibliographySortedInOrderOfAppearance:"
                    + " already have a bibliography");
        }
        CitedReferences citedReferences = getCitedReferencesSortedInOrderOfAppearance();
        citedReferences.numberCitedReferencesInCurrentOrder();
        citedReferences.distributeNumbers(this);
        bibliography = Optional.of(citedReferences);
    }

    /// precondition: database lookup already performed (otherwise we just sort citation keys)
    public void createPlainBibliographySortedByComparator(Comparator<BibEntry> entryComparator) {
        if (bibliography.isPresent()) {
            throw new IllegalStateException("createPlainBibliographySortedByComparator: already have a bibliography");
        }
        CitedReferences citedReferences = getCitedReferencesUnordered();
        citedReferences.sortByComparator(entryComparator);
        bibliography = Optional.of(citedReferences);
    }

    /// precondition: database lookup already performed (otherwise we just sort citation keys)
    public void createNumberedBibliographySortedByComparator(Comparator<BibEntry> entryComparator) {
        if (bibliography.isPresent()) {
            throw new IllegalStateException("createNumberedBibliographySortedByComparator: already have a bibliography");
        }
        CitedReferences citedReferences = getCitedReferencesUnordered();
        citedReferences.sortByComparator(entryComparator);
        citedReferences.numberCitedReferencesInCurrentOrder();
        citedReferences.distributeNumbers(this);
        bibliography = Optional.of(citedReferences);
    }

    /*
     * Query by CitationGroupId
     */

    public Optional<CitationGroup> getCitationGroup(CitationGroupId groupId) {
        CitationGroup group = citationGroupsUnordered.get(groupId);
        return Optional.ofNullable(group);
    }

    /// @return true if all citation groups have referenceMarkNameForLinking
    public boolean citationGroupsProvideReferenceMarkNameForLinking() {
        for (CitationGroup group : citationGroupsUnordered.values()) {
            if (group.getReferenceMarkNameForLinking().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /*
     * Callbacks.
     */

    public void afterCreateCitationGroup(CitationGroup group) {
        citationGroupsUnordered.put(group.groupId, group);

        globalOrder = Optional.empty();
        bibliography = Optional.empty();
    }

    public void afterRemoveCitationGroup(CitationGroup group) {
        citationGroupsUnordered.remove(group.groupId);
        globalOrder.map(l -> l.remove(group.groupId));

        bibliography = Optional.empty();
    }
}
