package org.jabref.gui.openoffice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyExistException;
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

class CitationGroups {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(CitationGroups.class);

    // public final Compat.DataModel dataModel;
    // private StorageBase.NamedRangeManager citationStorageManager;
    public final Backend52 backend;

    /**
     *  Original CitationGroups Data
     */
    private Map<CitationGroupID, CitationGroup> citationGroups;

    /**
     *  Extra Data
     */

    private Optional<List<CitationGroupID>> globalOrder;

    private Optional<CitedKeys> citedKeysAfterDatabaseLookup;

    /**
     *  This is going to be the bibliography
     */
    private Optional<CitedKeys> bibliography;

    /**
     * Constructor
     */
    public CitationGroups(DocumentConnection documentConnection)
        throws
        NoDocumentException,
        WrappedTargetException {

        // TODO: dataModel should come from looking at the
        // document and preferences.
        //
        this.backend = new Backend52();

        // Get the citationGroupNames
        List<String> citationGroupNames = this.backend.getJabRefReferenceMarkNames(documentConnection);


        this.citationGroups = readCitationGroupsFromDocument(this.backend,
                                                             documentConnection,
                                                             citationGroupNames);

        // Now we have almost every information from the document about citations.
        // What is left out: the ranges controlled by the reference marks.
        // But (I guess) those change too easily, so we only ask when actually needed.

        this.globalOrder = Optional.empty();
        this.citedKeysAfterDatabaseLookup = Optional.empty();
        this.bibliography = Optional.empty();
    }


    public String healthReport(DocumentConnection documentConnection)
        throws
        NoDocumentException {
        String r1 = backend.healthReport(documentConnection);
        // add more?
        return r1;
    }

    private static Map<CitationGroupID, CitationGroup>
    readCitationGroupsFromDocument(Backend52 backend,
                                   DocumentConnection documentConnection,
                                   List<String> citationGroupNames)
        throws
        WrappedTargetException,
        NoDocumentException {

        Map<CitationGroupID, CitationGroup> citationGroups = new HashMap<>();
        for (int i = 0; i < citationGroupNames.size(); i++) {
            final String name = citationGroupNames.get(i);
            CitationGroup cg =
                backend.readCitationGroupFromDocumentOrThrow(documentConnection, name);
            citationGroups.put(cg.cgid, cg);
        }
        return citationGroups;
    }

    public void setDatabaseLookupResults(Set<CitationPath> where,
                                         Optional<CitationDatabaseLookup.Result> db) {
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

    public CitedKeys lookupEntriesInDatabases(List<BibDatabase> databases) {
        CitationGroups cgs = this;

        CitedKeys cks = cgs.getCitedKeys();

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
        CitationGroups cgs = this;
        if (!cgs.bibliography.isEmpty()) {
            throw new RuntimeException(
                "createNumberedBibliographySortedInOrderOfAppearance: already have a bibliography");
        }
        CitedKeys sortedCitedKeys =
            cgs.getCitedKeysSortedInOrderOfAppearance();
        sortedCitedKeys.numberCitedKeysInCurrentOrder();
        sortedCitedKeys.distributeNumbers(cgs);
        cgs.bibliography = Optional.of(sortedCitedKeys);
    }

    public void createNumberedBibliographySortedByComparator(Comparator<BibEntry> entryComparator) {
        CitationGroups cgs = this;
        if (!cgs.bibliography.isEmpty()) {
            throw new RuntimeException(
                "createNumberedBibliographySortedByComparator: already have a bibliography");
        }
        CitedKeys citedKeys = cgs.getCitedKeys();
        citedKeys.sortByComparator(entryComparator); // TODO: must be after database lookup
        citedKeys.numberCitedKeysInCurrentOrder();
        citedKeys.distributeNumbers(cgs);
        this.bibliography = Optional.of(citedKeys);
    }

    public void createPlainBibliographySortedByComparator(Comparator<BibEntry> entryComparator) {
        CitationGroups cgs = this;
        if (!this.bibliography.isEmpty()) {
            throw new RuntimeException(
                "createPlainBibliographySortedByComparator: already have a bibliography");
        }
        CitedKeys citedKeys = cgs.getCitedKeys();
        citedKeys.sortByComparator(entryComparator); // TODO: must be after database lookup
        // citedKeys.numberCitedKeysInCurrentOrder();
        // citedKeys.distributeNumbers();
        this.bibliography = Optional.of(citedKeys);
    }

    public Set<CitationGroupID> getCitationGroupIDs() {
        return citationGroups.keySet();
    }

    /**
     * Creates a list of {@code
     * RangeSortable<CitationGroupID>} values for
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
    private static List<RangeSort.RangeSortable<CitationGroupID>>
    createVisualSortInput(CitationGroups cgs,
                          DocumentConnection documentConnection,
                          boolean mapFootnotesToFootnoteMarks)
        throws
        NoDocumentException,
        WrappedTargetException {

        List<CitationGroupID> cgids =
            new ArrayList<>(cgs.getCitationGroupIDs());

        List<RangeSort.RangeSortEntry> vses = new ArrayList<>();
        for (CitationGroupID cgid : cgids) {
            XTextRange range = cgs.getMarkRangeOrNull(documentConnection, cgid);
            if (range == null) {
                throw new RuntimeException("getMarkRangeOrNull returned null");
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
        RangeKeyedMapList<RangeSort.RangeSortEntry<CitationGroupID>> xxs
            = new RangeKeyedMapList<>();

        for (RangeSort.RangeSortEntry v : vses) {
            xxs.add(v.getRange(), v);
        }

        // build final list
        List<RangeSort.RangeSortEntry<CitationGroupID>> res = new ArrayList<>();

        for (TreeMap<XTextRange, List<RangeSort.RangeSortEntry<CitationGroupID>>>
                 xs : xxs.partitionValues()) {

            List<XTextRange> oxs = new ArrayList<>(xs.keySet());

            int indexInPartition = 0;
            for (int i = 0; i < oxs.size(); i++) {
                XTextRange a = oxs.get(i);
                List<RangeSort.RangeSortEntry<CitationGroupID>> avs = xs.get(a);
                for (int j = 0; j < avs.size(); j++) {
                    RangeSort.RangeSortEntry<CitationGroupID> v = avs.get(j);
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
        // List<RangeSortEntry<CitationGroupID>>
        // to
        // List<RangeSortable<CitationGroupID>>
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
    public List<CitationGroupID> getVisuallySortedCitationGroupIDs(DocumentConnection documentConnection,
                                                                   boolean mapFootnotesToFootnoteMarks)
        throws
        WrappedTargetException,
        NoDocumentException,
        JabRefException {
        CitationGroups cgs = this;
        List<RangeSort.RangeSortable<CitationGroupID>> vses =
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
        List<RangeSort.RangeSortable<CitationGroupID>> sorted =
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
        CitationGroups cgs = this;
        // boolean mapFootnotesToFootnoteMarks = false;
        List<RangeSort.RangeSortable<CitationGroupID>> vses =
            CitationGroups.createVisualSortInput(cgs,
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
    public List<CitationGroupID> getSortedCitationGroupIDs() {
        if (globalOrder.isEmpty()) {
            throw new RuntimeException("getSortedCitationGroupIDs: not ordered yet");
        }
        return globalOrder.get();
    }

    public void setGlobalOrder(List<CitationGroupID> globalOrder) {
        Objects.requireNonNull(globalOrder);
        if (globalOrder.size() != citationGroups.size()) {
            throw new RuntimeException(
                "setGlobalOrder: globalOrder.size() != citationGroups.size()");
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
            throw new RuntimeException("getCitationGroupOrThrow:"
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

    public Optional<List<Citation>> getCitations(CitationGroupID cgid) {
        return getCitationGroup(cgid).map(cg -> cg.citations);
    }

    public List<Citation> getSortedCitations(CitationGroupID cgid) {
        Optional<CitationGroup> cg = getCitationGroup(cgid);
        if (cg.isEmpty()) {
            throw new RuntimeException("getSortedCitations: invalid cgid");
        }
        return cg.get().getSortedCitations();
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
        WrappedTargetException,
        NotRemoveableException,
        PropertyExistException,
        IllegalTypeException {

        CitationGroup cg = backend.createCitationGroup(documentConnection,
                                                       citationKeys,
                                                       pageInfo,
                                                       itcType,
                                                       position,
                                                       insertSpaceAfter,
                                                       withoutBrackets);

        // add to our data
        this.citationGroups.put(cg.cgid, cg);
        // invalidate globalOrder.
        // TODO: look out for localOrder!
        this.globalOrder = Optional.empty();

        return cg.cgid;
    }

    public void removeCitationGroups(List<CitationGroup> cgs, DocumentConnection documentConnection)
        throws
        WrappedTargetException,
        NoDocumentException,
        NoSuchElementException,
        NotRemoveableException,
        PropertyExistException,
        IllegalTypeException {

        for (CitationGroup cg : cgs) {
            removeCitationGroup( cg, documentConnection );
        }
    }

    /**
     * Remove {@code cg} both from {@code this} and the document.
     *
     * Note: we invalidate the extra data we are storing
     *       (bibliography).
     *
     *       Update would be complicated, since we do not know how the
     *       bibliography was generated: it was partially done outside
     *       CitationGroups, and we did not store how.
     *
     *       So we stay with invalidating.
     *       Note: localOrder, numbering, uniqueLetters are not adjusted,
     *             it is easier to reread everything for a refresh.
     *
     */
    public void removeCitationGroup(CitationGroup cg,
                                    DocumentConnection documentConnection)
        throws
        WrappedTargetException,
        NoDocumentException,
        NoSuchElementException,
        NotRemoveableException,
        PropertyExistException,
        IllegalTypeException {

        // Apply
        backend.removeCitationGroup(cg, documentConnection);
        this.citationGroups.remove(cg.cgid);

        // Update what we can.
        this.globalOrder.map(l -> l.remove(cg.cgid));

        // Invalidate what we cannot update: CitedKeys
        this.citedKeysAfterDatabaseLookup = Optional.empty();
        this.bibliography = Optional.empty();
        // Could also: reset citation.number, citation.uniqueLetter.
    }

    /**
     * ranges controlled by citation groups should not overlap with each other.
     *
     * @param cgid : Must be known, throws if not.
     * @return Null if the reference mark is missing.
     *
     */
    public XTextRange getMarkRangeOrNull(DocumentConnection documentConnection,
                                         CitationGroupID cgid)
        throws
        NoDocumentException,
        WrappedTargetException {

        CitationGroup cg = this.getCitationGroup(cgid).orElseThrow(RuntimeException::new);
        return backend.getMarkRangeOrNull(cg, documentConnection);
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

        CitationGroup cg = this.getCitationGroup(cgid).orElseThrow(RuntimeException::new);
        return backend.getRawCursorForCitationGroup(cg, documentConnection);
    }

    public XTextCursor getFillCursorForCitationGroup(DocumentConnection documentConnection,
                                                     CitationGroupID cgid)
        throws
        NoDocumentException,
        WrappedTargetException,
        CreationException {

        CitationGroup cg = this.getCitationGroup(cgid).orElseThrow(RuntimeException::new);
        return backend.getFillCursorForCitationGroup(cg, documentConnection);
    }

    /**
     * Remove brackets, but if the result would become empty, leave
     * them; if the result would be a single characer, leave the left bracket.
     *
     * @param removeBracketsFromEmpty is intended to force removal if
     *        we are working on an "Empty citation" (INVISIBLE_CIT).
     */
    public void cleanFillCursorForCitationGroup(DocumentConnection documentConnection,
                                                CitationGroupID cgid)
        throws
        NoDocumentException,
        WrappedTargetException,
        CreationException {

        CitationGroup cg = this.getCitationGroup(cgid).orElseThrow(RuntimeException::new);
        backend.cleanFillCursorForCitationGroup(cg,documentConnection);
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

        List<CitationGroupID> cgids = new ArrayList<>(this.getCitationGroupIDs());

        for (CitationGroupID cgid : cgids) {
            XTextRange r = this.getMarkRangeOrNull(documentConnection, cgid);
            CitationGroup cg = this.getCitationGroup(cgid).orElseThrow(RuntimeException::new);
            String name = cg.cgRangeStorage.getName();
            xs.add(new RangeForOverlapCheck(r,
                                            cgid,
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
     *  Note: Here we directly communicate to the document, not
     *        through the backend. This is because mapping ranges to
     *        footnote marks does not depend on how do we mark or
     *        structure those ranges.
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
     *  This is for debugging, can be removed.
     */
    public void xshow() {
        System.out.printf("CitationGroups%n");
        System.out.printf("  citationGroups.size: %d%n", citationGroups.size());
        System.out.printf("  globalOrder: %s%n",
                          (globalOrder.isEmpty()
                           ? "isEmpty"
                           : String.format("%d", globalOrder.get().size())));
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

}
