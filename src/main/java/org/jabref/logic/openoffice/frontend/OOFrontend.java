package org.jabref.logic.openoffice.frontend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.JabRefException;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.backend.Backend52;
import org.jabref.model.openoffice.CitationEntry;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.rangesort.FunctionalTextViewCursor;
import org.jabref.model.openoffice.rangesort.RangeOverlap;
import org.jabref.model.openoffice.rangesort.RangeOverlapBetween;
import org.jabref.model.openoffice.rangesort.RangeOverlapWithin;
import org.jabref.model.openoffice.rangesort.RangeSort;
import org.jabref.model.openoffice.rangesort.RangeSortEntry;
import org.jabref.model.openoffice.rangesort.RangeSortVisual;
import org.jabref.model.openoffice.rangesort.RangeSortable;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationGroups;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.style.OODataModel;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.uno.UnoCursor;
import org.jabref.model.openoffice.uno.UnoTextRange;
import org.jabref.model.openoffice.util.OOListUtil;
import org.jabref.model.openoffice.util.OOVoidResult;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

public class OOFrontend {

    public final Backend52 backend;
    public final CitationGroups citationGroups;

    public OOFrontend(XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException {

        // TODO: dataModel should come from looking at the document and preferences.
        this.backend = new Backend52();

        // Get the citationGroupNames
        List<String> citationGroupNames = this.backend.getJabRefReferenceMarkNames(doc);

        Map<CitationGroupId, CitationGroup> citationGroups =
            readCitationGroupsFromDocument(this.backend, doc, citationGroupNames);
        this.citationGroups = new CitationGroups(citationGroups);
    }

    public OODataModel getDataModel() {
        return backend.dataModel;
    }

    public Optional<String> healthReport(XTextDocument doc)
        throws
        NoDocumentException {
        return backend.healthReport(doc);
    }

    private static Map<CitationGroupId, CitationGroup>
    readCitationGroupsFromDocument(Backend52 backend,
                                   XTextDocument doc,
                                   List<String> citationGroupNames)
        throws
        WrappedTargetException,
        NoDocumentException {

        Map<CitationGroupId, CitationGroup> citationGroups = new HashMap<>();
        for (String name : citationGroupNames) {
            CitationGroup group = backend.readCitationGroupFromDocumentOrThrow(doc, name);
            citationGroups.put(group.groupId, group);
        }
        return citationGroups;
    }

    /**
     * Creates a list of {@code RangeSortable<CitationGroup>} values for our {@code CitationGroup}
     * values. Originally designed to be passed to {@code visualSort}.
     *
     * The elements of the returned list are actually of type {@code RangeSortEntry<CitationGroup>}.
     *
     * The result is sorted within {@code XTextRange.getText()} partitions of the citation groups
     * according to their {@code XTextRange} (before mapping to footnote marks).
     *
     * In the result, RangeSortable.getIndexInPosition() contains unique indexes within the original
     * partition (not after mapFootnotesToFootnoteMarks).
     *
     * @param mapFootnotesToFootnoteMarks If true, replace ranges in footnotes with the range of the
     *        corresponding footnote mark. This is used for numbering the citations.
     *
     */
    private List<RangeSortable<CitationGroup>>
    createVisualSortInput(XTextDocument doc, boolean mapFootnotesToFootnoteMarks)
        throws
        NoDocumentException,
        WrappedTargetException {

        List<RangeSortEntry<CitationGroup>> sortables = new ArrayList<>();
        for (CitationGroup group : citationGroups.getCitationGroupsUnordered()) {
            XTextRange range = (this
                                .getMarkRange(doc, group)
                                .orElseThrow(IllegalStateException::new));
            sortables.add(new RangeSortEntry<>(range, 0, group));
        }

        /*
         *  At this point we are almost ready to return sortables.
         *
         *  But we may want to number citations in a footnote as if it appeared where the footnote
         *  mark is.
         *
         *  The following code replaces ranges within footnotes with the range for the corresponding
         *  footnote mark.
         *
         *  This brings further ambiguity if we have multiple citation groups within the same
         *  footnote: for the comparison they become indistinguishable. Numbering between them is
         *  not controlled. Also combineCiteMarkers will see them in the wrong order (if we use this
         *  comparison), and will not be able to merge. To avoid these, we sort textually within
         *  each .getText() partition and add indexInPosition accordingly.
         *
         */

        // Sort within partitions
        RangeSort.RangePartitions<RangeSortEntry<CitationGroup>> partitions =
            RangeSort.partitionAndSortRanges(sortables);

        // build final list
        List<RangeSortEntry<CitationGroup>> result = new ArrayList<>();
        for (List<RangeSortEntry<CitationGroup>> partition : partitions.getPartitions()) {

            int indexInPartition = 0;
            for (RangeSortEntry<CitationGroup> sortable : partition) {
                sortable.setIndexInPosition(indexInPartition++);
                if (mapFootnotesToFootnoteMarks) {
                    Optional<XTextRange> footnoteMarkRange =
                        UnoTextRange.getFootnoteMarkRange(sortable.getRange());
                    // Adjust range if we are inside a footnote:
                    if (footnoteMarkRange.isPresent()) {
                        sortable.setRange(footnoteMarkRange.get());
                    }
                }
                result.add(sortable);
            }
        }
        return result.stream().map(e -> e).collect(Collectors.toList());
    }

    /**
     *  @param mapFootnotesToFootnoteMarks If true, sort reference marks in footnotes as if they
     *         appeared at the corresponding footnote mark.
     *
     *  @return citation groups sorted by their visual positions.
     *
     *  Limitation: for two column layout visual (top-down, left-right) order does not match the
     *        expected (textual) order.
     *
     */
    private List<CitationGroup> getVisuallySortedCitationGroups(XTextDocument doc,
                                                                boolean mapFootnotesToFootnoteMarks,
                                                                FunctionalTextViewCursor fcursor)
        throws
        WrappedTargetException,
        NoDocumentException {

        List<RangeSortable<CitationGroup>> sortables = createVisualSortInput(doc, mapFootnotesToFootnoteMarks);

        List<RangeSortable<CitationGroup>> sorted = RangeSortVisual.visualSort(sortables, doc, fcursor);

        return (sorted.stream()
                .map(RangeSortable<CitationGroup>::getContent)
                .collect(Collectors.toList()));
    }

    /**
     * Return citation groups in visual order within (but not across) XText partitions.
     *
     * This is (1) sufficient for combineCiteMarkers which looks for consecutive XTextRanges within
     * each XText, (2) not confused by multicolumn layout or multipage display.
     */
    public List<CitationGroup>
    getCitationGroupsSortedWithinPartitions(XTextDocument doc, boolean mapFootnotesToFootnoteMarks)
        throws
        NoDocumentException,
        WrappedTargetException {
        // This is like getVisuallySortedCitationGroups,
        // but we skip the visualSort part.
        List<RangeSortable<CitationGroup>> sortables =
            createVisualSortInput(doc, mapFootnotesToFootnoteMarks);

        return (sortables.stream().map(e -> e.getContent()).collect(Collectors.toList()));
    }

    /**
     *  Create a citation group for the given citation keys, at the end of position.
     *
     *  On return {@code position} is collapsed, and is after the inserted space, or at the end of
     *  the reference mark.
     *
     * @param citationKeys In storage order
     * @param pageInfos In storage order
     * @param citationType
     * @param position Collapsed to its end.
     * @param insertSpaceAfter If true, we insert a space after the mark, that carries on format of
     *                         characters from the original position.
     */
    public CitationGroup createCitationGroup(XTextDocument doc,
                                             List<String> citationKeys,
                                             List<Optional<OOText>> pageInfos,
                                             CitationType citationType,
                                             XTextCursor position,
                                             boolean insertSpaceAfter)
        throws
        CreationException,
        NoDocumentException,
        WrappedTargetException,
        NotRemoveableException,
        PropertyVetoException,
        IllegalTypeException {

        Objects.requireNonNull(pageInfos);
        if (pageInfos.size() != citationKeys.size()) {
            throw new IllegalArgumentException("pageInfos.size != citationKeys.size");
        }
        CitationGroup group = backend.createCitationGroup(doc,
                                                          citationKeys,
                                                          pageInfos,
                                                          citationType,
                                                          position,
                                                          insertSpaceAfter);

        this.citationGroups.afterCreateCitationGroup(group);
        return group;
    }

    /**
     * Remove {@code group} both from the document and notify {@code citationGroups}
     */
    public void removeCitationGroup(CitationGroup group, XTextDocument doc)
        throws
        WrappedTargetException,
        NoDocumentException,
        NotRemoveableException {

        backend.removeCitationGroup(group, doc);
        this.citationGroups.afterRemoveCitationGroup(group);
    }

    public void removeCitationGroups(List<CitationGroup> cgs, XTextDocument doc)
        throws
        WrappedTargetException,
        NoDocumentException,
        NotRemoveableException {

        for (CitationGroup group : cgs) {
            removeCitationGroup(group, doc);
        }
    }

    /**
     * ranges controlled by citation groups should not overlap with each other.
     *
     * @return Optional.empty() if the reference mark is missing.
     *
     */
    public Optional<XTextRange> getMarkRange(XTextDocument doc, CitationGroup group)
        throws
        NoDocumentException,
        WrappedTargetException {
        return backend.getMarkRange(group, doc);
    }

    public XTextCursor getFillCursorForCitationGroup(XTextDocument doc, CitationGroup group)
        throws
        NoDocumentException,
        WrappedTargetException,
        CreationException {
        return backend.getFillCursorForCitationGroup(group, doc);
    }

    /**
     * Remove brackets added by getFillCursorForCitationGroup.
     */
    public void cleanFillCursorForCitationGroup(XTextDocument doc, CitationGroup group)
        throws
        NoDocumentException,
        WrappedTargetException {

        backend.cleanFillCursorForCitationGroup(group, doc);
    }

    /**
     * @return A RangeForOverlapCheck for each citation group.
     *
     *  result.size() == nRefMarks
     */
    public List<RangeForOverlapCheck<CitationGroupId>> citationRanges(XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException {

        List<RangeForOverlapCheck<CitationGroupId>> result =
            new ArrayList<>(citationGroups.numberOfCitationGroups());

        for (CitationGroup group : citationGroups.getCitationGroupsUnordered()) {
            XTextRange range = this.getMarkRange(doc, group).orElseThrow(IllegalStateException::new);
            String description = group.groupId.citationGroupIdAsString();
            result.add(new RangeForOverlapCheck<>(range,
                                                  group.groupId,
                                                  RangeForOverlapCheck.REFERENCE_MARK_KIND,
                                                  description));
        }
        return result;
    }

    public List<RangeForOverlapCheck<CitationGroupId>> bibliographyRanges(XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException {

        List<RangeForOverlapCheck<CitationGroupId>> result = new ArrayList<>();

        Optional<XTextRange> range = UpdateBibliography.getBibliographyRange(doc);
        if (range.isPresent()) {
            String description = "bibliography";
            result.add(new RangeForOverlapCheck<>(range.get(),
                                                  new CitationGroupId("bibliography"),
                                                  RangeForOverlapCheck.BIBLIOGRAPHY_MARK_KIND,
                                                  description));
        }
        return result;
    }

    public List<RangeForOverlapCheck<CitationGroupId>> viewCursorRanges(XTextDocument doc) {

        List<RangeForOverlapCheck<CitationGroupId>> result = new ArrayList<>();

        Optional<XTextRange> range = UnoCursor.getViewCursor(doc).map(e -> e);
        if (range.isPresent()) {
            String description = "cursor";
            result.add(new RangeForOverlapCheck<>(range.get(),
                                                  new CitationGroupId("cursor"),
                                                  RangeForOverlapCheck.CURSOR_MARK_KIND,
                                                  description));
        }
        return result;
    }

    /**
     * @return A range for each footnote mark where the footnote contains at least one citation group.
     *
     *  Purpose: We do not want markers of footnotes containing reference marks to overlap with
     *  reference marks. Overwriting these footnote marks might kill our reference marks in the
     *  footnote.
     *
     *  Note: Here we directly communicate to the document, not through the backend. This is because
     *        mapping ranges to footnote marks does not depend on how do we mark or structure those
     *        ranges.
     */
    public List<RangeForOverlapCheck<CitationGroupId>>
    footnoteMarkRanges(XTextDocument doc, List<RangeForOverlapCheck<CitationGroupId>> citationRanges) {

        // We partition by XText and use a single range from
        // each partition to get at the corresponding footnotemark range.

        List<RangeForOverlapCheck<CitationGroupId>> result = new ArrayList<>();
        RangeSort.RangePartitions<RangeForOverlapCheck<CitationGroupId>> partitions =
            RangeSort.partitionRanges(citationRanges);

        // Each partition corresponds to an XText, and each footnote has a single XText.
        // (This latter ignores the possibility of XTextContents inserted into footnotes.)
        // Also: different footnotes cannot share a footnotemark range, we are not creating duplicates.
        for (List<RangeForOverlapCheck<CitationGroupId>> partition : partitions.getPartitions()) {
            if (partition.isEmpty()) {
                continue;
            }
            RangeForOverlapCheck<CitationGroupId> citationRange = partition.get(0);

            Optional<XTextRange> footnoteMarkRange = UnoTextRange.getFootnoteMarkRange(citationRange.range);

            if (footnoteMarkRange.isEmpty()) {
                // not in footnote
                continue;
            }

            result.add(new RangeForOverlapCheck<>(footnoteMarkRange.get(),
                                                  citationRange.idWithinKind,
                                                  RangeForOverlapCheck.FOOTNOTE_MARK_KIND,
                                                  "FootnoteMark for " + citationRange.format()));
        }
        return result;
    }

    static String rangeOverlapsToMessage(List<RangeOverlap<RangeForOverlapCheck<CitationGroupId>>> overlaps) {

        if (overlaps.isEmpty()) {
            return "(*no overlaps*)";
        }

        StringBuilder msg = new StringBuilder();
        for (RangeOverlap<RangeForOverlapCheck<CitationGroupId>> overlap : overlaps) {
            String listOfRanges = (overlap.valuesForOverlappingRanges.stream()
                                   .map(v -> String.format("'%s'", v.format()))
                                   .collect(Collectors.joining(", ")));
            msg.append(
                switch (overlap.kind) {
                    case EQUAL_RANGE -> Localization.lang("Found identical ranges");
                    case OVERLAP -> Localization.lang("Found overlapping ranges");
                    case TOUCH -> Localization.lang("Found touching ranges");
                });
            msg.append(": ");
            msg.append(listOfRanges);
            msg.append("\n");
        }
        return msg.toString();
    }

    /**
     * Check for any overlap between userRanges and protected ranges.
     *
     * Assume userRanges is small (usually 1 elements for checking the cursor)
     *
     * Returns on first problem found.
     */
    public OOVoidResult<JabRefException>
    checkRangeOverlapsWithCursor(XTextDocument doc,
                                 List<RangeForOverlapCheck<CitationGroupId>> userRanges,
                                 boolean requireSeparation)
        throws
        NoDocumentException,
        WrappedTargetException {

        List<RangeForOverlapCheck<CitationGroupId>> citationRanges = citationRanges(doc);
        List<RangeForOverlapCheck<CitationGroupId>> ranges = new ArrayList<>();

        // ranges.addAll(userRanges);
        ranges.addAll(bibliographyRanges(doc));
        ranges.addAll(citationRanges);
        ranges.addAll(footnoteMarkRanges(doc, citationRanges));

        List<RangeOverlap<RangeForOverlapCheck<CitationGroupId>>> overlaps =
            RangeOverlapBetween.findFirst(doc,
                                          userRanges,
                                          ranges,
                                          requireSeparation);

        if (overlaps.isEmpty()) {
            return OOVoidResult.ok();
        }
        return OOVoidResult.error(new JabRefException("Found overlapping or touching ranges",
                                                      rangeOverlapsToMessage(overlaps)));
    }

    /**
     * @param requireSeparation Report range pairs that only share a boundary.
     * @param reportAtMost Limit number of overlaps reported (0 for no limit)
     *
     */
    public OOVoidResult<JabRefException> checkRangeOverlaps(XTextDocument doc,
                                                            List<RangeForOverlapCheck<CitationGroupId>> userRanges,
                                                            boolean requireSeparation,
                                                            int reportAtMost)
        throws
        NoDocumentException,
        WrappedTargetException {

        List<RangeForOverlapCheck<CitationGroupId>> citationRanges = citationRanges(doc);
        List<RangeForOverlapCheck<CitationGroupId>> ranges = new ArrayList<>();
        ranges.addAll(userRanges);
        ranges.addAll(bibliographyRanges(doc));
        ranges.addAll(citationRanges);
        ranges.addAll(footnoteMarkRanges(doc, citationRanges));

        List<RangeOverlap<RangeForOverlapCheck<CitationGroupId>>> overlaps =
            RangeOverlapWithin.findOverlappingRanges(doc, ranges, requireSeparation, reportAtMost);

        if (overlaps.isEmpty()) {
            return OOVoidResult.ok();
        }
        return OOVoidResult.error(new JabRefException("Found overlapping or touching ranges",
                                                      rangeOverlapsToMessage(overlaps)));
    }

    /**
     * GUI: Get a list of CitationEntry objects corresponding to citations
     * in the document.
     *
     * Called from: ManageCitationsDialogViewModel constructor.
     *
     * @return A list with entries corresponding to citations in the text, in arbitrary order (same
     *         order as from getJabRefReferenceMarkNames).
     *
     *               Note: visual or alphabetic order could be more manageable for the user. We
     *               could provide these here, but switching between them needs change on GUI
     *               (adding a toggle or selector).
     *
     *               Note: CitationEntry implements Comparable, where compareTo() and equals() are
     *                     based on refMarkName.  The order used in the "Manage citations" dialog
     *                     does not seem to use that.
     *
     *                     The 1st is labeled "Citation" (show citation in bold, and some context
     *                     around it).
     *
     *                     The columns can be sorted by clicking on the column title.  For the
     *                     "Citation" column, the sorting is based on the content, (the context
     *                     before the citation), not on the citation itself.
     *
     *                     In the "Extra information ..." column some visual indication of the
     *                     editable part could be helpful.
     *
     *         Wish: selecting an entry (or a button in the line) in the GUI could move the cursor
     *               in the document to the entry.
     */
    public List<CitationEntry> getCitationEntries(XTextDocument doc)
        throws
        WrappedTargetException,
        NoDocumentException {
        return this.backend.getCitationEntries(doc, citationGroups);
    }

    public void applyCitationEntries(XTextDocument doc, List<CitationEntry> citationEntries)
        throws
        PropertyVetoException,
        IllegalTypeException,
        IllegalArgumentException,
        WrappedTargetException {
        this.backend.applyCitationEntries(doc, citationEntries);
    }

    public void imposeGlobalOrder(XTextDocument doc, FunctionalTextViewCursor fcursor)
        throws
        WrappedTargetException,
        NoDocumentException {

        boolean mapFootnotesToFootnoteMarks = true;
        List<CitationGroup> sortedCitationGroups =
            getVisuallySortedCitationGroups(doc, mapFootnotesToFootnoteMarks, fcursor);
        List<CitationGroupId> sortedCitationGroupIds = OOListUtil.map(sortedCitationGroups, group -> group.groupId);
        citationGroups.setGlobalOrder(sortedCitationGroupIds);
    }
}
