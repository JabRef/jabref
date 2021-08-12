package org.jabref.logic.openoffice.style;

import java.util.Comparator;
import java.util.List;

import org.jabref.logic.bibtex.comparator.FieldComparator;
import org.jabref.logic.bibtex.comparator.FieldComparatorStack;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.style.CitationGroups;

public class OOProcess {

    static final Comparator<BibEntry> AUTHOR_YEAR_TITLE_COMPARATOR = makeAuthorYearTitleComparator();
    static final Comparator<BibEntry> YEAR_AUTHOR_TITLE_COMPARATOR = makeYearAuthorTitleComparator();

    private OOProcess() {
        /**/
    }

    private static Comparator<BibEntry> makeAuthorYearTitleComparator() {
        List<Comparator<BibEntry>> ayt = List.of(new FieldComparator(StandardField.AUTHOR),
                                                 new FieldComparator(StandardField.YEAR),
                                                 new FieldComparator(StandardField.TITLE));
        return new FieldComparatorStack<>(ayt);
    }

    private static Comparator<BibEntry> makeYearAuthorTitleComparator() {
        List<Comparator<BibEntry>> yat = List.of(new FieldComparator(StandardField.YEAR),
                                                 new FieldComparator(StandardField.AUTHOR),
                                                 new FieldComparator(StandardField.TITLE));
        return new FieldComparatorStack<>(yat);
    }

    /**
     *  The comparator used to sort within a group of merged
     *  citations.
     *
     *  The term used here is "multicite". The option controlling the
     *  order is "MultiCiteChronological" in style files.
     *
     *  Yes, they are always sorted one way or another.
     */
    public static Comparator<BibEntry> comparatorForMulticite(OOBibStyle style) {
        if (style.getMultiCiteChronological()) {
            return OOProcess.YEAR_AUTHOR_TITLE_COMPARATOR;
        } else {
            return OOProcess.AUTHOR_YEAR_TITLE_COMPARATOR;
        }
    }

    /**
     *  Fill cgs.bibliography and cgs.citationGroupsUnordered//CitationMarker
     *  according to style.
     */
    public static void produceCitationMarkers(CitationGroups cgs, List<BibDatabase> databases, OOBibStyle style) {

        if (!cgs.hasGlobalOrder()) {
            throw new IllegalStateException("produceCitationMarkers: globalOrder is misssing in cgs");
        }

        cgs.lookupCitations(databases);
        cgs.imposeLocalOrder(comparatorForMulticite(style));

        // fill CitationGroup.citationMarker
        if (style.isCitationKeyCiteMarkers()) {
            OOProcessCitationKeyMarkers.produceCitationMarkers(cgs, style);
        } else if (style.isNumberEntries()) {
            OOProcessNumericMarkers.produceCitationMarkers(cgs, style);
        } else {
            OOProcessAuthorYearMarkers.produceCitationMarkers(cgs, style);
        }
    }

}
