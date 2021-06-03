package org.jabref.model.openoffice.style;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.openoffice.util.OOListUtil;
import org.jabref.model.openoffice.util.OOPair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CitationGroups : the set of citation groups in the document.
 *
 * This is the main input (as well as output) for creating citation
 * markers and bibliography.
 *
 */
public class CitationGroups {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationGroups.class);

    private Map<CitationGroupId, CitationGroup> citationGroupsUnordered;

    /**
     * Provides order of appearance for the citation groups.
     */
    private Optional<List<CitationGroupId>> globalOrder;

    /**
     *  This is going to be the bibliography
     */
    private Optional<CitedKeys> bibliography;

    /**
     * Constructor
     */
    public CitationGroups(Map<CitationGroupId, CitationGroup> citationGroups) {

        this.citationGroupsUnordered = citationGroups;

        this.globalOrder = Optional.empty();
        this.bibliography = Optional.empty();
    }

    public int numberOfCitationGroups() {
        return citationGroupsUnordered.size();
    }

    /**
     * For each citation in {@code where}
     * call {@code fun.accept(new Pair(citation, value));}
     */
    public <T> void distributeToCitations(List<CitationPath> where,
                                          Consumer<OOPair<Citation, T>> fun,
                                          T value) {

        for (CitationPath p : where) {
            CitationGroup cg = citationGroupsUnordered.get(p.group);
            if (cg == null) {
                LOGGER.warn("CitationGroups.distributeToCitations: group missing");
                continue;
            }
            Citation cit = cg.citationsInStorageOrder.get(p.storageIndexInGroup);
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
        if (true) {
            // collect-lookup-distribute
            //
            // CitationDatabaseLookupResult for the same citation key
            // is the same object. Until we insert a new citation from the GUI.
            CitedKeys cks = getCitedKeysUnordered();
            cks.lookupInDatabases(databases);
            cks.distributeLookupResults(this);
        } else {
            // lookup each citation directly
            //
            // CitationDatabaseLookupResult for the same citation key
            // may be a different object: CitedKey.addPath has to use equals,
            // so CitationDatabaseLookupResult has to override Object.equals,
            // which depends on BibEntry.equals and BibDatabase.equals
            // doing the right thing. Seems to work. But what we gained
            // from avoiding collect-and-distribute may be lost in more
            // complicated consistency checking in addPath.
            //
            for (CitationGroup cg : getCitationGroupsUnordered()) {
                for (Citation cit : cg.citationsInStorageOrder) {
                    cit.lookupInDatabases(databases);
                }
            }
        }
    }

    public List<CitationGroup> getCitationGroupsUnordered() {
        return new ArrayList<>(citationGroupsUnordered.values());
    }

    /**
     * Citation groups in {@code globalOrder}
     */
    public List<CitationGroup> getCitationGroupsInGlobalOrder() {
        if (globalOrder.isEmpty()) {
            throw new RuntimeException("getCitationGroupsInGlobalOrder: not ordered yet");
        }
        return OOListUtil.map(globalOrder.get(), cgid -> citationGroupsUnordered.get(cgid));
    }

    /**
     * Impose an order of citation groups by providing the order
     * of their citation group idendifiers.
     *
     * Also set indexInGlobalOrder for each citation group.
     */
    public void setGlobalOrder(List<CitationGroupId> globalOrder) {
        Objects.requireNonNull(globalOrder);
        if (globalOrder.size() != numberOfCitationGroups()) {
            throw new RuntimeException("setGlobalOrder:"
                                       + " globalOrder.size() != numberOfCitationGroups()");
        }
        this.globalOrder = Optional.of(globalOrder);

        // Propagate to each CitationGroup
        int i = 0;
        for (CitationGroupId cgid : globalOrder) {
            citationGroupsUnordered.get(cgid).setIndexInGlobalOrder(Optional.of(i));
            i++;
        }
    }

    public boolean hasGlobalOrder() {
        return globalOrder.isPresent();
    }

    /**
     * Impose an order for citations within each group.
     */
    public void imposeLocalOrder(Comparator<BibEntry> entryComparator) {
        for (CitationGroup cg : citationGroupsUnordered.values()) {
            cg.imposeLocalOrder(entryComparator);
        }
    }

    /**
     * Collect citations into a list of cited sources using neither
     * CitationGroup.globalOrder or Citation.localOrder
     */
    public CitedKeys getCitedKeysUnordered() {
        LinkedHashMap<String, CitedKey> res = new LinkedHashMap<>();
        for (CitationGroup cg : citationGroupsUnordered.values()) {
            int storageIndexInGroup = 0;
            for (Citation cit : cg.citationsInStorageOrder) {
                String key = cit.citationKey;
                CitationPath p = new CitationPath(cg.cgid, storageIndexInGroup);
                if (res.containsKey(key)) {
                    res.get(key).addPath(p, cit);
                } else {
                    res.put(key, new CitedKey(key, p, cit));
                }
                storageIndexInGroup++;
            }
        }
        return new CitedKeys(res);
    }

    /**
     * CitedKeys created iterating citations in (globalOrder,localOrder)
     */
    public CitedKeys getCitedKeysSortedInOrderOfAppearance() {
        LinkedHashMap<String, CitedKey> res = new LinkedHashMap<>();
        if (!hasGlobalOrder()) {
            throw new RuntimeException("getSortedCitedKeys: no globalOrder");
        }
        for (CitationGroup cg : getCitationGroupsInGlobalOrder()) {
            for (int i : cg.getLocalOrder()) {
                Citation cit = cg.citationsInStorageOrder.get(i);
                String citationKey = cit.citationKey;
                CitationPath p = new CitationPath(cg.cgid, i);
                if (res.containsKey(citationKey)) {
                    res.get(citationKey).addPath(p, cit);
                } else {
                    res.put(citationKey, new CitedKey(citationKey, p, cit));
                }
            }
        }
        return new CitedKeys(res);
    }

    public Optional<CitedKeys> getBibliography() {
        return bibliography;
    }

    /**
     * @return Citation keys where lookupCitations() failed.
     */
    public List<String> getUnresolvedKeys() {

        CitedKeys bib = getBibliography().orElse(getCitedKeysUnordered());

        List<String> unresolvedKeys = new ArrayList<>();
        for (CitedKey ck : bib.values()) {
            if (ck.getLookupResult().isEmpty()) {
                unresolvedKeys.add(ck.citationKey);
            }
        }
        return unresolvedKeys;
    }

    public void createNumberedBibliographySortedInOrderOfAppearance() {
        if (!bibliography.isEmpty()) {
            throw new RuntimeException("createNumberedBibliographySortedInOrderOfAppearance:"
                                       + " already have a bibliography");
        }
        CitedKeys citedKeys = getCitedKeysSortedInOrderOfAppearance();
        citedKeys.numberCitedKeysInCurrentOrder();
        citedKeys.distributeNumbers(this);
        bibliography = Optional.of(citedKeys);
    }

    /**
     * precondition: database lookup already performed (otherwise we just sort citation keys)
     */
    public void createPlainBibliographySortedByComparator(Comparator<BibEntry> entryComparator) {
        if (!bibliography.isEmpty()) {
            throw new RuntimeException("createPlainBibliographySortedByComparator:"
                                       + " already have a bibliography");
        }
        CitedKeys citedKeys = getCitedKeysUnordered();
        citedKeys.sortByComparator(entryComparator);
        bibliography = Optional.of(citedKeys);
    }

    /**
     * precondition: database lookup already performed (otherwise we just sort citation keys)
     */
    public void createNumberedBibliographySortedByComparator(Comparator<BibEntry> entryComparator) {
        if (!bibliography.isEmpty()) {
            throw new RuntimeException("createNumberedBibliographySortedByComparator:"
                                       + " already have a bibliography");
        }
        CitedKeys citedKeys = getCitedKeysUnordered();
        citedKeys.sortByComparator(entryComparator);
        citedKeys.numberCitedKeysInCurrentOrder();
        citedKeys.distributeNumbers(this);
        bibliography = Optional.of(citedKeys);
    }

    /*
     * Query by CitationGroupId
     */

    public Optional<CitationGroup> getCitationGroup(CitationGroupId cgid) {
        CitationGroup cg = citationGroupsUnordered.get(cgid);
        return Optional.ofNullable(cg);
    }

    /**
     * Call this when the citation group is unquestionably there.
     */
    public CitationGroup getCitationGroupOrThrow(CitationGroupId cgid) {
        CitationGroup cg = citationGroupsUnordered.get(cgid);
        if (cg == null) {
            throw new RuntimeException("getCitationGroupOrThrow:"
                                       + " the requested CitationGroup is not available");
        }
        return cg;
    }

    /*
     * @return true if all citation groups have referenceMarkNameForLinking
     */
    public boolean citationGroupsProvideReferenceMarkNameForLinking() {
        for (CitationGroup cg : citationGroupsUnordered.values()) {
            if (cg.getReferenceMarkNameForLinking().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /*
     * Callbacks.
     */

    public void afterCreateCitationGroup(CitationGroup cg) {
        citationGroupsUnordered.put(cg.cgid, cg);

        globalOrder = Optional.empty();
        bibliography = Optional.empty();
    }

    public void afterRemoveCitationGroup(CitationGroup cg) {
        citationGroupsUnordered.remove(cg.cgid);
        globalOrder.map(l -> l.remove(cg.cgid));

        bibliography = Optional.empty();
    }

}
