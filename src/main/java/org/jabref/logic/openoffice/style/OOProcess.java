package org.jabref.logic.openoffice.style;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jabref.logic.bibtex.comparator.FieldComparator;
import org.jabref.logic.bibtex.comparator.FieldComparatorStack;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.style.CitationGroups;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OOProcess {

    static final Comparator<BibEntry> AUTHOR_YEAR_TITLE_COMPARATOR = makeAuthorYearTitleComparator();
    static final Comparator<BibEntry> YEAR_AUTHOR_TITLE_COMPARATOR = makeYearAuthorTitleComparator();

    private static final Logger LOGGER = LoggerFactory.getLogger(OOProcess.class);

    private static Comparator<BibEntry> makeAuthorYearTitleComparator() {
        FieldComparator a = new FieldComparator(StandardField.AUTHOR);
        FieldComparator y = new FieldComparator(StandardField.YEAR);
        FieldComparator t = new FieldComparator(StandardField.TITLE);

        List<Comparator<BibEntry>> ayt = new ArrayList<>(3);
        ayt.add(a);
        ayt.add(y);
        ayt.add(t);
        return new FieldComparatorStack<>(ayt);
    }

    private static Comparator<BibEntry> makeYearAuthorTitleComparator() {
        FieldComparator y = new FieldComparator(StandardField.YEAR);
        FieldComparator a = new FieldComparator(StandardField.AUTHOR);
        FieldComparator t = new FieldComparator(StandardField.TITLE);

        List<Comparator<BibEntry>> yat = new ArrayList<>(3);
        yat.add(y);
        yat.add(a);
        yat.add(t);
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
    public static void produceCitationMarkers(CitationGroups cgs,
                                              List<BibDatabase> databases,
                                              OOBibStyle style) {

        if (!cgs.hasGlobalOrder()) {
            throw new RuntimeException("produceCitationMarkers: globalOrder is misssing in cgs");
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
