package org.jabref.gui.openoffice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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

class Codec {
    private static final String BIB_CITATION = "JR_cite";
    private static final Pattern CITE_PATTERN =
        Pattern.compile(BIB_CITATION + "(\\d*)_(\\d*)_(.*)");

    /**
     * This is what we get back from parsing a refMarkName.
     *
     */
    public static class ParsedRefMark {
        /**  "", "0", "1" ... */
        public String i;
        /** in-text-citation type */
        public int itcType;
        /** Citation keys embedded in the reference mark. */
        public List<String> citationKeys;

        ParsedRefMark(String i, int itcType, List<String> citationKeys) {
            this.i = i;
            this.itcType = itcType;
            this.citationKeys = citationKeys;
        }
    }

    /**
     * Produce a reference mark name for JabRef for the given citation
     * key and itcType that does not yet appear among the reference
     * marks of the document.
     *
     * @param bibtexKey The citation key.
     * @param itcType   Encodes the effect of withText and
     *                  inParenthesis options.
     *
     * The first occurrence of bibtexKey gets no serial number, the
     * second gets 0, the third 1 ...
     *
     * Or the first unused in this series, after removals.
     */
    public static String getUniqueReferenceMarkName(DocumentConnection documentConnection,
                                                    String bibtexKey,
                                                    int itcType)
        throws NoDocumentException {

        XNameAccess xNamedRefMarks = documentConnection.getReferenceMarks();
        int i = 0;
        String name = BIB_CITATION + '_' + itcType + '_' + bibtexKey;
        while (xNamedRefMarks.hasByName(name)) {
            name = BIB_CITATION + i + '_' + itcType + '_' + bibtexKey;
            i++;
        }
        return name;
    }

    /**
     * Parse a JabRef reference mark name.
     *
     * @return Optional.empty() on failure.
     *
     */
    public static Optional<ParsedRefMark> parseRefMarkName(String refMarkName) {

        Matcher citeMatcher = CITE_PATTERN.matcher(refMarkName);
        if (!citeMatcher.find()) {
            return Optional.empty();
        }

        List<String> keys = Arrays.asList(citeMatcher.group(3).split(","));
        String i = citeMatcher.group(1);
        int itcType = Integer.parseInt(citeMatcher.group(2));
        return (Optional.of(new Codec.ParsedRefMark(i, itcType, keys)));
    }

//    /**
//     * Extract the list of citation keys from a reference mark name.
//     *
//     * @param name The reference mark name.
//     * @return The list of citation keys encoded in the name.
//     *
//     *         In case of duplicated citation keys,
//     *         only the first occurrence.
//     *         Otherwise their order is preserved.
//     *
//     *         If name does not match CITE_PATTERN,
//     *         an empty list of strings is returned.
//     */
//    private static List<String> parseRefMarkNameToUniqueCitationKeys(String name) {
//        Optional<ParsedRefMark> op = parseRefMarkName(name);
//        return (op.map(parsedRefMark ->
//                       parsedRefMark.citationKeys.stream()
//                       .distinct()
//                       .collect(Collectors.toList()))
//                .orElseGet(ArrayList::new));
//    }

    /**
     * @return true if name matches the pattern used for JabRef
     * reference mark names.
     */
    public static boolean isJabRefReferenceMarkName(String name) {
        return (CITE_PATTERN.matcher(name).find());
    }

    /**
     * Filter a list of reference mark names by `isJabRefReferenceMarkName`
     *
     * @param names The list to be filtered.
     */
    public static List<String> filterIsJabRefReferenceMarkName(List<String> names) {
        return (names
                .stream()
                .filter(Codec::isJabRefReferenceMarkName)
                .collect(Collectors.toList()));
    }
    /**
     * Get reference mark names from the document matching the pattern
     * used for JabRef reference mark names.
     *
     * Note: the names returned are in arbitrary order.
     *
     *
     *
     */
    public static List<String> getJabRefReferenceMarkNames(StorageBase.NamedRangeManager manager,
                                                           DocumentConnection documentConnection)
        throws
        NoDocumentException {
        List<String> allNames = manager.getUsedNames(documentConnection);
        return filterIsJabRefReferenceMarkName(allNames);
    }

//    /**
//     * For each name in referenceMarkNames set types[i] and
//     * bibtexKeys[i] to values parsed from referenceMarkNames.get(i)
//     *
//     * @param referenceMarkNames Should only contain parsable names.
//     * @param types              OUT Must be same length as referenceMarkNames.
//     * @param bibtexKeys         OUT First level must be same length as referenceMarkNames.
//     */
//    private static void parseRefMarkNamesToArrays(List<String> referenceMarkNames,
//                                                  int[] types,
//                                                  String[][] bibtexKeys) {
//
//        final int nRefMarks = referenceMarkNames.size();
//        assert (types.length == nRefMarks);
//        assert (bibtexKeys.length == nRefMarks);
//        for (int i = 0; i < nRefMarks; i++) {
//            final String name = referenceMarkNames.get(i);
//            Optional<ParsedRefMark> op = parseRefMarkName(name);
//            if (op.isEmpty()) {
//                // We have a problem. We want types[i] and bibtexKeys[i]
//                // to correspond to referenceMarkNames.get(i).
//                // And do not want null in bibtexKeys (or error code in types)
//                // on return.
//                throw new IllegalArgumentException(
//                    "parseRefMarkNamesToArrays expects parsable referenceMarkNames");
//            }
//            ParsedRefMark ov = op.get();
//            types[i] = ov.itcType;
//            bibtexKeys[i] = ov.citationKeys.toArray(String[]::new);
//        }
//    }
}
