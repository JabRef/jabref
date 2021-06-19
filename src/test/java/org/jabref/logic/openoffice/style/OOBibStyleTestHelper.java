package org.jabref.logic.openoffice.style;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationLookupResult;
import org.jabref.model.openoffice.style.CitationMarkerEntry;
import org.jabref.model.openoffice.style.CitationMarkerNumericBibEntry;
import org.jabref.model.openoffice.style.CitationMarkerNumericEntry;
import org.jabref.model.openoffice.style.NonUniqueCitationMarker;
import org.jabref.model.openoffice.style.PageInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OOBibStyleTestHelper {
    /*
     * begin Helpers for testing style.getNumCitationMarker2
     */

    /*
     * Minimal implementation for CitationMarkerNumericEntry
     */
    static class CitationMarkerNumericEntryImpl implements CitationMarkerNumericEntry {

        /*
         * The number encoding "this entry is unresolved" for the constructor.
         */
        public final static int UNRESOLVED_ENTRY_NUMBER = 0;

        private String citationKey;
        private Optional<Integer> num;
        private Optional<OOText> pageInfo;

        public CitationMarkerNumericEntryImpl(String citationKey, int num, Optional<OOText> pageInfo) {
            this.citationKey = citationKey;
            this.num = (num == UNRESOLVED_ENTRY_NUMBER
                        ? Optional.empty()
                        : Optional.of(num));
            this.pageInfo = PageInfo.normalizePageInfo(pageInfo);
        }

        @Override
        public String getCitationKey() {
            return citationKey;
        }

        @Override
        public Optional<Integer> getNumber() {
            return num;
        }

        @Override
        public Optional<OOText> getPageInfo() {
            return pageInfo;
        }
    }

    static class CitationMarkerNumericBibEntryImpl implements CitationMarkerNumericBibEntry {
        String key;
        Optional<Integer> number;

        public CitationMarkerNumericBibEntryImpl(String key, Optional<Integer> number) {
            this.key = key;
            this.number = number;
        }

        @Override
        public String getCitationKey() {
            return key;
        }

        @Override
        public Optional<Integer> getNumber() {
            return number;
        }
    }

    static CitationMarkerNumericBibEntry numBibEntry(String key, Optional<Integer> number) {
        return new CitationMarkerNumericBibEntryImpl(key, number);
    }

    /**
     * Reproduce old method
     *
     * @param inList true means label for the bibliography
     */
    static String runGetNumCitationMarker2a(OOBibStyle style,
                                            List<Integer> num, int minGroupingCount, boolean inList) {
        if (inList) {
            if (num.size() != 1) {
                throw new IllegalArgumentException("Numeric label for the bibliography with "
                                                   + String.valueOf(num.size()) + " numbers?");
            }
            int n = num.get(0);
            CitationMarkerNumericBibEntryImpl x =
                new CitationMarkerNumericBibEntryImpl("key",
                                                      (n == 0) ? Optional.empty() : Optional.of(n));
            return style.getNumCitationMarkerForBibliography(x).toString();
        } else {
            List<CitationMarkerNumericEntry> input =
                num.stream()
                .map(n ->
                     new CitationMarkerNumericEntryImpl("key" + String.valueOf(n),
                                                        n,
                                                        Optional.empty()))
                .collect(Collectors.toList());
            return style.getNumCitationMarker2(input, minGroupingCount).toString();
        }
    }

    /*
     * Unlike getNumCitationMarker, getNumCitationMarker2 can handle pageInfo.
     */
    static CitationMarkerNumericEntry numEntry(String key, int num, String pageInfoOrNull) {
        Optional<OOText> pageInfo = Optional.ofNullable(OOText.fromString(pageInfoOrNull));
        return new CitationMarkerNumericEntryImpl(key, num, pageInfo);
    }

    static String runGetNumCitationMarker2b(OOBibStyle style,
                                            int minGroupingCount,
                                            CitationMarkerNumericEntry... s) {
        List<CitationMarkerNumericEntry> input = Stream.of(s).collect(Collectors.toList());
        OOText res = style.getNumCitationMarker2(input, minGroupingCount);
        return res.toString();
    }

    /*
     * end Helpers for testing style.getNumCitationMarker2
     */

    /*
     * begin helper
     */
    static CitationMarkerEntry makeCitationMarkerEntry(BibEntry entry,
                                                       BibDatabase database,
                                                       String uniqueLetterQ,
                                                       String pageInfoQ,
                                                       boolean isFirstAppearanceOfSource) {
        if (entry.getCitationKey().isEmpty()) {
            throw new IllegalArgumentException("entry.getCitationKey() is empty");
        }
        String citationKey = entry.getCitationKey().get();
        Citation result = new Citation(citationKey);
        result.setLookupResult(Optional.of(new CitationLookupResult(entry, database)));
        result.setUniqueLetter(Optional.ofNullable(uniqueLetterQ));
        Optional<OOText> pageInfo = Optional.ofNullable(OOText.fromString(pageInfoQ));
        result.setPageInfo(PageInfo.normalizePageInfo(pageInfo));
        result.setIsFirstAppearanceOfSource(isFirstAppearanceOfSource);
        return result;
    }

    /*
     * Similar to old API. pageInfo is new, and unlimAuthors is
     * replaced with isFirstAppearanceOfSource
     */
    static String getCitationMarker2(OOBibStyle style,
                                     List<BibEntry> entries,
                                     Map<BibEntry, BibDatabase> entryDBMap,
                                     boolean inParenthesis,
                                     String[] uniquefiers,
                                     Boolean[] isFirstAppearanceOfSource,
                                     String[] pageInfo) {
        if (uniquefiers == null) {
            uniquefiers = new String[entries.size()];
            Arrays.fill(uniquefiers, null);
        }
        if (pageInfo == null) {
            pageInfo = new String[entries.size()];
            Arrays.fill(pageInfo, null);
        }
        if (isFirstAppearanceOfSource == null) {
            isFirstAppearanceOfSource = new Boolean[entries.size()];
            Arrays.fill(isFirstAppearanceOfSource, false);
        }
        List<CitationMarkerEntry> citationMarkerEntries = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            BibEntry entry = entries.get(i);
            CitationMarkerEntry e = makeCitationMarkerEntry(entry,
                                                            entryDBMap.get(entry),
                                                            uniquefiers[i],
                                                            pageInfo[i],
                                                            isFirstAppearanceOfSource[i]);
            citationMarkerEntries.add(e);
        }
        return style.createCitationMarker(citationMarkerEntries,
                                          inParenthesis,
                                          NonUniqueCitationMarker.THROWS).toString();
    }

    /*
     * end helper
     */

    static void testGetNumCitationMarkerExtra(OOBibStyle style) throws IOException {
        // Identical numeric entries are joined.
        assertEquals("[1; 2]", runGetNumCitationMarker2b(style, 3,
                                     numEntry("x1", 1, null),
                                     numEntry("x2", 2, null),
                                     numEntry("x1", 2, null),
                                     numEntry("x2", 1, null)));

        // ... unless minGroupingCount <= 0
        assertEquals("[1; 1; 2; 2]", runGetNumCitationMarker2b(style, 0,
                                     numEntry("x1", 1, null),
                                     numEntry("x2", 2, null),
                                     numEntry("x1", 2, null),
                                     numEntry("x2", 1, null)));

        // ... or have different pageInfos
        assertEquals("[1; p1a; 1; p1b; 2; p2; 3]", runGetNumCitationMarker2b(style, 1,
                                                         numEntry("x1", 1, "p1a"),
                                                         numEntry("x1", 1, "p1b"),
                                                         numEntry("x2", 2, "p2"),
                                                         numEntry("x2", 2, "p2"),
                                                         numEntry("x3", 3, null),
                                                         numEntry("x3", 3, null)));

        // Consecutive numbers can become a range ...
        assertEquals("[1-3]", runGetNumCitationMarker2b(style, 1,
                                    numEntry("x1", 1, null),
                                    numEntry("x2", 2, null),
                                    numEntry("x3", 3, null)));

        // ... unless minGroupingCount is too high
        assertEquals("[1; 2; 3]", runGetNumCitationMarker2b(style, 4,
                                        numEntry("x1", 1, null),
                                        numEntry("x2", 2, null),
                                        numEntry("x3", 3, null)));

        // ... or if minGroupingCount <= 0
        assertEquals("[1; 2; 3]", runGetNumCitationMarker2b(style, 0,
                                        numEntry("x1", 1, null),
                                        numEntry("x2", 2, null),
                                        numEntry("x3", 3, null)));

        // ... a pageInfo needs to be emitted
        assertEquals("[1; p1; 2-3]", runGetNumCitationMarker2b(style, 1,
                                           numEntry("x1", 1, "p1"),
                                           numEntry("x2", 2, null),
                                           numEntry("x3", 3, null)));

        // null and "" pageInfos are taken as equal.
        // Due to trimming, "   " is the same as well.
        assertEquals("[1]", runGetNumCitationMarker2b(style, 1,
                                  numEntry("x1", 1, ""),
                                  numEntry("x1", 1, null),
                                  numEntry("x1", 1, "  ")));

        // pageInfos are trimmed
        assertEquals("[1; p1]", runGetNumCitationMarker2b(style, 1,
                                      numEntry("x1", 1, "p1"),
                                      numEntry("x1", 1, " p1"),
                                      numEntry("x1", 1, "p1 ")));

        // The citation numbers come out sorted
        assertEquals("[3-5; 7; 10-12]", runGetNumCitationMarker2b(style, 1,
                                              numEntry("x12", 12, null),
                                              numEntry("x7", 7, null),
                                              numEntry("x3", 3, null),
                                              numEntry("x4", 4, null),
                                              numEntry("x11", 11, null),
                                              numEntry("x10", 10, null),
                                              numEntry("x5", 5, null)));

        // pageInfos are sorted together with the numbers
        // (but they inhibit ranges where they are, even if they are identical,
        //  but not empty-or-null)
        assertEquals("[3; p3; 4; p4; 5; p5; 7; p7; 10; px; 11; px; 12; px]",
                     runGetNumCitationMarker2b(style, 1,
                                               numEntry("x12", 12, "px"),
                                               numEntry("x7", 7, "p7"),
                                               numEntry("x3", 3, "p3"),
                                               numEntry("x4", 4, "p4"),
                                               numEntry("x11", 11, "px"),
                                               numEntry("x10", 10, "px"),
                                               numEntry("x5", 5, "p5")));

        // pageInfo sorting (for the same number)
        assertEquals("[1; 1; a; 1; b]",
                     runGetNumCitationMarker2b(style, 1,
                                               numEntry("x1", 1, ""),
                                               numEntry("x1", 1, "b"),
                                               numEntry("x1", 1, "a")));

        // pageInfo sorting (for the same number) is not numeric.
        assertEquals("[1; p100; 1; p20; 1; p9]",
                     runGetNumCitationMarker2b(style, 1,
                                               numEntry("x1", 1, "p20"),
                                               numEntry("x1", 1, "p9"),
                                               numEntry("x1", 1, "p100")));

        assertEquals("[1-3]",
                     runGetNumCitationMarker2b(style, 1,
                                               numEntry("x1", 1, null),
                                               numEntry("x2", 2, null),
                                               numEntry("x3", 3, null)));

        assertEquals("[1; 2; 3]",
                     runGetNumCitationMarker2b(style, 5,
                                               numEntry("x1", 1, null),
                                               numEntry("x2", 2, null),
                                               numEntry("x3", 3, null)));

        assertEquals("[1; 2; 3]",
                     runGetNumCitationMarker2b(style, -1,
                                               numEntry("x1", 1, null),
                                               numEntry("x2", 2, null),
                                               numEntry("x3", 3, null)));

        assertEquals("[1; 3; 12]",
                     runGetNumCitationMarker2b(style, 1,
                                               numEntry("x1", 1, null),
                                               numEntry("x12", 12, null),
                                               numEntry("x3", 3, null)));

        assertEquals("[3-5; 7; 10-12]",
                     runGetNumCitationMarker2b(style, 1,
                                               numEntry("x12", 12, ""),
                                               numEntry("x7", 7, ""),
                                               numEntry("x3", 3, ""),
                                               numEntry("x4", 4, ""),
                                               numEntry("x11", 11, ""),
                                               numEntry("x10", 10, ""),
                                               numEntry("x5", 5, "")));
    }
}
