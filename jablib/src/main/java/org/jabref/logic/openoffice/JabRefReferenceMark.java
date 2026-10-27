package org.jabref.logic.openoffice;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.openoffice.oocsltext.CSLCitationType;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record JabRefReferenceMark(
        String name,
        List<String> citationKeys,
        List<Integer> citationNumbers,
        String uniqueId,
        CSLCitationType citationType) implements ReferenceMark {
    public static final String[] PREFIXES = {"JABREF_", "CID_"};
    public static final String IN_TEXT_MARKER = "IN_TEXT";
    public static final String EMPTY_MARKER = "EMPTY";
    public static final String NORMAL_MARKER = "NORMAL";

    /// e.g. "JABREF_Smith_2020 CID_1 abcd1234 NORMAL"
    private static final Pattern REFERENCE_MARK_FORMAT = Pattern.compile(
            "^(JABREF_[\\w-:—./–]+ CID_\\d+(?:, JABREF_[\\w-:—./–]+ CID_\\d+)*) (\\w+)(?: (" + IN_TEXT_MARKER + "|" + EMPTY_MARKER + "|" + NORMAL_MARKER + "))?$",
            Pattern.UNICODE_CHARACTER_CLASS);

    private static final Pattern ENTRY_PATTERN = Pattern.compile(
            "JABREF_([\\w-:—./–]+) CID_(\\d+)",
            Pattern.UNICODE_CHARACTER_CLASS);

    public static boolean isJabRefReferenceMarkName(String name) {
        return REFERENCE_MARK_FORMAT.matcher(name).matches();
    }

    public static String buildReferenceMarkName(List<String> citationKeys, List<Integer> citationNumbers, String uniqueId, CSLCitationType citationType) {
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 0; i < citationKeys.size(); i++) {
            if (i > 0) {
                nameBuilder.append(", ");
            }
            nameBuilder.append(PREFIXES[0]).append(citationKeys.get(i))
                       .append(" ").append(PREFIXES[1]).append(citationNumbers.get(i));
        }
        nameBuilder.append(" ").append(uniqueId);

        // Embed citation nature into reference mark
        switch (citationType) {
            case IN_TEXT ->
                    nameBuilder.append(" ").append(IN_TEXT_MARKER);
            case EMPTY ->
                    nameBuilder.append(" ").append(EMPTY_MARKER);
            case NORMAL ->
                    nameBuilder.append(" ").append(NORMAL_MARKER);
        }
        return nameBuilder.toString();
    }

    public static JabRefReferenceMark buildReferenceMark(List<String> citationKeys,
                                                         List<Integer> citationNumbers,
                                                         CSLCitationType citationType) {
        String uniqueId = ReferenceMark.generateRandomCUID(8);
        return new JabRefReferenceMark(
                buildReferenceMarkName(citationKeys, citationNumbers, uniqueId, citationType),
                citationKeys,
                citationNumbers,
                uniqueId,
                citationType);
    }

    public static Optional<JabRefReferenceMark> parse(String name) {
        Matcher matcher = REFERENCE_MARK_FORMAT.matcher(name);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String entriesString = matcher.group(1).trim();
        String uniqueId = matcher.group(2).trim();

        String citationTypeMarker = matcher.group(3);
        if (citationTypeMarker == null) {
            citationTypeMarker = NORMAL_MARKER;
        }
        CSLCitationType citationType = switch (citationTypeMarker) {
            case IN_TEXT_MARKER ->
                    CSLCitationType.IN_TEXT;
            case EMPTY_MARKER ->
                    CSLCitationType.EMPTY;
            default ->
                    CSLCitationType.NORMAL;
        };

        List<String> citationKeys = new ArrayList<>();
        List<Integer> citationNumbers = new ArrayList<>();

        Matcher entryMatcher = ENTRY_PATTERN.matcher(entriesString);
        while (entryMatcher.find()) {
            citationKeys.add(entryMatcher.group(1));
            citationNumbers.add(Integer.parseInt(entryMatcher.group(2)));
        }

        if (citationKeys.isEmpty() || citationNumbers.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new JabRefReferenceMark(
                name,
                List.copyOf(citationKeys),
                List.copyOf(citationNumbers),
                uniqueId,
                citationType));
    }
}
