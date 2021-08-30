package org.jabref.model.openoffice.style;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.util.OOListUtil;

/**
 * A CitationGroup describes a group of citations.
 */
public class CitationGroup {

    public final OODataModel dataModel;

    /*
     * Identifies this citation group.
     */
    public final CitationGroupId groupId;

    /*
     * The core data, stored in the document:
     * The type of citation and citations in storage order.
     */
    public final CitationType citationType;
    public final List<Citation> citationsInStorageOrder;

    /*
     * Extra data
     */

    /*
     * A name of a reference mark to link to by formatCitedOnPages.
     * May be initially empty, if backend does not use reference marks.
     *
     * produceCitationMarkers might want fill it to support cross-references to citation groups from
     * the bibliography.
     */
    private Optional<String> referenceMarkNameForLinking;

    /*
     * Indices into citations: citations[localOrder[i]] provides ith citation according to the
     * currently imposed local order for presentation.
     *
     * Initialized to (0..(nCitations-1)) in the constructor.
     */
    private List<Integer> localOrder;

    /*
     * "Cited on pages" uses this to sort the cross-references.
     */
    private Optional<Integer> indexInGlobalOrder;

    /*
     * Citation marker.
     */
    private Optional<OOText> citationMarker;

    public CitationGroup(OODataModel dataModel,
                         CitationGroupId groupId,
                         CitationType citationType,
                         List<Citation> citationsInStorageOrder,
                         Optional<String> referenceMarkNameForLinking) {
        this.dataModel = dataModel;
        this.groupId = groupId;
        this.citationType = citationType;
        this.citationsInStorageOrder = Collections.unmodifiableList(citationsInStorageOrder);
        this.localOrder = OOListUtil.makeIndices(citationsInStorageOrder.size());
        this.referenceMarkNameForLinking = referenceMarkNameForLinking;
        this.indexInGlobalOrder = Optional.empty();
        this.citationMarker = Optional.empty();
    }

    public int numberOfCitations() {
        return citationsInStorageOrder.size();
    }

    /*
     * localOrder
     */

    /**
     * Sort citations for presentation within a CitationGroup.
     */
    void imposeLocalOrder(Comparator<BibEntry> entryComparator) {

        // For JabRef52 the single pageInfo is always in the last-in-localorder citation.
        // We adjust here accordingly by taking it out and adding it back after sorting.
        final int last = this.numberOfCitations() - 1;
        Optional<OOText> lastPageInfo = Optional.empty();
        if (dataModel == OODataModel.JabRef52) {
            Citation lastCitation = getCitationsInLocalOrder().get(last);
            lastPageInfo = lastCitation.getPageInfo();
            lastCitation.setPageInfo(Optional.empty());
        }

        this.localOrder = OOListUtil.order(citationsInStorageOrder,
                                           new CompareCitation(entryComparator, true));

        if (dataModel == OODataModel.JabRef52) {
            getCitationsInLocalOrder().get(last).setPageInfo(lastPageInfo);
        }
    }

    public List<Integer> getLocalOrder() {
        return Collections.unmodifiableList(localOrder);
    }

    /*
     * citations
     */

    public List<Citation> getCitationsInLocalOrder() {
        return OOListUtil.map(localOrder, i -> citationsInStorageOrder.get(i));
    }

    /*
     * indexInGlobalOrder
     */

    public void setIndexInGlobalOrder(Optional<Integer> indexInGlobalOrder) {
        this.indexInGlobalOrder = indexInGlobalOrder;
    }

    public Optional<Integer> getIndexInGlobalOrder() {
        return this.indexInGlobalOrder;
    }

    /*
     * referenceMarkNameForLinking
     */

    public Optional<String> getReferenceMarkNameForLinking() {
        return referenceMarkNameForLinking;
    }

    public void setReferenceMarkNameForLinking(Optional<String> referenceMarkNameForLinking) {
        this.referenceMarkNameForLinking = referenceMarkNameForLinking;
    }

    /*
     * citationMarker
     */

    public void setCitationMarker(Optional<OOText> citationMarker) {
        this.citationMarker = citationMarker;
    }

    public Optional<OOText> getCitationMarker() {
        return this.citationMarker;
    }

}
