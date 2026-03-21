package org.jabref.logic.openoffice;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.thibaultmeyer.cuid.CUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceMark {
    public static final String[] PREFIXES = {"JABREF_", "CID_"};
    public static final String IN_TEXT_MARKER = "IN_TEXT";

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceMark.class);

    private static final Pattern REFERENCE_MARK_FORMAT = Pattern.compile(
            "^(JABREF_[\\w-:—./–]+ CID_\\d+(?:, JABREF_[\\w-:—./–]+ CID_\\d+)*) (\\w+)(?: (" + IN_TEXT_MARKER + "))?$",
            Pattern.UNICODE_CHARACTER_CLASS);

    private static final Pattern ENTRY_PATTERN = Pattern.compile(
            "JABREF_([\\w-:—./–]+) CID_(\\d+)",
            Pattern.UNICODE_CHARACTER_CLASS);

    private final String name;
    private List<String> citationKeys;
    private List<Integer> citationNumbers;
    private String uniqueId;
    private boolean inText;

    /// - Single entry: `JABREF_{citationKey} CID_{citationNumber} {uniqueId}`
    /// - Group of entries: `JABREF_{citationKey1} CID_{citationNumber1}, JABREF_{citationKey2} CID_{citationNumber2}, ..., JABREF_{citationKeyN} CID_{citationNumberN} {uniqueId}`
    /// - Disallowed: `JABREF_{citationKey} CID_{citationNumber}` (no unique ID at the end)
    /// - Disallowed: `JABREF_{citationKey1} CID_{citationNumber1} JABREF_{citationKey2} CID_{citationNumber2} {uniqueId}` (no comma between entries)
    ///
    /// @param name The format
    public ReferenceMark(String name) {
        this.name = name;
        parse(name);
    }

    public ReferenceMark(String name, List<String> citationKeys, List<Integer> citationNumbers, String uniqueId, boolean inText) {
        this.name = name;
        this.citationKeys = citationKeys;
        this.citationNumbers = citationNumbers;
        this.uniqueId = uniqueId;
        this.inText = inText;
    }

    private void parse(String name) {
        Matcher matcher = REFERENCE_MARK_FORMAT.matcher(name);
        if (!matcher.matches()) {
            LOGGER.warn("CSLReferenceMark: name={} does not match pattern. Assuming random values", name);
            this.citationKeys = List.of(CUID.randomCUID2(8).toString());
            this.citationNumbers = List.of(0);
            this.uniqueId = this.citationKeys.getFirst();
            this.inText = false;
            return;
        }

        String entriesString = matcher.group(1).trim();
        this.uniqueId = matcher.group(2) != null ? matcher.group(2).trim() : CUID.randomCUID2(8).toString();
        this.inText = matcher.group(3) != null;

        this.citationKeys = new ArrayList<>();
        this.citationNumbers = new ArrayList<>();

        Matcher entryMatcher = ENTRY_PATTERN.matcher(entriesString);
        while (entryMatcher.find()) {
            this.citationKeys.add(entryMatcher.group(1));
            this.citationNumbers.add(Integer.parseInt(entryMatcher.group(2)));
        }

        if (this.citationKeys.isEmpty() || this.citationNumbers.isEmpty()) {
            LOGGER.warn("CSLReferenceMark: Failed to parse any entries from name={}. Assuming random values", name);
            this.citationKeys = List.of(CUID.randomCUID2(8).toString());
            this.citationNumbers = List.of(0);
            this.inText = false;
        }

        LOGGER.debug("CSLReferenceMark: citationKeys={} citationNumbers={} uniqueId={} inText={}", getCitationKeys(), getCitationNumbers(), getUniqueId(), isInText());
    }

    public String getName() {
        return name;
    }

    /// The BibTeX citation keys
    public List<String> getCitationKeys() {
        return citationKeys;
    }

    public List<Integer> getCitationNumbers() {
        return citationNumbers;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public boolean isInText() {
        return inText;
    }

    public static Optional<ReferenceMark> of(String name) {
        ReferenceMark mark = new ReferenceMark(name);
        return mark.citationKeys.isEmpty() ? Optional.empty() : Optional.of(mark);
    }
}
