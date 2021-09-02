package org.jabref.logic.openoffice.backend;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.model.openoffice.style.CitationType;

/**
 *  How and what is encoded in reference mark names under JabRef 5.2.
 *
 *  - pageInfo does not appear here. It is not encoded in the mark name.
 */
class Codec52 {
    private static final String BIB_CITATION = "JR_cite";
    private static final Pattern CITE_PATTERN =
        // Pattern.compile(BIB_CITATION + "(\\d*)_(\\d*)_(.*)");
        // citationType is always "1" "2" or "3"
        Pattern.compile(BIB_CITATION + "(\\d*)_([123])_(.*)");

    private Codec52() {
        /**/
    }

    /**
     * This is what we get back from parsing a refMarkName.
     */
    public static class ParsedMarkName {
        /**  "", "0", "1" ... */
        public final String index;
        /** in-text-citation type */
        public final CitationType citationType;
        /** Citation keys embedded in the reference mark. */
        public final List<String> citationKeys;

        ParsedMarkName(String index, CitationType citationType, List<String> citationKeys) {
            Objects.requireNonNull(index);
            Objects.requireNonNull(citationKeys);
            this.index = index;
            this.citationType = citationType;
            this.citationKeys = citationKeys;
        }
    }

    /**
     * Integer representation was written into the document in JabRef52, keep it for compatibility.
     */
    private static CitationType citationTypeFromInt(int code) {
        return switch (code) {
            case 1 -> CitationType.AUTHORYEAR_PAR;
            case 2 -> CitationType.AUTHORYEAR_INTEXT;
            case 3 -> CitationType.INVISIBLE_CIT;
            default -> throw new IllegalArgumentException("Invalid CitationType code");
        };
    }

    private static int citationTypeToInt(CitationType type) {
        return switch (type) {
            case AUTHORYEAR_PAR -> 1;
            case AUTHORYEAR_INTEXT -> 2;
            case INVISIBLE_CIT -> 3;
            default -> throw new IllegalArgumentException("Invalid CitationType");
        };
    }

    /**
     * Produce a reference mark name for JabRef for the given citationType and list citation keys that
     * does not yet appear among the reference marks of the document.
     *
     * @param usedNames    Reference mark names already in use.
     * @param citationKeys Identifies the cited sources.
     * @param citationType Encodes the effect of withText and inParenthesis options.
     *
     * The first occurrence of citationKeys gets no serial number, the second gets 0, the third 1 ...
     *
     * Or the first unused in this series, after removals.
     */
    public static String getUniqueMarkName(Set<String> usedNames,
                                           List<String> citationKeys,
                                           CitationType citationType) {

        String citationKeysPart = String.join(",", citationKeys);

        int index = 0;
        int citTypeCode = citationTypeToInt(citationType);
        String name = BIB_CITATION + '_' + citTypeCode + '_' + citationKeysPart;
        while (usedNames.contains(name)) {
            name = BIB_CITATION + index + '_' + citTypeCode + '_' + citationKeysPart;
            index++;
        }
        return name;
    }

    /**
     * Parse a JabRef (reference) mark name.
     *
     * @return Optional.empty() on failure.
     *
     */
    public static Optional<ParsedMarkName> parseMarkName(String refMarkName) {

        Matcher citeMatcher = CITE_PATTERN.matcher(refMarkName);
        if (!citeMatcher.find()) {
            return Optional.empty();
        }

        List<String> keys = Arrays.asList(citeMatcher.group(3).split(","));
        String index = citeMatcher.group(1);
        int citTypeCode = Integer.parseInt(citeMatcher.group(2));
        CitationType citationType = citationTypeFromInt(citTypeCode);
        return (Optional.of(new Codec52.ParsedMarkName(index, citationType, keys)));
    }

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
        return (names.stream()
                     .filter(Codec52::isJabRefReferenceMarkName)
                     .collect(Collectors.toList()));
    }
}
