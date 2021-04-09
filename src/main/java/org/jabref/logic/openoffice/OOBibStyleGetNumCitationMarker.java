package org.jabref.logic.openoffice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class OOBibStyleGetNumCitationMarker {

    /*
     * Helper class for sorting citation numbers while
     * maintaining their correspondance to pageInfos.
     */
    private static class NumberWithPageInfo {
        int num;
        String pageInfo;
        NumberWithPageInfo(int num, String pageInfo) {
            this.num = num;
            this.pageInfo = pageInfo;
        }
    }

    /**
     * Defines sort order for NumberWithPageInfo entries.
     *
     * null comes before non-null
     */
    private static int compareNumberWithPageInfo(NumberWithPageInfo a, NumberWithPageInfo b) {
        int res = Integer.compare(a.num, b.num);
        if (res == 0) {
            res = OOBibStyle.comparePageInfo( a.pageInfo, b.pageInfo );
        }
        return res;
    }

    private enum CitationMarkerPurpose {
        /** Creating citation marker for in-text citation. */
        CITATION,
        /** Creating citation marker for the bibliography. */
        BIBLIOGRAPHY
    }

    /**
     * See {@see getNumCitationMarkerCommon} for details.
     */
    public static String getNumCitationMarkerForInText(OOBibStyle style,
                                                       List<Integer> numbers,
                                                       int minGroupingCount,
                                                       List<String> pageInfosForCitations) {
        return getNumCitationMarkerCommon(style,
                                          numbers,
                                          minGroupingCount,
                                          CitationMarkerPurpose.CITATION,
                                          pageInfosForCitations);
    }

    /**
     *  Create a numeric marker for use in the bibliography as label for the entry.
     *
     *  To support for example numbers in superscript without brackets for the text,
     *  but "[1]" form for the bibliogaphy, the style can provide
     *  the optional "BracketBeforeInList" and "BracketAfterInList" strings
     *  to be used in the bibliography instead of "BracketBefore" and "BracketAfter"
     *
     *  @return "[${number}]" where
     *       "[" stands for BRACKET_BEFORE_IN_LIST (with fallback BRACKET_BEFORE)
     *       "]" stands for BRACKET_AFTER_IN_LIST (with fallback BRACKET_AFTER)
     *       "${number}" stands for the formatted number.
     */
    public static String getNumCitationMarkerForBibliography(OOBibStyle style,
                                                             int number) {
        return getNumCitationMarkerCommon(style,
                                          Collections.singletonList(number),
                                          0,
                                          CitationMarkerPurpose.BIBLIOGRAPHY,
                                          null);
    }

    /**
     * Format a number-based citation marker for the given number or numbers.
     *
     * This is the common implementation behind
     * getNumCitationMarkerForInText and
     * getNumCitationMarkerForBibliography. The latter could be easily
     * separated unless there is (or going to be) a need for handling
     * multiple numbers or page info by getNumCitationMarkerForBibliography.
     *
     * @param numbers The citation numbers.
     *
     *               A zero in the list means: could not look this up
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
     *                    For pageInfo null and "" (after
     *                    pageInfo.trim()) are considered equal (and missing).
     *
     * @param minGroupingCount Zero and negative means never group
     *
     * @param purpose BIBLIOGRAPHY (was: inList==True) when creating for a bibliography entry,
     *                CITATION (was: inList=false) when creating in-text citation.
     *
     *               If BIBLIOGRAPHY: Prefer BRACKET_BEFORE_IN_LIST over BRACKET_BEFORE,
     *                                   and BRACKET_AFTER_IN_LIST over BRACKET_AFTER.
     *                                Ignore pageInfosForCitations.
     *
     * @param pageInfosForCitations  Null for "none", or a list with a
     *        pageInfo for each citation. Any or all of these can be null as well.
     *
     * @return The text for the citation.
     *
     */
    private static String getNumCitationMarkerCommon(OOBibStyle style,
                                                     List<Integer> numbers,
                                                     int minGroupingCount,
                                                     CitationMarkerPurpose purpose,
                                                     List<String> pageInfosForCitations) {

        final boolean joinIsDisabled = (minGroupingCount <= 0);
        final int notFoundInDatabases = 0;
        final int nCitations = numbers.size();

        /*
         * strictPurpose: if true, require (nCitations == 1) when (purpose == BIBLIOGRAPHY),
         *                otherwise allow multiple citation numbers and process the BIBLIOGRAPHY case
         *                as CITATION with no pageInfo.
         */
        final boolean strictPurpose = true;

        String bracketBefore = style.getBracketBefore();
        String bracketAfter = style.getBracketAfter();

        /*
         * purpose == BIBLIOGRAPHY means: we are formatting for the
         *                       bibliography, (not for in-text citation).
         */
        if (purpose == CitationMarkerPurpose.BIBLIOGRAPHY) {
            // prefer BRACKET_BEFORE_IN_LIST and BRACKET_AFTER_IN_LIST
            bracketBefore = style.getBracketBeforeInListWithFallBack();
            bracketAfter = style.getBracketAfterInListWithFallBack();

            if (strictPurpose) {
                // If (purpose==BIBLIOGRAPHY), then
                // we expect exactly one number here, and can handle quickly
                if (nCitations != 1) {
                    throw new RuntimeException(
                        "getNumCitationMarker:"
                        + "nCitations != 1 for purpose==BIBLIOGRAPHY."
                        + String.format(" nCitations = %d", nCitations));
                }
                //
                StringBuilder sb = new StringBuilder(bracketBefore);
                final int current = numbers.get(0);
                if (current < 0) {
                    throw new RuntimeException("getNumCitationMarker: found negative value");
                }
                sb.append(current != notFoundInDatabases
                          ? String.valueOf(current)
                          : OOBibStyle.UNDEFINED_CITATION_MARKER);
                sb.append(bracketAfter);
                return sb.toString();
            }
        }

        /*
         * From here:
         *  - formatting for in-text (not for bibliography)
         *  - need to care about pageInfosForCitations
         *
         *  - In case {@code strictPurpose} above is set to false and allows us to
         *    get here, and {@code purpose==BIBLIOGRAPHY}, then we just fill
         *    pageInfos with null values.
         */
        List<String> pageInfos =
            OOBibStyle.regularizePageInfosForCitations((purpose == CitationMarkerPurpose.BIBLIOGRAPHY
                                                        ? null
                                                        : pageInfosForCitations),
                                                       numbers.size());

        // Sort the numbers, together with the corresponding pageInfo values
        List<NumberWithPageInfo> nps = new ArrayList<>();
        for (int i = 0; i < nCitations; i++) {
            nps.add(new NumberWithPageInfo(numbers.get(i), pageInfos.get(i)));
        }
        Collections.sort(nps, OOBibStyleGetNumCitationMarker::compareNumberWithPageInfo);

        // "["
        StringBuilder sb = new StringBuilder(bracketBefore);

        /*
         * int emitBlock(List<NumberWithPageInfo> block)
         *
         * Given a block containing 1 or (two or more)
         * NumberWithPageInfo entries collected as singletons or
         * joinable into an "i-j" form, append to {@code sb} the
         * formatted text.
         *
         * Assumes:
         *
         * - block is not empty
         *
         * - For a block with a single element the element may have
         *    pageInfo and its num part may be zero
         *    (notFoundInDatabases).
         *
         * - For a block with two or more elements
         *
         *   - The elements do not have pageInfo and their num part is
         *     not zero.
         *
         *   - The elements num parts are consecutive positive integers,
         *     without repetition.
         *
         * Note: this function is long enough to move into a separate method.
         *       On the other hand, its assumptions strongly tie it to
         *       the loop below that collects the block.
         *
         * @return The number of blocks emitted. Since currently
         *         throws if the block is empty, the returned value is
         *         always 1.
         *
         */
        ToIntFunction<List<NumberWithPageInfo>> emitBlock = (List<NumberWithPageInfo> block) -> {
            // uses:  sb, this,

            final int blockSize = block.size();
            if (blockSize == 0) {
                throw new RuntimeException("We should not get here");
                // return 0;
            }

            if (blockSize == 1) {
                // Add single entry:
                final int num = block.get(0).num;
                sb.append(num == notFoundInDatabases
                          ? OOBibStyle.UNDEFINED_CITATION_MARKER
                          : String.valueOf(num));
                // Emit pageInfo
                String pageInfo = block.get(0).pageInfo;
                if (pageInfo != null) {
                    sb.append(style.getPageInfoSeparator() + pageInfo);
                }
            } else {

                /*
                 * Check assumptions
                 */

                // block has at least 2 elements
                if (blockSize < 2) {
                    throw new RuntimeException("impossible: (blockSize < 2)");
                }
                // None of these elements has a pageInfo,
                // because if it had, we would not join.
                for (NumberWithPageInfo x : block) {
                    if (x.pageInfo != null) {
                        throw new RuntimeException("impossible: (x.pageInfo != null)");
                    }
                }
                // None of these elements needs UNDEFINED_CITATION_MARKER,
                // because if it did, we would not join.
                for (NumberWithPageInfo x : block) {
                    if (x.num == notFoundInDatabases) {
                        throw new RuntimeException("impossible: (x.num == notFoundInDatabases)");
                    }
                }
                // consecutive elements have consecutive numbers
                for (int j = 1; j < blockSize; j++) {
                    if (block.get(j).num != (block.get(j - 1).num + 1)) {
                        throw new RuntimeException("impossible: consecutive elements"
                                                   + " without consecutive numbers");
                    }
                }

                /*
                 * Do the actual work
                 */
                if (blockSize >= minGroupingCount) {
                    int first = block.get(0).num;
                    int last = block.get(blockSize - 1).num;
                    if (((last + 1) - first) != blockSize) {
                        throw new RuntimeException("impossible:"
                                                   + " blockSize and length of num range differ");
                    }
                    // Emit: "first-last"
                    sb.append(first);
                    sb.append(style.getGroupedNumbersSeparator());
                    sb.append(last);
                } else {
                    // Emit: first,first+1,...,last
                    for (int j = 0; j < blockSize; j++) {
                        if (j > 0) {
                            sb.append(style.getCitationSeparator());
                        }
                        sb.append(block.get(j).num);
                    }
                }
            }
            return 1;
        };

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

        int blocksEmitted = 0;
        List<NumberWithPageInfo> currentBlock = new ArrayList<>();
        List<NumberWithPageInfo> nextBlock = new ArrayList<>();

        for (int i = 0; i < nCitations; i++) {

            final NumberWithPageInfo current = nps.get(i);
            if (current.num < 0) {
                throw new RuntimeException("getNumCitationMarker: found negative value");
            }

            if (currentBlock.size() == 0) {
                currentBlock.add(current);
            } else {
                NumberWithPageInfo prev = currentBlock.get(currentBlock.size() - 1);
                if ((notFoundInDatabases == current.num)
                     || (notFoundInDatabases == prev.num)) {
                    nextBlock.add(current); // do not join if not found
                } else if (joinIsDisabled) {
                    nextBlock.add(current); // join disabled
                } else if (compareNumberWithPageInfo(current, prev) == 0) {
                    // Same as prev, just forget it.
                } else if ((current.num == (prev.num + 1))
                           && (prev.pageInfo == null)
                           && (current.pageInfo == null)) {
                    // Just two consecutive numbers without pageInfo: join
                    currentBlock.add(current);
                } else {
                    // do not join
                    nextBlock.add(current);
                }
            }

            if (nextBlock.size() > 0) {
                // emit current block
                // We are emitting a block
                if (blocksEmitted > 0) {
                    sb.append(style.getCitationSeparator());
                }
                int emittedNow = emitBlock.applyAsInt(currentBlock);
                if (emittedNow > 0) {
                    blocksEmitted += emittedNow;
                    currentBlock = nextBlock;
                    nextBlock = new ArrayList<>();
                }
            } // blockSize != 0

        } // for i

        if (nextBlock.size() != 0) {
            throw new RuntimeException("impossible: (nextBlock.size() != 0) after loop");
        }

        if (currentBlock.size() > 0) {
            // We are emitting a block
            if (blocksEmitted > 0) {
                sb.append(style.getCitationSeparator());
            }
            emitBlock.applyAsInt(currentBlock);
        }

        // Emit: "]"
        sb.append(bracketAfter);
        return sb.toString();
    }

}
