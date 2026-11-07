package org.jabref.logic.openoffice.frontend;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.openoffice.engine.storage.MarkStore;
import org.jabref.logic.openoffice.rangesort.FunctionalTextViewCursor;
import org.jabref.logic.openoffice.rangesort.RangeSort;
import org.jabref.logic.openoffice.rangesort.RangeSortEntry;
import org.jabref.logic.openoffice.rangesort.RangeSortVisual;
import org.jabref.logic.openoffice.rangesort.RangeSortable;
import org.jabref.logic.openoffice.uno.NoDocumentException;
import org.jabref.logic.openoffice.uno.UnoScreenRefresh;
import org.jabref.logic.openoffice.uno.UnoTextRange;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroups;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XText;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Orders within each text container before mapping footnotes to body anchors, preserving ties
/// between several citations in one footnote. Group editing deliberately uses the unmapped order.
@NullMarked
public final class DocumentOrder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentOrder.class);

    public List<CitationGroup> withinTextContainers(XTextDocument document, CitationGroups groups, MarkStore store)
            throws NoDocumentException, WrappedTargetException {
        return inputs(document, groups, store, false).stream().map(RangeSortable::getContent).toList();
    }

    public List<CitationGroup> inDocumentOrder(XTextDocument document, CitationGroups groups, MarkStore store,
                                               Optional<FunctionalTextViewCursor> viewCursor)
            throws NoDocumentException, WrappedTargetException {
        return sort(document, inputs(document, groups, store, true), viewCursor);
    }

    public List<CitationGroup> withInsertion(XTextDocument document, CitationGroups groups, MarkStore store,
                                             CitationGroup pending, XTextRange position,
                                             Optional<FunctionalTextViewCursor> viewCursor)
            throws NoDocumentException, WrappedTargetException {
        List<RangeSortable<CitationGroup>> all = new ArrayList<>(inputs(document, groups, store, false));
        all.add(new RangeSortEntry<>(position, 0, pending));
        RangeSort.RangePartitions<RangeSortable<CitationGroup>> partitions = RangeSort.partitionAndSortRanges(all);
        List<RangeSortable<CitationGroup>> mapped = new ArrayList<>();
        for (List<RangeSortable<CitationGroup>> partition : partitions.getPartitions()) {
            for (int index = 0; index < partition.size(); index++) {
                RangeSortable<CitationGroup> entry = partition.get(index);
                RangeSortEntry<CitationGroup> sortable = new RangeSortEntry<>(entry.getRange(), index, entry.getContent());
                UnoTextRange.getFootnoteMarkRange(sortable.getRange()).ifPresent(sortable::setRange);
                mapped.add(sortable);
            }
        }
        return sort(document, mapped, viewCursor);
    }

    private List<CitationGroup> sort(XTextDocument document, List<RangeSortable<CitationGroup>> inputs,
                                     Optional<FunctionalTextViewCursor> viewCursor) {
        if (viewCursor.isPresent() && !UnoScreenRefresh.hasControllersLocked(document)) {
            FunctionalTextViewCursor cursor = viewCursor.orElseThrow();
            try {
                return RangeSortVisual.visualSort(inputs, document, cursor).stream().map(RangeSortable::getContent).toList();
            } catch (com.sun.star.uno.RuntimeException | IllegalStateException exception) {
                LOGGER.warn("Visual citation ordering failed; using textual order within text containers", exception);
            } finally {
                cursor.restore(document);
            }
        } else {
            LOGGER.warn("No usable unlocked view cursor; using textual citation order within text containers");
        }
        Map<XText, List<RangeSortable<CitationGroup>>> partitions = new LinkedHashMap<>();
        for (RangeSortable<CitationGroup> input : inputs) {
            partitions.computeIfAbsent(input.getRange().getText(), _ -> new ArrayList<>()).add(input);
        }
        for (List<RangeSortable<CitationGroup>> partition : partitions.values()) {
            partition.sort((left, right) -> {
                int order = UnoTextRange.compareStarts(left.getRange(), right.getRange());
                return order == 0 ? Integer.compare(left.getIndexInPosition(), right.getIndexInPosition()) : order;
            });
        }
        return partitions.values().stream().flatMap(List::stream).map(RangeSortable::getContent).toList();
    }

    private static List<RangeSortable<CitationGroup>> inputs(XTextDocument document, CitationGroups groups, MarkStore store,
                                                             boolean mapFootnotes)
            throws NoDocumentException, WrappedTargetException {
        Map<XText, List<RangeSortEntry<CitationGroup>>> partitions = new LinkedHashMap<>();
        for (CitationGroup group : groups.getCitationGroupsUnordered()) {
            XTextRange range = store.getMarkRange(group, document)
                                    .orElseThrow(() -> new NoDocumentException("Missing citation range during document ordering"));
            partitions.computeIfAbsent(range.getText(), _ -> new ArrayList<>()).add(new RangeSortEntry<>(range, 0, group));
        }
        List<RangeSortable<CitationGroup>> result = new ArrayList<>();
        for (List<RangeSortEntry<CitationGroup>> partition : partitions.values()) {
            RangeSort.sortWithinPartition(partition);
            for (int index = 0; index < partition.size(); index++) {
                RangeSortEntry<CitationGroup> entry = partition.get(index);
                entry.setIndexInPosition(index);
                if (mapFootnotes) {
                    UnoTextRange.getFootnoteMarkRange(entry.getRange()).ifPresent(entry::setRange);
                }
                result.add(entry);
            }
        }
        return result;
    }
}
