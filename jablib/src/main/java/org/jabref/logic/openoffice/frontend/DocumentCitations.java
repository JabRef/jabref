package org.jabref.logic.openoffice.frontend;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.JabRefException;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.engine.BibliographySection;
import org.jabref.logic.openoffice.engine.storage.MarkStore;
import org.jabref.logic.openoffice.rangesort.FunctionalTextViewCursor;
import org.jabref.logic.openoffice.rangesort.RangeOverlap;
import org.jabref.logic.openoffice.rangesort.RangeOverlapBetween;
import org.jabref.logic.openoffice.rangesort.RangeOverlapWithin;
import org.jabref.logic.openoffice.rangesort.RangeSort;
import org.jabref.logic.openoffice.uno.CreationException;
import org.jabref.logic.openoffice.uno.NoDocumentException;
import org.jabref.logic.openoffice.uno.UnoRedlines;
import org.jabref.logic.openoffice.uno.UnoTextRange;
import org.jabref.model.openoffice.CitationEntry;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationGroups;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.util.OOResult;
import org.jabref.model.openoffice.util.OOVoidResult;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.XComponentContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

/// Action-local facade combining a mutable citation model with live Writer range and storage access.
///
/// [CitationGroups] is loaded at construction and kept in sync with group creation and removal through
/// this facade. [MarkStore] accesses the live document; this is neither a document subtype nor an
/// immutable snapshot. Document actions and renderer coordination belong to
/// [org.jabref.logic.openoffice.engine.CitationEngine].
@NullMarked
public class DocumentCitations {

    public final MarkStore markStore;
    public final CitationGroups citationGroups;

    public DocumentCitations(XTextDocument doc) throws NoDocumentException, WrappedTargetException {

        this.markStore = new MarkStore();
        this.citationGroups = markStore.readAll(doc);
    }

    /// Return citation groups in visual order within (but not across) XText partitions.
    ///
    /// This is (1) sufficient for combineCiteMarkers which looks for consecutive XTextRanges within each XText, (2) not confused by multicolumn layout or multipage display.
    public List<CitationGroup>
    getCitationGroupsSortedWithinPartitions(XTextDocument doc, boolean mapFootnotesToFootnoteMarks)
            throws NoDocumentException, WrappedTargetException {
        DocumentOrder order = new DocumentOrder();
        return mapFootnotesToFootnoteMarks
               ? order.inDocumentOrder(doc, citationGroups, markStore, Optional.empty())
               : order.withinTextContainers(doc, citationGroups, markStore);
    }

    /// Create a citation group for the given citation keys, at the end of position.
    ///
    /// On return `position` is collapsed, and is after the inserted space, or at the end of the reference mark.
    ///
    /// @param citationKeys      In storage order
    /// @param pageInfos         In storage order
    /// @param position          Collapsed to its end.
    /// @param insertSpaceBefore If true, we insert a space before the mark.
    /// @param insertSpaceAfter  If true, we insert a space after the mark, that carries on format of characters from the original position.
    public CitationGroup createCitationGroup(XTextDocument doc,
                                             XComponentContext context,
                                             List<String> citationKeys,
                                             @NonNull List<Optional<OOText>> pageInfos,
                                             CitationType citationType,
                                             XTextCursor position,
                                             boolean insertSpaceBefore,
                                             boolean insertSpaceAfter)
            throws
            CreationException,
            NoDocumentException,
            WrappedTargetException,
            NotRemoveableException,
            PropertyVetoException,
            IllegalTypeException {
        CitationGroup group = markStore.createCitationGroup(doc,
                context,
                citationKeys,
                pageInfos,
                citationType,
                position,
                insertSpaceBefore,
                insertSpaceAfter);

        this.citationGroups.afterCreateCitationGroup(group);
        return group;
    }

    /// Remove `group` both from the document and notify `citationGroups`
    public void removeCitationGroup(CitationGroup group, XTextDocument doc)
            throws WrappedTargetException, NoDocumentException, NotRemoveableException {

        markStore.removeCitationGroup(group, doc);
        this.citationGroups.afterRemoveCitationGroup(group);
    }

    public void removeCitationGroups(List<CitationGroup> citationGroups, XTextDocument doc)
            throws
            WrappedTargetException,
            NoDocumentException,
            NotRemoveableException {

        for (CitationGroup group : citationGroups) {
            removeCitationGroup(group, doc);
        }
    }

    /// ranges controlled by citation groups should not overlap with each other.
    ///
    /// @return Optional.empty() if the reference mark is missing.
    public Optional<XTextRange> getMarkRange(XTextDocument doc, CitationGroup group)
            throws NoDocumentException, WrappedTargetException {
        return markStore.getMarkRange(group, doc);
    }

    /// @return A RangeForOverlapCheck for each citation group. result.size() == nRefMarks
    public List<RangeForOverlapCheck<CitationGroupId>> getCitationRanges(XTextDocument doc)
            throws NoDocumentException, WrappedTargetException {

        List<RangeForOverlapCheck<CitationGroupId>> result =
                new ArrayList<>(citationGroups.numberOfCitationGroups());

        for (CitationGroup group : citationGroups.getCitationGroupsUnordered()) {
            Optional<XTextRange> range = this.getMarkRange(doc, group);
            if (range.isEmpty()) {
                continue;
            }
            XTextRange textRange = range.get();
            String description = textRange.getString();
            result.add(new RangeForOverlapCheck<>(textRange,
                    group.groupId,
                    RangeForOverlapCheck.REFERENCE_MARK_KIND,
                    description));
        }
        return result;
    }

    public List<RangeForOverlapCheck<CitationGroupId>> bibliographyRanges(XTextDocument doc)
            throws NoDocumentException, WrappedTargetException {

        List<RangeForOverlapCheck<CitationGroupId>> result = new ArrayList<>();

        Optional<XTextRange> range = BibliographySection.range(doc);
        if (range.isPresent()) {
            String description = "bibliography";
            result.add(new RangeForOverlapCheck<>(range.get(),
                    new CitationGroupId("bibliography"),
                    RangeForOverlapCheck.BIBLIOGRAPHY_MARK_KIND,
                    description));
        }
        return result;
    }

    /// @return A range for each footnote mark where the footnote contains at least one citation group. Purpose: We do not want markers of footnotes containing reference marks to overlap with reference marks. Overwriting these footnote marks might kill our reference marks in the footnote.
    ///
    /// Note: Here we directly communicate to the document, not through the markStore. This is because mapping ranges to footnote marks does not depend on how do we mark or structure those ranges.
    public List<RangeForOverlapCheck<CitationGroupId>>
    footnoteMarkRanges(List<RangeForOverlapCheck<CitationGroupId>> citationRanges) {
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
            RangeForOverlapCheck<CitationGroupId> citationRange = partition.getFirst();

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
            String listOfRanges = overlap.valuesForOverlappingRanges.stream()
                                                                    .map(v -> "'%s'".formatted(v.format()))
                                                                    .collect(Collectors.joining(", "));
            msg.append(
                    switch (overlap.kind) {
                        case EQUAL_RANGE ->
                                Localization.lang("Found identical ranges");
                        case OVERLAP ->
                                Localization.lang("Found overlapping ranges");
                        case TOUCH ->
                                Localization.lang("Found touching ranges");
                    });
            msg.append(": ");
            msg.append(listOfRanges);
            msg.append("\n");
        }
        return msg.toString();
    }

    /// Check for any overlap between userRanges and protected ranges.
    ///
    /// Assume userRanges is small (usually 1 elements for checking the cursor)
    ///
    /// Returns on first problem found.

    public OOVoidResult<JabRefException> checkRangeOverlapsWithCursor(XTextDocument doc,
                                                                      List<RangeForOverlapCheck<CitationGroupId>> userRanges,
                                                                      boolean requireSeparation) {
        try {
            List<RangeForOverlapCheck<CitationGroupId>> citationRanges = getCitationRanges(doc);

            List<RangeForOverlapCheck<CitationGroupId>> ranges = new ArrayList<>();

            // ranges.addAll(userRanges);
            ranges.addAll(bibliographyRanges(doc));
            ranges.addAll(citationRanges);
            ranges.addAll(footnoteMarkRanges(citationRanges));

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
        } catch (NoDocumentException | WrappedTargetException e) {
            return OOVoidResult.error(new JabRefException(e.getMessage(), e));
        }
    }

    public OOVoidResult<JabRefException> checkCitationEdits(XTextDocument doc) {
        OOVoidResult<JabRefException> overlaps = checkRangeOverlaps(doc, List.of(), false, 1);
        if (overlaps.isError()) {
            return overlaps;
        }
        try {
            List<XTextRange> ranges = getCitationRanges(doc).stream().map(RangeForOverlapCheck::getRange).toList();
            if (UnoRedlines.countDeletedRangesTouching(doc, ranges) > 0) {
                return OOVoidResult.error(new JabRefException(Localization.lang("Recording and/or Recorded changes"),
                        Localization.lang("Citations inside deletions that have not been accepted yet may reappear.") + "\n"
                                + Localization.lang("Use [Edit]/[Track Changes]/[Manage] to resolve them first.")));
            }
            return OOVoidResult.ok();
        } catch (NoDocumentException | WrappedTargetException exception) {
            return OOVoidResult.error(new JabRefException(exception.toString(), exception));
        }
    }

    /// @param requireSeparation Report range pairs that only share a boundary.
    /// @param reportAtMost      Limit number of overlaps reported (0 for no limit)
    public OOVoidResult<JabRefException> checkRangeOverlaps(XTextDocument doc,
                                                            List<RangeForOverlapCheck<CitationGroupId>> userRanges,
                                                            boolean requireSeparation,
                                                            int reportAtMost) {
        try {
            List<RangeForOverlapCheck<CitationGroupId>> citationRanges = getCitationRanges(doc);
            List<RangeForOverlapCheck<CitationGroupId>> ranges = new ArrayList<>();
            ranges.addAll(userRanges);
            ranges.addAll(bibliographyRanges(doc));
            ranges.addAll(citationRanges);
            ranges.addAll(footnoteMarkRanges(citationRanges));

            List<RangeOverlap<RangeForOverlapCheck<CitationGroupId>>> overlaps =
                    RangeOverlapWithin.findOverlappingRanges(doc, ranges, requireSeparation, reportAtMost);

            if (overlaps.isEmpty()) {
                return OOVoidResult.ok();
            }
            return OOVoidResult.error(new JabRefException("Found overlapping or touching ranges",
                    rangeOverlapsToMessage(overlaps)));
        } catch (NoDocumentException | WrappedTargetException e) {
            return OOVoidResult.error(new JabRefException(e.getMessage(), e));
        }
    }

    /// GUI: Get a list of CitationEntry objects corresponding to citations in the document.
    ///
    /// Called from: ManageCitationsDialogViewModel constructor.
    ///
    /// @return A list with entries corresponding to citations in the text, in arbitrary order (same order as from getJabRefReferenceMarkNames). Note: visual or alphabetic order could be more manageable for the user. We could provide these here, but switching between them needs change on GUI (adding a toggle or selector).
    ///
    /// Note: CitationEntry implements Comparable, where compareTo() and equals() are based on refMarkName.  The order used in the "Manage citations" dialog does not seem to use that.
    ///
    /// The 1st is labeled "Citation" (show citation in bold, and some context around it).
    ///
    /// The columns can be sorted by clicking on the column title.  For the "Citation" column, the sorting is based on the content, (the context before the citation), not on the citation itself.
    ///
    /// In the "Extra information ..." column some visual indication of the editable part could be helpful.
    ///
    /// Wish: selecting an entry (or a button in the line) in the GUI could move the cursor in the document to the entry.
    public List<CitationEntry> getCitationEntries(XTextDocument doc)
            throws WrappedTargetException, NoDocumentException {
        return this.markStore.getCitationEntries(doc, citationGroups);
    }

    public void applyCitationEntries(XTextDocument doc, List<CitationEntry> citationEntries)
            throws
            PropertyVetoException,
            IllegalTypeException,
            NotRemoveableException,
            IllegalArgumentException,
            WrappedTargetException {
        this.markStore.applyCitationEntries(doc, citationEntries);
    }

    public void imposeGlobalOrder(XTextDocument doc) throws WrappedTargetException, NoDocumentException {
        OOResult<FunctionalTextViewCursor, String> cursor = FunctionalTextViewCursor.get(doc);
        Optional<FunctionalTextViewCursor> available = cursor.isError() ? Optional.empty() : Optional.of(cursor.get());
        citationGroups.setGlobalOrder(new ArrayList<>(new DocumentOrder().inDocumentOrder(doc, citationGroups, markStore, available).stream().map(group -> group.groupId).toList()));
    }
}
