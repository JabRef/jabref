package org.jabref.logic.openoffice.style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.CitationMarkerNumericBibEntry;
import org.jabref.model.openoffice.style.CitationMarkerNumericEntry;
import org.jabref.model.openoffice.style.PageInfo;
import org.jabref.model.openoffice.util.OOListUtil;

class OOBibStyleGetNumCitationMarker {

    /*
     * The number encoding "this entry is unresolved"
     */
    public final static int UNRESOLVED_ENTRY_NUMBER = 0;

    private OOBibStyleGetNumCitationMarker() {
        /**/
    }

    /**
     * Defines sort order for CitationMarkerNumericEntry.
     */
    private static int compareCitationMarkerNumericEntry(CitationMarkerNumericEntry a,
                                                         CitationMarkerNumericEntry b) {
        int na = a.getNumber().orElse(UNRESOLVED_ENTRY_NUMBER);
        int nb = b.getNumber().orElse(UNRESOLVED_ENTRY_NUMBER);
        int res = Integer.compare(na, nb);
        if (res == 0) {
            res = PageInfo.comparePageInfo(a.getPageInfo(), b.getPageInfo());
        }
        return res;
    }

    /**
     *  Create a numeric marker for use in the bibliography as label for the entry.
     *
     *  To support for example numbers in superscript without brackets for the text,
     *  but "[1]" form for the bibliography, the style can provide
     *  the optional "BracketBeforeInList" and "BracketAfterInList" strings
     *  to be used in the bibliography instead of "BracketBefore" and "BracketAfter"
     *
     *  @return "[${number}]" where
     *       "[" stands for BRACKET_BEFORE_IN_LIST (with fallback BRACKET_BEFORE)
     *       "]" stands for BRACKET_AFTER_IN_LIST (with fallback BRACKET_AFTER)
     *       "${number}" stands for the formatted number.
     */
    public static OOText getNumCitationMarkerForBibliography(OOBibStyle style,
                                                             CitationMarkerNumericBibEntry entry) {
        // prefer BRACKET_BEFORE_IN_LIST and BRACKET_AFTER_IN_LIST
        String bracketBefore = style.getBracketBeforeInListWithFallBack();
        String bracketAfter = style.getBracketAfterInListWithFallBack();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(style.getCitationGroupMarkupBefore());
        stringBuilder.append(bracketBefore);
        final Optional<Integer> current = entry.getNumber();
        stringBuilder.append(current.isPresent()
                  ? String.valueOf(current.get())
                  : (OOBibStyle.UNDEFINED_CITATION_MARKER + entry.getCitationKey()));
        stringBuilder.append(bracketAfter);
        stringBuilder.append(style.getCitationGroupMarkupAfter());
        return OOText.fromString(stringBuilder.toString());
    }

    /*
     * emitBlock : a helper for getNumCitationMarker2
     *
     * Given a block containing either a single entry or two or more
     * entries that are joinable into an "i-j" form, append to {@code stringBuilder} the
     * formatted text.
     *
     * Assumes:
     *
     * - block is not empty
     *
     * - For a block with a single element the element may have
     *    pageInfo and its num part may be Optional.empty()
     *
     * - For a block with two or more elements
     *
     *   - The elements do not have pageInfo and their number part is
     *     not empty.
     *
     *   - The elements number parts are consecutive positive integers,
     *     without repetition.
     *
     */
    private static void emitBlock(List<CitationMarkerNumericEntry> block,
                                  OOBibStyle style,
                                  int minGroupingCount,
                                  StringBuilder stringBuilder) {

        final int blockSize = block.size();
        if (blockSize == 0) {
            throw new IllegalArgumentException("The block is empty");
        }

        if (blockSize == 1) {
            // Add single entry:
            CitationMarkerNumericEntry entry = block.get(0);
            final Optional<Integer> num = entry.getNumber();
            stringBuilder.append(num.isEmpty()
                                 ? (OOBibStyle.UNDEFINED_CITATION_MARKER + entry.getCitationKey())
                                 : String.valueOf(num.get()));
            // Emit pageInfo
            Optional<OOText> pageInfo = entry.getPageInfo();
            if (pageInfo.isPresent()) {
                stringBuilder.append(style.getPageInfoSeparator());
                stringBuilder.append(OOText.toString(pageInfo.get()));
            }
            return;
        }

        if (blockSize >= 2) {

            /*
             * Check assumptions
             */

            if (block.stream().anyMatch(x -> x.getPageInfo().isPresent())) {
                throw new IllegalArgumentException("Found pageInfo in a block with more than one elements");
            }

            if (block.stream().anyMatch(x -> x.getNumber().isEmpty())) {
                throw new IllegalArgumentException("Found unresolved entry in a block with more than one elements");
            }

            for (int j = 1; j < blockSize; j++) {
                if ((block.get(j).getNumber().get() - block.get(j - 1).getNumber().get()) != 1) {
                    throw new IllegalArgumentException("Numbers are not consecutive");
                }
            }

            /*
             * Do the actual work
             */

            if (blockSize >= minGroupingCount) {
                int first = block.get(0).getNumber().get();
                int last = block.get(blockSize - 1).getNumber().get();
                if (last != (first + blockSize - 1)) {
                    throw new IllegalArgumentException("blockSize and length of num range differ");
                }

                // Emit: "first-last"
                stringBuilder.append(first);
                stringBuilder.append(style.getGroupedNumbersSeparator());
                stringBuilder.append(last);
            } else {

                // Emit: first, first+1,..., last
                for (int j = 0; j < blockSize; j++) {
                    if (j > 0) {
                        stringBuilder.append(style.getCitationSeparator());
                    }
                    stringBuilder.append(block.get(j).getNumber().get());
                }
            }
            return;
        }
    }

    /**
     * Format a number-based citation marker for the given number or numbers.
     *
     * @param entries Provide the citation numbers.
     *
     *               An Optional.empty() number means: could not look this up
     *               in the databases. Positive integers are the valid numbers.
     *
     *               Duplicate citation numbers are allowed:
     *
     *                 - If their pageInfos are identical, only a
     *                   single instance is emitted.
     *
     *                 - If their pageInfos differ, the number is emitted with each
     *                    distinct pageInfo.
     *
     *                    pageInfos are expected to be normalized
     *
     * @param minGroupingCount Zero and negative means never group.
     *                    Only used by tests to override the value in style.
     *
     * @return The text for the citation.
     *
     */
    public static OOText getNumCitationMarker2(OOBibStyle style,
                                               List<CitationMarkerNumericEntry> entries,
                                               int minGroupingCount) {

        final boolean joinIsDisabled = (minGroupingCount <= 0);
        final int nCitations = entries.size();

        final String bracketBefore = style.getBracketBefore();
        final String bracketAfter = style.getBracketAfter();

        // Sort a copy of entries
        List<CitationMarkerNumericEntry> sorted = OOListUtil.map(entries, e -> e);
        sorted.sort(OOBibStyleGetNumCitationMarker::compareCitationMarkerNumericEntry);

        // "["
        StringBuilder stringBuilder = new StringBuilder(bracketBefore);

        /*
         *  Original:
         *  [2,3,4]   -> [2-4]
         *  [0,1,2]   -> [??,1,2]
         *  [0,1,2,3] -> [??,1-3]
         *
         *  Now we have to consider: duplicate numbers and pageInfos
         *  [1,1] -> [1]
         *  [1,1 "pp nn"] -> keep separate if pageInfo differs
         *  [1 "pp nn",1 "pp nn"] -> [1 "pp nn"]
         */

        boolean blocksEmitted = false;
        List<CitationMarkerNumericEntry> currentBlock = new ArrayList<>();
        List<CitationMarkerNumericEntry> nextBlock = new ArrayList<>();

        for (int i = 0; i < nCitations; i++) {

            final CitationMarkerNumericEntry current = sorted.get(i);
            if (current.getNumber().isPresent() && current.getNumber().get() < 0) {
                throw new IllegalArgumentException("getNumCitationMarker2: found negative number");
            }

            if (currentBlock.isEmpty()) {
                currentBlock.add(current);
            } else {
                CitationMarkerNumericEntry prev = currentBlock.get(currentBlock.size() - 1);
                if (current.getNumber().isEmpty() || prev.getNumber().isEmpty()) {
                    nextBlock.add(current); // do not join if not found
                } else if (joinIsDisabled) {
                    nextBlock.add(current); // join disabled
                } else if (compareCitationMarkerNumericEntry(current, prev) == 0) {
                    // Same as prev, just forget it.
                } else if ((current.getNumber().get() == (prev.getNumber().get() + 1))
                           && (prev.getPageInfo().isEmpty())
                           && (current.getPageInfo().isEmpty())) {
                    // Just two consecutive numbers without pageInfo: join
                    currentBlock.add(current);
                } else {
                    // do not join
                    nextBlock.add(current);
                }
            }

            if (!nextBlock.isEmpty()) {
                // emit current block
                if (blocksEmitted) {
                    stringBuilder.append(style.getCitationSeparator());
                }
                emitBlock(currentBlock, style, minGroupingCount, stringBuilder);
                blocksEmitted = true;
                currentBlock = nextBlock;
                nextBlock = new ArrayList<>();
            }

        }

        if (!nextBlock.isEmpty()) {
            throw new IllegalStateException("impossible: (nextBlock.size() != 0) after loop");
        }

        if (!currentBlock.isEmpty()) {
            // We are emitting a block
            if (blocksEmitted) {
                stringBuilder.append(style.getCitationSeparator());
            }
            emitBlock(currentBlock, style, minGroupingCount, stringBuilder);
        }

        // Emit: "]"
        stringBuilder.append(bracketAfter);
        return OOText.fromString(stringBuilder.toString());
    }

}
