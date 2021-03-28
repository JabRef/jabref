package org.jabref.gui.openoffice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.JabRefException;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XFootnote;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.UnoRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * At the start of GUI actions we may want to check the state of the document.
 *
 * Operations:
 *   createCitationGroup
 *   deleteCitationGroup
 *
 */
class CitationGroupsV001 {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(CitedKeys.class);

    private static final String BIB_CITATION = "JR_cite";
    private static final Pattern CITE_PATTERN =
            Pattern.compile(BIB_CITATION + "(\\d*)_(\\d*)_(.*)");

    private static final String ZERO_WIDTH_SPACE = "\u200b";

    // for debugging we may want visible bracket
    private static final boolean
    REFERENCE_MARK_USE_INVISIBLE_BRACKETS = true; // !debug;

    private static final String
    REFERENCE_MARK_LEFT_BRACKET = REFERENCE_MARK_USE_INVISIBLE_BRACKETS ? ZERO_WIDTH_SPACE : "<";

    private static final String
    REFERENCE_MARK_RIGHT_BRACKET = REFERENCE_MARK_USE_INVISIBLE_BRACKETS ? ZERO_WIDTH_SPACE : ">";


    /**
     *  Original CitationGroups Data
     */
    Map<CitationGroupID, CitationGroup> citationGroups;

    /**
     *  Extra Data
     */
    // For custom properties belonging to us, but
    // without a corresponding reference mark.
    // These can be deleted.
    private List<String> pageInfoThrash;

    private Optional<List<CitationGroupID>> globalOrder;

    private Optional<CitedKeys> citedKeysAfterDatabaseLookup;
    /**
     *  This is going to be the bibliography
     */
    private Optional<CitedKeys> bibliography;

    /**
     * Constructor
     */
    public CitationGroupsV001(DocumentConnection documentConnection)
        throws
        NoDocumentException,
        WrappedTargetException {

        // Get the citationGroupNames
        List<String> citationGroupNames = getJabRefReferenceMarkNames(documentConnection);

        this.pageInfoThrash = findUnusedJabrefPropertyNames(documentConnection,
                                                            citationGroupNames);

        this.citationGroups = readCitationGroupsFromDocument(documentConnection,
                                                             citationGroupNames);

        // Now we have almost every information from the document about citations.
        // What is left out: the ranges controlled by the reference marks.
        // But (I guess) those change too easily, so we only ask when actually needed.

        this.globalOrder = Optional.empty();
        this.citedKeysAfterDatabaseLookup = Optional.empty();
        this.bibliography = Optional.empty();
    }

    private static List<String> findUnusedJabrefPropertyNames(DocumentConnection documentConnection,
                                                              List<String> citationGroupNames) {
        // Collect unused jabrefPropertyNames
        Set<String> citationGroupNamesSet =
            citationGroupNames.stream().collect(Collectors.toSet());

        List<String> pageInfoThrash = new ArrayList<>();
        List<String> jabrefPropertyNames =
            documentConnection.getCustomPropertyNames()
            .stream()
            .filter(CitationGroupsV001::isJabRefReferenceMarkName)
            .collect(Collectors.toList());
        for (String pn : jabrefPropertyNames) {
            if (!citationGroupNamesSet.contains(pn)) {
                pageInfoThrash.add(pn);
            }
        }
        return pageInfoThrash;
    }

    private static Map<CitationGroupID, CitationGroup>
    readCitationGroupsFromDocument(DocumentConnection documentConnection,
                                   List<String> citationGroupNames)
        throws
        WrappedTargetException {

        Map<CitationGroupID, CitationGroup> citationGroups = new HashMap<>();
        for (int i = 0; i < citationGroupNames.size(); i++) {
            final String name = citationGroupNames.get(i);
            CitationGroup cg =
                readCitationGroupFromDocumentOrThrow(documentConnection, name);
            citationGroups.put(cg.cgid, cg);
        }
        return citationGroups;
    }

    private static CitationGroup
    readCitationGroupFromDocumentOrThrow(DocumentConnection documentConnection,
                                         String refMarkName)
        throws
        WrappedTargetException {

        Optional<ParsedRefMark> op = parseRefMarkName(refMarkName);
        if (op.isEmpty()) {
            // We have a problem. We want types[i] and bibtexKeys[i]
            // to correspond to referenceMarkNames.get(i).
            // And do not want null in bibtexKeys (or error code in types)
            // on return.
            throw new IllegalArgumentException(
                "citationGroups: found unparsable referenceMarkName");
        }
        ParsedRefMark ov = op.get();
        CitationGroupID id = new CitationGroupID(refMarkName);
        List<Citation> citations = ((ov.citationKeys == null)
                                    ? new ArrayList<>()
                                    : (ov.citationKeys.stream()
                                       .map(Citation::new)
                                       .collect(Collectors.toList())));

        Optional<String> pageInfo = documentConnection.getCustomProperty(refMarkName);

        CitationGroup cg = new CitationGroup(id,
                                             ov.itcType,
                                             citations,
                                             pageInfo,
                                             refMarkName);
        return cg;
    }

    class CitationPath {
        CitationGroupID group;
        int storageIndexInGroup;
        CitationPath(CitationGroupID group,
                     int storageIndexInGroup) {
            this.group = group;
            this.storageIndexInGroup = storageIndexInGroup;
        }
    }

    class DatabaseLookupResult {
        BibEntry entry;
        BibDatabase database;
        DatabaseLookupResult(BibEntry entry, BibDatabase database) {
            this.entry = entry;
            this.database = database;
        }
    }

    class CitedKey implements SortableCitation {
        String key;  // TODO: rename to citationKey
        LinkedHashSet<CitationPath> where;
        Optional<DatabaseLookupResult> db;
        Optional<Integer> number; // For Numbered citation styles.
        Optional<String> uniqueLetter; // For AuthorYear citation styles.
        Optional<String> normCitMarker;  // For AuthorYear citation styles.

        CitedKey(String key, CitationPath p, Citation cit) {
            this.key = key;
            this.where = new LinkedHashSet<>(); // remember order
            this.where.add(p);
            this.db = cit.db;
            this.number = cit.number;
            this.uniqueLetter = cit.uniqueLetter;
            this.normCitMarker = Optional.empty();
        }

        @Override
        public String getCitationKey(){
            return key;
        }

        @Override
        public Optional<BibEntry> getBibEntry(){
            return (db.isPresent()
                    ? Optional.of(db.get().entry)
                    : Optional.empty());
        }

        void addPath(CitationPath p, Citation cit) {
            this.where.add(p);
            if (cit.db != this.db) {
                throw new RuntimeException("CitedKey.addPath: mismatch on cit.db");
            }
            if (cit.number != this.number) {
                throw new RuntimeException("CitedKey.addPath: mismatch on cit.number");
            }
            if (cit.uniqueLetter != this.uniqueLetter) {
                throw new RuntimeException("CitedKey.addPath: mismatch on cit.uniqueLetter");
            }
        }

        void lookupInDatabases(List<BibDatabase> databases) {
            Optional<DatabaseLookupResult> res = Optional.empty();
            for (BibDatabase database : databases) {
                Optional<BibEntry> entry = database.getEntryByCitationKey(key);
                if (entry.isPresent()) {
                    res = Optional.of(new DatabaseLookupResult(entry.get(), database));
                    break;
                }
            }
            // store result
            this.db = res;
        }

        void distributeDatabaseLookupResult(CitationGroupsV001 cgs) {
            cgs.setDatabaseLookupResults(where, db);
        }

        void distributeNumber(CitationGroupsV001 cgs) {
            cgs.setNumbers(where, number);
        }

        void distributeUniqueLetter(CitationGroupsV001 cgs) {
            cgs.setUniqueLetters(where, uniqueLetter);
        }
    }

    interface SortableCitation {
        public String getCitationKey();
        public Optional<BibEntry> getBibEntry();
    }

    static class CitationComparator implements Comparator<SortableCitation> {

        Comparator<BibEntry> entryComparator;
        boolean unresolvedComesFirst;

        CitationComparator(Comparator<BibEntry> entryComparator,
                       boolean unresolvedComesFirst) {
            this.entryComparator = entryComparator;
            this.unresolvedComesFirst = unresolvedComesFirst;
        }

        public int compare(SortableCitation a, SortableCitation b) {
            Optional<BibEntry> abe = a.getBibEntry();
            Optional<BibEntry> bbe = b.getBibEntry();

            if (abe.isEmpty() && bbe.isEmpty()) {
                // Both are unresolved: compare them by citation key.
                String ack = a.getCitationKey();
                String bck = b.getCitationKey();
                return ack.compareTo(bck);
            }
            // Comparing unresolved and real entry

            final int mul = unresolvedComesFirst ? (+1) : (-1);
            if (abe.isEmpty()) {
                return -mul;
            }
            if (bbe.isEmpty()) {
                return mul;
            }
            // Proper comparison of entries
            return entryComparator.compare(abe.get(),
                                           bbe.get());
        }
    }

    class CitedKeys {

        /**
         * Order-preserving map from citation keys to associated data.
         */
        LinkedHashMap<String, CitedKey> data;

        CitedKeys(LinkedHashMap<String, CitedKey> data) {
            this.data = data;
        }

        /**
         *  The cited keys in sorted order.
         */
        public List<CitedKey> values() {
            return new ArrayList<>(data.values());
        }

        /**
         * Sort entries for the bibliography.
         */
        /*
        class SortCitedKeys implements Comparator<CitedKey> {

            Comparator<BibEntry> entryComparator;
            boolean unresolvedComesFirst;

            SortCitedKeys(Comparator<BibEntry> entryComparator,
                          boolean unresolvedComesFirst) {
                this.entryComparator = entryComparator;
                this.unresolvedComesFirst = unresolvedComesFirst;
            }

            public int compare(CitedKey a, CitedKey b) {
                if (a.db.isEmpty() && b.db.isEmpty()) {
                    // Both are unresolved: compare them by citation key.
                    return a.key.compareTo(b.key);
                }
                // Comparing unresolved and real entry
                final int mul = unresolvedComesFirst ? (+1) : (-1);
                if (a.db.isEmpty()) {
                    return -mul;
                }
                if (b.db.isEmpty()) {
                    return mul;
                }
                // Proper comparison of entries
                return entryComparator.compare(a.db.get().entry,
                                               b.db.get().entry);
            }
        }
        */

        void sortByComparator(Comparator<BibEntry> entryComparator) {
            List<CitedKey> cks = new ArrayList<>(data.values());
            cks.sort(new CitationComparator(entryComparator, true));
            LinkedHashMap<String, CitedKey> newData = new LinkedHashMap<>();
            for (CitedKey ck : cks) {
                newData.put(ck.key, ck);
            }
            data = newData;
        }

        void numberCitedKeysInCurrentOrder() {
            int i = 1;
            for (CitedKey ck : data.values()) {
                ck.number = Optional.of(i); // was: -1 for UndefinedBibtexEntry
                i++;
            }
        }

        void lookupInDatabases(List<BibDatabase> databases) {
            for (CitedKey ck : this.data.values()) {
                ck.lookupInDatabases(databases);
            }
        }

        void distributeDatabaseLookupResults(CitationGroupsV001 cgs) {
            for (CitedKey ck : this.data.values()) {
                ck.distributeDatabaseLookupResult(cgs);
            }
        }

        void distributeNumbers(CitationGroupsV001 cgs) {
            for (CitedKey ck : this.data.values()) {
                ck.distributeNumber(cgs);
            }
        }

        void distributeUniqueLetters(CitationGroupsV001 cgs) {
            for (CitedKey ck : this.data.values()) {
                ck.distributeUniqueLetter(cgs);
            }
        }

    }

    public void setDatabaseLookupResults(Set<CitationPath> where,
                                         Optional<DatabaseLookupResult> db) {
        for (CitationPath p : where) {
            CitationGroup cg = this.citationGroups.get(p.group);
            if (cg == null) {
                LOGGER.warn("CitationGroups.setDatabaseLookupResult: group missing");
                continue;
            }
            Citation cit = cg.citations.get(p.storageIndexInGroup);
            cit.db = db;
        }
    }

    public CitationGroupsV001.CitedKeys lookupEntriesInDatabases(List<BibDatabase> databases) {
        CitationGroupsV001 cgs = this;

        CitationGroupsV001.CitedKeys cks = cgs.getCitedKeys();

        cks.lookupInDatabases(databases);
        cks.distributeDatabaseLookupResults(cgs);
        // record we did a database lookup
        // and allow extracting unresolved keys.
        this.citedKeysAfterDatabaseLookup = Optional.of(cks);
        return cks;
    }

    public void setNumbers(Set<CitationPath> where,
                           Optional<Integer> number) {
        for (CitationPath p : where) {
            CitationGroup cg = this.citationGroups.get(p.group);
            if (cg == null) {
                LOGGER.warn("CitationGroups.setNumbers: group missing");
                continue;
            }
            Citation cit = cg.citations.get(p.storageIndexInGroup);
            cit.number = number;
        }
    }

    public void setUniqueLetters(Set<CitationPath> where,
                                 Optional<String> uniqueLetter) {
        for (CitationPath p : where) {
            CitationGroup cg = this.citationGroups.get(p.group);
            if (cg == null) {
                LOGGER.warn("CitationGroups.setUniqueLetters: group missing");
                continue;
            }
            Citation cit = cg.citations.get(p.storageIndexInGroup);
            cit.uniqueLetter = uniqueLetter;
        }
    }

    public void imposeLocalOrderByComparator(Comparator<BibEntry> entryComparator) {
        for (CitationGroup cg : citationGroups.values()) {
            cg.imposeLocalOrderByComparator(entryComparator);
        }
    }

    public CitedKeys getCitedKeys() {
        LinkedHashMap<String, CitedKey> res = new LinkedHashMap<>();
        for (CitationGroup cg : citationGroups.values()) {
            int storageIndexInGroup = 0;
            for (Citation cit : cg.citations) {
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
        if (globalOrder.isEmpty()) {
            throw new RuntimeException("getSortedCitedKeys: no globalOrder");
        }
        for (CitationGroupID cgid : globalOrder.get()) {
            CitationGroup cg = getCitationGroup(cgid)
                .orElseThrow(RuntimeException::new);
            for (int i : cg.localOrder) {
                Citation cit = cg.citations.get(i);
                String citationKey = cit.citationKey;
                CitationPath p = new CitationPath(cgid, i);
                if (res.containsKey(citationKey)) {
                    res.get(citationKey).addPath(p, cit);
                } else {
                    res.put(citationKey, new CitedKey(citationKey, p, cit));
                }
            }
        }
        return new CitedKeys(res);
    }

    Optional<CitedKeys> getBibliography() {
        return bibliography;
    }

    public void createNumberedBibliographySortedInOrderOfAppearance() {
        CitationGroupsV001 cgs = this;
        if (!cgs.bibliography.isEmpty()) {
            throw new RuntimeException(
                "createNumberedBibliographySortedInOrderOfAppearance: already have a bibliography");
        }
        CitationGroupsV001.CitedKeys sortedCitedKeys =
            cgs.getCitedKeysSortedInOrderOfAppearance();
        sortedCitedKeys.numberCitedKeysInCurrentOrder();
        sortedCitedKeys.distributeNumbers(cgs);
        cgs.bibliography = Optional.of(sortedCitedKeys);
    }

    public void createNumberedBibliographySortedByComparator(Comparator<BibEntry> entryComparator) {
        CitationGroupsV001 cgs = this;
        if (!cgs.bibliography.isEmpty()) {
            throw new RuntimeException(
                "createNumberedBibliographySortedByComparator: already have a bibliography");
        }
        CitationGroupsV001.CitedKeys citedKeys = cgs.getCitedKeys();
        citedKeys.sortByComparator(entryComparator); // TODO: must be after database lookup
        citedKeys.numberCitedKeysInCurrentOrder();
        citedKeys.distributeNumbers(cgs);
        this.bibliography = Optional.of(citedKeys);
    }

    public void
    createPlainBibliographySortedByComparator(Comparator<BibEntry> entryComparator) {
        CitationGroupsV001 cgs = this;
        if (!this.bibliography.isEmpty()) {
            throw new RuntimeException(
                "createPlainBibliographySortedByComparator: already have a bibliography");
        }
        CitationGroupsV001.CitedKeys citedKeys = cgs.getCitedKeys();
        citedKeys.sortByComparator(entryComparator); // TODO: must be after database lookup
        // citedKeys.numberCitedKeysInCurrentOrder();
        // citedKeys.distributeNumbers();
        this.bibliography = Optional.of(citedKeys);
    }

    public Set<CitationGroupID>
    getCitationGroupIDs() {
        return citationGroups.keySet();
    }

    /**
     * Creates a list of {@code
     * RangeSortable<CitationGroupsV001.CitationGroupID>} values for
     * our {@code CitationGroup} values. Originally designed to be
     * passed to {@code visualSort}.
     *
     * The elements of the returned list are actually of type {@code
     * RangeSortEntry<CitationGroupID>}.
     *
     * The result is sorted within {@code XTextRange.getText()}
     * partitions of the citation groups according to their {@code
     * XTextRange} (before mapping to footnote marks).
     *
     * In the result, RangeSortable.getIndexInPosition() contains
     * unique indexes within the original partition (not after
     * mapFootnotesToFootnoteMarks).
     *
     * @param cgs The source of CitationGroup values.
     * @param documentConnection Connection to the document.
     * @param mapFootnotesToFootnoteMarks If true, replace ranges in
     *        footnotes with the range of the corresponding footnote
     *        mark. This is used for numbering the citations.
     *
     */
    private static List<RangeSort.RangeSortable<CitationGroupsV001.CitationGroupID>>
    createVisualSortInput(CitationGroupsV001 cgs,
                          DocumentConnection documentConnection,
                          boolean mapFootnotesToFootnoteMarks)
        throws
        NoDocumentException,
        WrappedTargetException {

        List<CitationGroupsV001.CitationGroupID> cgids =
            new ArrayList<>(cgs.getCitationGroupIDs());

        List<RangeSort.RangeSortEntry> vses = new ArrayList<>();
        for (CitationGroupsV001.CitationGroupID cgid : cgids) {
            XTextRange range = cgs.getReferenceMarkRangeOrNull(documentConnection, cgid);
            if (range == null) {
                throw new RuntimeException("getReferenceMarkRangeOrNull returned null");
            }
            vses.add(new RangeSort.RangeSortEntry(range, 0, cgid));
        }

        /*
         *  At this point we are almost ready to return vses.
         *
         *  For example we may want to number citations in a footnote
         *  as if it appeared where the footnote mark is.
         *
         *  The following code replaces ranges within footnotes with
         *  the range for the corresponding footnote mark.
         *
         *  This brings further ambiguity if we have multiple
         *  citations within the same footnote: for the comparison
         *  they become indistinguishable. Numbering between them is
         *  not controlled. Also combineCiteMarkers will see them in
         *  the wrong order (if we use this comparison), and will not
         *  be able to merge. To avoid these, we sort textually within
         *  each .getText() partition and add indexInPosition
         *  accordingly.
         *
         */

        // Sort within partitions
        RangeKeyedMapList<RangeSort.RangeSortEntry<CitationGroupsV001.CitationGroupID>> xxs
            = new RangeKeyedMapList<>();

        for (RangeSort.RangeSortEntry v : vses) {
            xxs.add(v.getRange(), v);
        }

        // build final list
        List<RangeSort.RangeSortEntry<CitationGroupsV001.CitationGroupID>> res = new ArrayList<>();

        for (TreeMap<XTextRange, List<RangeSort.RangeSortEntry<CitationGroupsV001.CitationGroupID>>>
                 xs : xxs.partitionValues()) {

            List<XTextRange> oxs = new ArrayList<>(xs.keySet());

            int indexInPartition = 0;
            for (int i = 0; i < oxs.size(); i++) {
                XTextRange a = oxs.get(i);
                List<RangeSort.RangeSortEntry<CitationGroupsV001.CitationGroupID>> avs = xs.get(a);
                for (int j = 0; j < avs.size(); j++) {
                    RangeSort.RangeSortEntry<CitationGroupsV001.CitationGroupID> v = avs.get(j);
                    v.indexInPosition = indexInPartition++;
                    if (mapFootnotesToFootnoteMarks) {
                        // Adjust range if we are inside a footnote:
                        if (unoQI(XFootnote.class, v.range.getText()) != null) {
                            // Find the linking footnote marker:
                            XFootnote footer = unoQI(XFootnote.class, v.range.getText());
                            // The footnote's anchor gives the correct position in the text:
                            v.range = footer.getAnchor();
                        }
                    }
                    res.add(v);
                }
            }
        }
        // convert
        // List<RangeSortEntry<CitationGroupsV001.CitationGroupID>>
        // to
        // List<RangeSortable<CitationGroupsV001.CitationGroupID>>
        return res.stream().map(e -> e).collect(Collectors.toList());
    }

    /**
     *  Return JabRef reference mark names sorted by their visual positions.
     *
     *  @param mapFootnotesToFootnoteMarks If true, sort reference
     *         marks in footnotes as if they appeared at the
     *         corresponding footnote mark.
     *
     *  @return JabRef reference mark names sorted by these positions.
     *
     *  Limitation: for two column layout visual (top-down,
     *        left-right) order does not match the expected (textual)
     *        order.
     *
     */
    public List<CitationGroupsV001.CitationGroupID>
    getVisuallySortedCitationGroupIDs(DocumentConnection documentConnection,
                                      boolean mapFootnotesToFootnoteMarks)
        throws
        WrappedTargetException,
        NoDocumentException,
        JabRefException {
        CitationGroupsV001 cgs = this;
        List<RangeSort.RangeSortable<CitationGroupsV001.CitationGroupID>> vses =
            createVisualSortInput(cgs,
                                  documentConnection,
                                  mapFootnotesToFootnoteMarks);

        if (vses.size() != cgs.citationGroups.size()) {
            throw new RuntimeException("getVisuallySortedCitationGroupIDs:"
                                       + " vses.size() != cgs.citationGroups.size()");
        }

        String messageOnFailureToObtainAFunctionalXTextViewCursor =
            Localization.lang("Please move the cursor into the document text.")
            + "\n"
            + Localization.lang("To get the visual positions of your citations"
                                + " I need to move the cursor around,"
                                + " but could not get it.");
        List<RangeSort.RangeSortable<CitationGroupsV001.CitationGroupID>> sorted =
            RangeSortVisual.visualSort(vses,
                                       documentConnection,
                                       messageOnFailureToObtainAFunctionalXTextViewCursor);

        if (sorted.size() != cgs.citationGroups.size()) {
            // This Fired
            throw new RuntimeException("getVisuallySortedCitationGroupIDs:"
                                       + " sorted.size() != cgs.citationGroups.size()");
        }

        return (sorted.stream()
                .map(e -> e.getContent())
                .collect(Collectors.toList()));
    }

    /**
     * Calculate and return citation group IDs in visual order.
     */
    public List<CitationGroupID>
    getCitationGroupIDsSortedWithinPartitions(DocumentConnection documentConnection,
                                              boolean mapFootnotesToFootnoteMarks)
        throws
        NoDocumentException,
        WrappedTargetException {
        // This is like getVisuallySortedCitationGroupIDs,
        // but we skip the visualSort part.
        CitationGroupsV001 cgs = this;
        // boolean mapFootnotesToFootnoteMarks = false;
        List<RangeSort.RangeSortable<CitationGroupID>> vses =
            CitationGroupsV001.createVisualSortInput(cgs,
                                                     documentConnection,
                                                     mapFootnotesToFootnoteMarks);

        if (vses.size() != cgs.citationGroups.size()) {
            throw new RuntimeException("getCitationGroupIDsSortedWithinPartitions:"
                                       + " vses.size() != cgs.citationGroups.size()");
        }
        return (vses.stream()
                .map(e -> e.getContent())
                .collect(Collectors.toList()));
    }

    /**
     * Citation group IDs in {@code globalOrder}
     */
    public List<CitationGroupID>
    getSortedCitationGroupIDs() {
        if (globalOrder.isEmpty()) {
            throw new RuntimeException("getSortedCitationGroupIDs: not ordered yet");
        }
        return globalOrder.get();
    }

    public void
    setGlobalOrder(List<CitationGroupID> globalOrder) {
        Objects.requireNonNull(globalOrder);
        if (globalOrder.size() != citationGroups.size()) {
            throw new RuntimeException(
                "CitationGroupsV001.setGlobalOrder: globalOrder.size() != citationGroups.size()");
        }
        this.globalOrder = Optional.of(globalOrder);
    }

    public Optional<CitationGroup> getCitationGroup(CitationGroupID cgid) {
        CitationGroup e = citationGroups.get(cgid);
        return Optional.ofNullable(e);
    }

    /**
     * Call this when the citation group is unquestionably there.
     */
    public CitationGroup getCitationGroupOrThrow(CitationGroupID cgid) {
        CitationGroup e = citationGroups.get(cgid);
        if (e == null) {
            throw new RuntimeException("CitationGroupsV001.getCitationGroupOrThrow:"
                                       + " the requested CitationGroup is not available");
        }
        return e;
    }

    private Optional<String> getReferenceMarkName(CitationGroupID cgid) {
        return getCitationGroup(cgid).map(cg -> cg.referenceMarkName);
    }

    private Optional<Integer> getItcType(CitationGroupID cgid) {
        return getCitationGroup(cgid).map(cg -> cg.itcType);
    }

    public int numberOfCitationGroups() {
        return citationGroups.size();
    }

    public Optional<String> getPageInfo(CitationGroupID cgid) {
        return (getCitationGroup(cgid)
                .map(cg -> cg.pageInfo)
                .flatMap(x -> x));
    }

    public Optional<List<Citation>>
    getCitations(CitationGroupID cgid) {
        return getCitationGroup(cgid).map(cg -> cg.citations);
    }

    public List<Citation>
    getSortedCitations(CitationGroupID cgid) {
        Optional<CitationGroup> cg = getCitationGroup(cgid);
        if (cg.isEmpty()) {
            throw new RuntimeException("getSortedCitations: invalid cgid");
        }
        return cg.get().getSortedCitations();
    }

    /* ***********************************
     *
     *  modifies both storage and presentation
     *
     * ***********************************/

    /**
     *  Insert {@code n} spaces in a way that reference
     *  marks just before or just after the cursor are not affected.
     *
     *  This is based on the observation, that starting two
     *  new paragraphs separates us from a reference mark on either side.
     *
     *  The pattern used is:
     *  {@code safeInsertSpaces(n): para, para, left, space(n), right-delete, left(n), left-delete}
     *
     *  @param position Where to insert (at position.getStart())
     *  @param n  Number of spaces to insert.
     *
     *  @return a new cursor, covering the just-inserted spaces.
     *
     *  This could be generalized to insert arbitrary text safely
     *  between two reference marks. But we do not need that now.
     */
    private static XTextCursor
    safeInsertSpacesBetweenReferenceMarks(XTextRange position, int n) {
        // Start with an empty cursor at position.getStart();
        XText text = position.getText();
        XTextCursor cursor = text.createTextCursorByRange(position.getStart());
        text.insertString(cursor, "\r\r", false); // para, para
        cursor.goLeft((short) 1, false); // left
        text.insertString(cursor, " ".repeat(n), false); // space(n)
        cursor.goRight((short) 1, true);
        cursor.setString(""); // right-delete
        cursor.goLeft((short) n, false); // left(n)
        cursor.goLeft((short) 1, true);
        cursor.setString(""); // left-delete
        cursor.goRight((short) n, true);
        return cursor;
    }

    /**
     *  Create a reference mark with the given name, at the
     *  end of position.
     *
     *  To reduce the difference from the original representation, we
     *  only insist on having at least two characters inside reference
     *  marks. These may be ZERO_WIDTH_SPACE characters or other
     *  placeholder not likely to appear in a citation mark.
     *
     *  This placeholder is only needed if the citation mark is
     *  otherwise empty (e.g. when we just create it).
     *
     *  getFillCursorForCitationGroup yields a bracketed cursor, that
     *  can be used to fill in / overwrite the value inside.
     *
     *  After each getFillCursorForCitationGroup, we require a call to
     *  cleanFillCursorForCitationGroup, which removes the brackets,
     *  unless if it would make the content less than two
     *  characters. If we need only one placeholder, we keep the left
     *  bracket.  If we need two, then the content is empty. The
     *  removeBracketsFromEmpty parameter of
     *  cleanFillCursorForCitationGroup overrides this, and for empty
     *  citations it will remove the brackets, leaving an empty
     *  reference mark. The idea behind this is that we do not need to
     *  refill empty marks (itcTypes INVISIBLE_CIT), and the caller
     *  can tell us that we are dealing with one of these.
     *
     *  Thus the only user-visible difference in citation marks is
     *  that instead of empty marks we use two brackets, for
     *  single-character marks we add a left bracket before.
     *
     *  Character-attribute inheritance: updates inherit from the
     *  first character inside, not from the left.
     *
     *  On return {@code position} is collapsed, and is after the
     *  inserted space, or at the end of the reference mark.
     *
     *  @param documentConnection Connection to document.
     *  @param position Collapsed to its end.
     *  @param insertSpaceAfter We insert a space after the mark, that
     *                          carries on format of characters from
     *                          the original position.
     *
     *  @param withoutBrackets  Force empty reference mark (no brackets).
     *                          For use with INVISIBLE_CIT.
     *
     */
    public CitationGroupID createCitationGroup(DocumentConnection documentConnection,
                                               List<String> citationKeys,
                                               Optional<String> pageInfo,
                                               int itcType,
                                               XTextCursor position,
                                               boolean insertSpaceAfter,
                                               boolean withoutBrackets)
        throws
        CreationException,
        NoDocumentException,
        WrappedTargetException {

        String xkey =
            citationKeys.stream()
            .collect(Collectors.joining(","));

        String refMarkName =
            getUniqueReferenceMarkName(documentConnection,
                                       xkey,
                                       itcType);

        CitationGroupID cgid = new CitationGroupID(refMarkName);

        List<Citation> citations =
            citationKeys.stream()
            .map(Citation::new)
            .collect(Collectors.toList());

        /*
        new ArrayList<>(citationKeys.size());
        for (int j = 0; j < ov.citationKeys.size(); j++) {
            citatitons.add(new Citation(citationKeys.get(j)));
        }
        */

        CitationGroup cg = new CitationGroup(cgid,
                                             itcType,
                                             citations,
                                             pageInfo,
                                             refMarkName);

        // add to our data
        this.citationGroups.put(cgid, cg);
        // invalidate globalOrder.
        // TODO: look out for localOrder!
        this.globalOrder = Optional.empty();

        /*
         * Apply to document
         */

        createReferenceMarkForCitationGroup(
            documentConnection,
            refMarkName,
            position,
            insertSpaceAfter,
            withoutBrackets);
        return cgid;
    }

    private static void createReferenceMarkForCitationGroup(DocumentConnection documentConnection,
                                                            String refMarkName,
                                                            XTextCursor position,
                                                            boolean insertSpaceAfter,
                                                            boolean withoutBrackets)
        throws
        CreationException {

        // The cursor we received: we push it before us.
        position.collapseToEnd();

        XTextCursor cursor = safeInsertSpacesBetweenReferenceMarks(position.getEnd(), 2);

        // cursors before the first and after the last space
        XTextCursor cursorBefore =
            cursor.getText().createTextCursorByRange(cursor.getStart());
        XTextCursor cursorAfter =
            cursor.getText().createTextCursorByRange(cursor.getEnd());

        cursor.collapseToStart();
        cursor.goRight((short) 1, false);
        // now we are between two spaces

        final String left = REFERENCE_MARK_LEFT_BRACKET;
        final String right = REFERENCE_MARK_RIGHT_BRACKET;
        final short leftLength = (short) left.length();
        final short rightLength = (short) right.length();
        String bracketedContent = (withoutBrackets
                                   ? ""
                                   : left + right);

        cursor.getText().insertString(
            cursor,
            bracketedContent,
            true);

        documentConnection.insertReferenceMark(refMarkName,
                                               cursor,
                                               true /* absorb */);

        cursorBefore.goRight((short) 1, true);
        cursorBefore.setString("");
        if (!insertSpaceAfter) {
            cursorAfter.goLeft((short) 1, true);
            cursorAfter.setString("");
        }
    }

    /*
     * Remove it from the {@code this} and the document.
     *
     * TODO: either invalidate or update the extra data we are storing
     *       (bibliography). Update may be complicated, since we do
     *       not know how the bibliography was generated: it was partially done
     *       outside CitationGroupsV001, and we did not store how.
     */
    public void removeCitationGroups(List<CitationGroup> cgs, DocumentConnection documentConnection)
        throws
        WrappedTargetException,
        NoDocumentException,
        NoSuchElementException {

        for (CitationGroup cg : cgs) {
            documentConnection.removeReferenceMark(cg.referenceMarkName);
            this.citationGroups.remove(cg.cgid);
            this.globalOrder.map(l -> l.remove(cg.cgid));

            // Invalidate CitedKeys
            this.citedKeysAfterDatabaseLookup = Optional.empty();
            this.bibliography = Optional.empty();
            /*
             * this.citedKeysAfterDatabaseLookup.map(cks -> cks.forgetCitationGroup(cg.cgid));
             * this.bibliography.map(cks -> cks.forgetCitationGroup(cg.cgid));
             */
        }
    }

    public void removeCitationGroup(CitationGroup cg, DocumentConnection documentConnection)
        throws
        WrappedTargetException,
        NoDocumentException,
        NoSuchElementException {

        removeCitationGroups(Collections.singletonList(cg), documentConnection);
    }

    /**
     * Remove brackets, but if the result would become empty, leave
     * them; if the result would be a single characer, leave the left bracket.
     *
     * @param removeBracketsFromEmpty is intended to force removal if
     *        we are working on an "Empty citation" (INVISIBLE_CIT).
     */
    public void cleanFillCursorForCitationGroup(DocumentConnection documentConnection,
                                                CitationGroupID cgid,
                                                boolean removeBracketsFromEmpty)
        throws
        NoDocumentException,
        WrappedTargetException,
        CreationException {

        boolean alwaysRemoveBrackets = true;
        final String left = REFERENCE_MARK_LEFT_BRACKET;
        final String right = REFERENCE_MARK_RIGHT_BRACKET;
        final short leftLength = (short) left.length();
        final short rightLength = (short) right.length();

        CitationGroupsV001 cgs = this;
        String name = cgs.getReferenceMarkName(cgid).orElseThrow(RuntimeException::new);

        XTextCursor full = cgs.getRawCursorForCitationGroup(cgid, documentConnection);
        final String fullText = full.getString();
        final int fullTextLength = fullText.length();

        XTextCursor alpha = full.getText().createTextCursorByRange(full);
        alpha.collapseToStart();

        XTextCursor beta = full.getText().createTextCursorByRange(full);
        beta.collapseToStart();
        beta.goRight(leftLength, false);

        XTextCursor omega = full.getText().createTextCursorByRange(full);
        omega.collapseToEnd();

        if (!fullText.startsWith(left)) {
            throw new RuntimeException(
                String.format("cleanFillCursorForCitationGroup:"
                              + " (%s) does not start with REFERENCE_MARK_LEFT_BRACKET",
                              name));
        }

        if (!fullText.endsWith(right)) {
            throw new RuntimeException(
                String.format("cleanFillCursorForCitationGroup:"
                              + " (%s) does not end with REFERENCE_MARK_RIGHT_BRACKET",
                              name));
        }

        final int contentLength = (fullTextLength - (leftLength + rightLength));
        if (contentLength < 0) {
            throw new RuntimeException(
                String.format("cleanFillCursorForCitationGroup: length(%s) < 0",
                              name));
        }

        boolean removeRight = ((contentLength >= 1)
                               || ((contentLength == 0) && removeBracketsFromEmpty)
                               || alwaysRemoveBrackets);

        boolean removeLeft = ((contentLength >= 2)
                              || ((contentLength == 0) && removeBracketsFromEmpty)
                              || alwaysRemoveBrackets);

        if (removeRight) {
            omega.goLeft(rightLength, true);
            omega.setString("");
        }

        if (removeLeft) {
            alpha.goRight(leftLength, true);
            alpha.setString("");
        }
    }

    /**
     * Cursor for the reference marks as is, not prepared for filling,
     * but does not need cleanFillCursorForCitationGroup either.
     */
    public XTextCursor getRawCursorForCitationGroup(CitationGroupID cgid,
                                                    DocumentConnection documentConnection)
        throws
        NoDocumentException,
        WrappedTargetException,
        CreationException {

        String name = this.getReferenceMarkName(cgid).orElseThrow(RuntimeException::new);
        XTextCursor full = null;

        XTextContent markAsTextContent =
            documentConnection.getReferenceMarkAsTextContentOrNull(name);

        if (markAsTextContent == null) {
            throw new RuntimeException(
                String.format(
                    "getRawCursorForCitationGroup: markAsTextContent(%s) == null",
                    name));
        }

        full = DocumentConnection.getTextCursorOfTextContent(markAsTextContent);
        if (full == null) {
            throw new RuntimeException("getRawCursorForCitationGroup: full == null");
        }
        return full;
    }

    public XTextCursor getFillCursorForCitationGroup(DocumentConnection documentConnection,
                                                     CitationGroupID cgid)
        throws
        NoDocumentException,
        WrappedTargetException,
        CreationException {

        String name = this.getReferenceMarkName(cgid).orElseThrow(RuntimeException::new);

        final boolean debugThisFun = false;
        final String left = REFERENCE_MARK_LEFT_BRACKET;
        final String right = REFERENCE_MARK_RIGHT_BRACKET;
        final short leftLength = (short) left.length();
        final short rightLength = (short) right.length();

        XTextCursor full = null;
        String fullText = null;
        for (int i = 1; i <= 2; i++) {
            XTextContent markAsTextContent =
                documentConnection.getReferenceMarkAsTextContentOrNull(name);

            if (markAsTextContent == null) {
                throw new RuntimeException(
                    String.format("getFillCursorForCitationGroup:"
                                  + " markAsTextContent(%s) == null (attempt %d)",
                                  name,
                                  i));
            }

            full = DocumentConnection.getTextCursorOfTextContent(markAsTextContent);
            if (full == null) {
                throw new RuntimeException(
                    String.format("getFillCursorForCitationGroup: full == null (attempt %d)", i));
            }

            fullText = full.getString();

            if (debugThisFun) {
                System.out.printf("getFillCursor: fulltext = '%s'%n", fullText);
            }

            if (fullText.length() >= 2) {
                break;
            } else {
                // (fullText.length() < 2)
                if (i == 2) {
                    throw new RuntimeException(
                        String.format("getFillCursorForCitationGroup:"
                                      + " (fullText.length() < 2) (attempt %d)",
                                      i));
                }
                // too short, recreate
                if (debugThisFun) {
                    System.out.println("getFillCursor: too short, recreate");
                }
                full.setString("");
                try {
                    documentConnection.removeReferenceMark(name);
                } catch (NoSuchElementException ex) {
                    LOGGER.warn(
                        String.format("getFillCursorForCitationGroup got NoSuchElementException"
                                      + " for '%s'",
                                      name));
                }
                createReferenceMarkForCitationGroup(
                    documentConnection,
                    name,
                    full,
                    false, /* insertSpaceAfter */
                    false  /* withoutBrackets */);
            }
        }

        if (full == null) {
            throw new RuntimeException("getFillCursorForCitationGroup: full == null (after loop)");
        }
        if (fullText == null) {
            throw new RuntimeException("getFillCursorForCitationGroup: fullText == null (after loop)");
        }

        // we have at least two characters inside
        XTextCursor alpha = full.getText().createTextCursorByRange(full);
        alpha.collapseToStart();
        XTextCursor omega = full.getText().createTextCursorByRange(full);
        omega.collapseToEnd();

        XTextCursor beta = full.getText().createTextCursorByRange(full);
        beta.collapseToStart();
        beta.goRight((short) 1, false);
        beta.goRight((short) (fullText.length() - 2), true);
        beta.setString(left + right);
        beta.collapseToEnd();
        beta.goLeft(rightLength, false);
        // drop the initial character
        alpha.goRight((short) 1, true);
        alpha.setString("");
        // drop the last character
        omega.goLeft((short) 1, true);
        omega.setString("");
        return beta;
    }

    /**
     * Produce a reference mark name for JabRef for the given citation
     * key and itcType that does not yet appear among the reference
     * marks of the document.
     *
     * @param bibtexKey The citation key.
     * @param itcType   Encodes the effect of withText and
     *                  inParenthesis options.
     *
     * The first occurrence of bibtexKey gets no serial number, the
     * second gets 0, the third 1 ...
     *
     * Or the first unused in this series, after removals.
     */
    private String getUniqueReferenceMarkName(DocumentConnection documentConnection,
                                              String bibtexKey,
                                              int itcType)
        throws NoDocumentException {

        XNameAccess xNamedRefMarks = documentConnection.getReferenceMarks();
        int i = 0;
        String name = BIB_CITATION + '_' + itcType + '_' + bibtexKey;
        while (xNamedRefMarks.hasByName(name)) {
            name = BIB_CITATION + i + '_' + itcType + '_' + bibtexKey;
            i++;
        }
        return name;
    }

    /**
     * This is what we get back from parsing a refMarkName.
     *
     */
    private static class ParsedRefMark {
        /**  "", "0", "1" ... */
        public String i;
        /** in-text-citation type */
        public int itcType;
        /** Citation keys embedded in the reference mark. */
        public List<String> citationKeys;

        ParsedRefMark(String i, int itcType, List<String> citationKeys) {
            this.i = i;
            this.itcType = itcType;
            this.citationKeys = citationKeys;
        }
    }

    /**
     * Parse a JabRef reference mark name.
     *
     * @return Optional.empty() on failure.
     *
     */
    private static Optional<ParsedRefMark> parseRefMarkName(String refMarkName) {

        Matcher citeMatcher = CITE_PATTERN.matcher(refMarkName);
        if (!citeMatcher.find()) {
            return Optional.empty();
        }

        List<String> keys = Arrays.asList(citeMatcher.group(3).split(","));
        String i = citeMatcher.group(1);
        int itcType = Integer.parseInt(citeMatcher.group(2));
        return (Optional.of(new CitationGroupsV001.ParsedRefMark(i, itcType, keys)));
    }

    /**
     * Extract the list of citation keys from a reference mark name.
     *
     * @param name The reference mark name.
     * @return The list of citation keys encoded in the name.
     *
     *         In case of duplicated citation keys,
     *         only the first occurrence.
     *         Otherwise their order is preserved.
     *
     *         If name does not match CITE_PATTERN,
     *         an empty list of strings is returned.
     */
    private List<String> parseRefMarkNameToUniqueCitationKeys(String name) {
        Optional<ParsedRefMark> op = parseRefMarkName(name);
        return (op.map(parsedRefMark ->
                       parsedRefMark.citationKeys.stream()
                       .distinct()
                       .collect(Collectors.toList()))
                .orElseGet(ArrayList::new));
    }

    /**
     * @return true if name matches the pattern used for JabRef
     * reference mark names.
     */
    private static boolean isJabRefReferenceMarkName(String name) {
        return (CITE_PATTERN.matcher(name).find());
    }

    /**
     * Filter a list of reference mark names by `isJabRefReferenceMarkName`
     *
     * @param names The list to be filtered.
     */
    private static List<String> filterIsJabRefReferenceMarkName(List<String> names) {
        return (names
                .stream()
                .filter(CitationGroupsV001::isJabRefReferenceMarkName)
                .collect(Collectors.toList()));
    }

    /**
     * Get reference mark names from the document matching the pattern
     * used for JabRef reference mark names.
     *
     * Note: the names returned are in arbitrary order.
     *
     *
     *
     */
    private List<String> getJabRefReferenceMarkNames(DocumentConnection documentConnection)
        throws
        NoDocumentException {
        List<String> allNames = documentConnection.getReferenceMarkNames();
        return filterIsJabRefReferenceMarkName(allNames);
    }

    /**
     * For each name in referenceMarkNames set types[i] and
     * bibtexKeys[i] to values parsed from referenceMarkNames.get(i)
     *
     * @param referenceMarkNames Should only contain parsable names.
     * @param types              OUT Must be same length as referenceMarkNames.
     * @param bibtexKeys         OUT First level must be same length as referenceMarkNames.
     */
    private static void parseRefMarkNamesToArrays(List<String> referenceMarkNames,
                                                  int[] types,
                                                  String[][] bibtexKeys) {

        final int nRefMarks = referenceMarkNames.size();
        assert (types.length == nRefMarks);
        assert (bibtexKeys.length == nRefMarks);
        for (int i = 0; i < nRefMarks; i++) {
            final String name = referenceMarkNames.get(i);
            Optional<ParsedRefMark> op = parseRefMarkName(name);
            if (op.isEmpty()) {
                // We have a problem. We want types[i] and bibtexKeys[i]
                // to correspond to referenceMarkNames.get(i).
                // And do not want null in bibtexKeys (or error code in types)
                // on return.
                throw new IllegalArgumentException(
                    "parseRefMarkNamesToArrays expects parsable referenceMarkNames");
            }
            ParsedRefMark ov = op.get();
            types[i] = ov.itcType;
            bibtexKeys[i] = ov.citationKeys.toArray(String[]::new);
        }
    }

    /**
     * Extract citation keys from names of referenceMarks in the document.
     *
     * Each citation key is listed only once, in the order of first appearance
     * (in `names`, which itself is in arbitrary order)
     *
     * doc.referenceMarks.names.map(parse).flatten.unique
     */
    private List<String> findCitedKeys(DocumentConnection documentConnection)
        throws
        NoSuchElementException,
        WrappedTargetException,
        NoDocumentException {

        List<String> names = getJabRefReferenceMarkNames(documentConnection);

        // assert it supports XTextContent
        XNameAccess xNamedMarks = documentConnection.getReferenceMarks();
        for (String name1 : names) {
            Object bookmark = xNamedMarks.getByName(name1);
            assert (null != DocumentConnection.asTextContent(bookmark));
        }

        // Collect to a flat list while keep only the first appearance.
        List<String> keys = new ArrayList<>();
        for (String name1 : names) {
            List<String> newKeys = parseRefMarkNameToUniqueCitationKeys(name1);
            for (String key : newKeys) {
                if (!keys.contains(key)) {
                    keys.add(key);
                }
            }
        }

        return keys;
    }

    /**
     *  Given the name of a reference mark, get the corresponding
     *  pageInfo text.
     *
     *  @param documentConnection Connection to a document.
     *  @param name Name of the custom property to query.
     *  @return "" for missing or empty pageInfo
     */
    private static String getPageInfoForReferenceMarkName(DocumentConnection documentConnection,
                                                          String name)
        throws
        WrappedTargetException,
        UnknownPropertyException {

        Optional<String> pageInfo = documentConnection.getCustomProperty(name);
        if (pageInfo.isEmpty() || pageInfo.get().isEmpty()) {
            return "";
        }
        return pageInfo.get();
    }

    /*
     * ranges controlled by citation groups should not overlap with each other.
     *
     * @param cgid : Must be known.
     * @return Null if the reference mark is missing.
     */
    public XTextRange getReferenceMarkRangeOrNull(DocumentConnection documentConnection,
                                                  CitationGroupID cgid)
        throws
        NoDocumentException,
        WrappedTargetException {
        String name = (this.getReferenceMarkName(cgid)
                       .orElseThrow(RuntimeException::new));
        return documentConnection.getReferenceMarkRangeOrNull(name);
    }

    /**
     * @return A RangeForOverlapCheck for each citation group.
     *
     *  result.size() == nRefMarks
     */
    List<RangeForOverlapCheck> citationRanges(DocumentConnection documentConnection)
        throws
        NoDocumentException,
        WrappedTargetException {

        List<RangeForOverlapCheck> xs = new ArrayList<>(numberOfCitationGroups());

        List<CitationGroupsV001.CitationGroupID> cgids =
            new ArrayList<>(this.getCitationGroupIDs());

        for (CitationGroupID cgid : cgids) {
            XTextRange r = this.getReferenceMarkRangeOrNull(documentConnection, cgid);
            String name = this.getCitationGroup(cgid).get().referenceMarkName;
            xs.add(new RangeForOverlapCheck(
                       r, cgid,
                       RangeForOverlapCheck.REFERENCE_MARK_KIND,
                       name));
        }
        return xs;
    }

    /**
     * @return A range for each footnote mark where the footnote
     *         contains at least one citation group.
     *
     *  Purpose: We do not want markers of footnotes containing
     *  reference marks to overlap with reference
     *  marks. Overwriting these footnote marks might kill our
     *  reference marks in the footnote.
     *
     */
    List<RangeForOverlapCheck> footnoteMarkRanges(DocumentConnection documentConnection)
        throws
        NoDocumentException,
        WrappedTargetException {

        // Avoid inserting the same mark twice.
        // Could use RangeSet if we had that.
        RangeKeyedMap<Boolean> seen = new RangeKeyedMap<>();

        List<RangeForOverlapCheck> xs = new ArrayList<>();

        List<RangeForOverlapCheck> citRanges = citationRanges(documentConnection);

        for (RangeForOverlapCheck base : citRanges) {
            XTextRange r = base.range;

            XTextRange footnoteMarkRange =
                DocumentConnection.getFootnoteMarkRangeOrNull(r);

            if (footnoteMarkRange == null) {
                // not in footnote
                continue;
            }

            boolean seenContains = seen.containsKey(footnoteMarkRange);
            if (!seenContains) {
                seen.put(footnoteMarkRange, true);
                xs.add(new RangeForOverlapCheck(
                           footnoteMarkRange,
                           base.i, // cgid :: identifies of citation group
                           RangeForOverlapCheck.FOOTNOTE_MARK_KIND,
                           "FootnoteMark for " + base.description));
            }
        }
        return xs;
    }

    /**
     * unoQI : short for UnoRuntime.queryInterface
     *
     * @return A reference to the requested UNO interface type if
     *         available, otherwise null.
     */
    private static <T> T unoQI(Class<T> zInterface,
                               Object object) {
        return UnoRuntime.queryInterface(zInterface, object);
    }

    static class CitationGroupID {
        String id;
        CitationGroupID(String id) {
            this.id = id;
        }

        /**
         *  CitationEntry needs refMark or other identifying string
         */
        String asString() {
            return id;
        }
    }

    static class Citation  implements SortableCitation {

        /** key in database */
        String citationKey;
        Optional<DatabaseLookupResult> db;
        Optional<Integer> number;
        Optional<String> uniqueLetter;

        /* missing: something that differentiates this from other
         * citations of the same citationKey
         */

        Citation(String citationKey) {
            this.citationKey = citationKey;
            this.db = Optional.empty();
            this.number = Optional.empty();
            this.uniqueLetter = Optional.empty();
        }

        @Override
        public String getCitationKey(){
            return citationKey;
        }

        @Override
        public Optional<BibEntry> getBibEntry(){
            return (db.isPresent()
                    ? Optional.of(db.get().entry)
                    : Optional.empty());
        }
    }

    static List<Integer> makeIndices(int n) {
        return Stream.iterate(0, i -> i + 1).limit(n).collect(Collectors.toList());
    }

    static class CitationGroup {
        CitationGroupID cgid;
        int itcType;
        List<Citation> citations;
        List<Integer> localOrder;
        // Currently pageInfo belongs to the group
        Optional<String> pageInfo;

        /**
         * Locator in document
         */
        String referenceMarkName;

        CitationGroup(
            CitationGroupID cgid,
            int itcType,
            List<Citation> citations,
            Optional<String> pageInfo,
            String referenceMarkName) {
            this.cgid = cgid;
            this.itcType = itcType;
            this.citations = citations;
            this.pageInfo = pageInfo;
            this.referenceMarkName = referenceMarkName;
            this.localOrder = makeIndices(citations.size());
        }

        List<Citation>
        getSortedCitations() {
            List<Citation> res = new ArrayList<>(citations.size());
            for (int i : localOrder) {
                res.add(citations.get(i));
            }
            return res;
        }

        List<Integer>
        getSortedNumbers() {
            List<Citation> cits = getSortedCitations();
            return (cits.stream()
                    .map(cit -> cit.number.orElseThrow(RuntimeException::new))
                    .collect(Collectors.toList()));
        }

        class CitationAndIndex implements SortableCitation {
            Citation c;
            int i;
            CitationAndIndex(Citation c, int i) {
                this.c = c;
                this.i = i;
            }

            @Override
            public String getCitationKey(){
                return c.getCitationKey();
            }

            @Override
            public Optional<BibEntry> getBibEntry(){
                return c.getBibEntry();
            }
        }

        /**
         * Sort citations for presentation within a CitationGroup.
         */
        /*
        class SortCitations implements Comparator<CitationAndIndex> {

            Comparator<BibEntry> entryComparator;
            boolean unresolvedComesFirst;

            SortCitations(Comparator<BibEntry> entryComparator,
                          boolean unresolvedComesFirst) {
                this.entryComparator = entryComparator;
                this.unresolvedComesFirst = unresolvedComesFirst;
            }

            public int compare(CitationAndIndex a, CitationAndIndex b) {
                if (a.c.db.isEmpty() && b.c.db.isEmpty()) {
                    // Both are unresolved: compare them by citation key.
                    String ack = a.c.citationKey;
                    String bck = b.c.citationKey;
                    return ack.compareTo(bck);
                }
                // Comparing unresolved and real entry
                final boolean unresolvedComesFirst = true;
                final int mul = unresolvedComesFirst ? (+1) : (-1);
                if (a.c.db.isEmpty()) {
                    return -mul;
                }
                if (b.c.db.isEmpty()) {
                    return mul;
                }
                // Proper comparison of entries
                return entryComparator.compare(a.c.db.get().entry,
                                               b.c.db.get().entry);
            }
        }
        */

        void imposeLocalOrderByComparator(Comparator<BibEntry> entryComparator) {
            List<CitationAndIndex> cks = new ArrayList<>();
            for (int i = 0; i < citations.size(); i++) {
                Citation c = citations.get(i);
                cks.add(new CitationAndIndex(c, i));
            }
            // Collections.sort(cks, new SortCitations(entryComparator, true));
            Collections.sort(cks, new CitationComparator(entryComparator, true));

            List<Integer> o = new ArrayList<>();
            for (CitationAndIndex ck : cks) {
                o.add(ck.i);
            }
            this.localOrder = o;
        }
    }

    public void show() {
        System.out.printf("CitationGroupsV001%n");
        System.out.printf("  citationGroups.size: %d%n", citationGroups.size());
        System.out.printf("  pageInfoThrash.size: %d%n", pageInfoThrash.size());
        System.out.printf("  globalOrder: %s%n",
                          (globalOrder.isEmpty()
                           ? "isEmpty"
                           : String.format("%d", globalOrder.get().size())));
    }

} // class citationGroups

