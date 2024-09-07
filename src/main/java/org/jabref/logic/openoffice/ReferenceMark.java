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

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceMark.class);

    private static final Pattern REFERENCE_MARK_FORMAT = Pattern.compile("^((?:JABREF_\\w+ CID_\\w+(?:,\\s*)?)+)(\\s*\\w+)?$");
    private static final Pattern ENTRY_PATTERN = Pattern.compile("JABREF_(\\w+) CID_(\\w+)");

    private final String name;
    private List<String> citationKeys;
    private List<Integer> citationNumbers;
    private String uniqueId;

    public ReferenceMark(String name) {
        this.name = name;
        parse(name);
    }

    public ReferenceMark(String name, List<String> citationKeys, List<Integer> citationNumbers, String uniqueId) {
        this.name = name;
        this.citationKeys = citationKeys;
        this.citationNumbers = citationNumbers;
        this.uniqueId = uniqueId;
    }

    private void parse(String name) {
        Matcher matcher = REFERENCE_MARK_FORMAT.matcher(name);
        if (!matcher.matches()) {
            LOGGER.warn("CSLReferenceMark: name={} does not match pattern. Unable to parse.", name);
            this.citationKeys = new ArrayList<>();
            this.citationNumbers = new ArrayList<>();
            this.uniqueId = CUID.randomCUID2(8).toString();
            return;
        }

        String entriesString = matcher.group(1).trim();
        this.uniqueId = matcher.group(2) != null ? matcher.group(2).trim() : CUID.randomCUID2(8).toString();

        this.citationKeys = new ArrayList<>();
        this.citationNumbers = new ArrayList<>();

        Matcher entryMatcher = ENTRY_PATTERN.matcher(entriesString);
        while (entryMatcher.find()) {
            this.citationKeys.add(entryMatcher.group(1));
            this.citationNumbers.add(Integer.parseInt(entryMatcher.group(2)));
        }

        if (this.citationKeys.isEmpty() || this.citationNumbers.isEmpty()) {
            LOGGER.warn("CSLReferenceMark: Failed to parse any entries from name={}. Reference mark is empty.", name);
        }

        LOGGER.debug("CSLReferenceMark: citationKeys={} citationNumbers={} uniqueId={}", getCitationKeys(), getCitationNumbers(), getUniqueId());
    }

    public String getName() {
        return name;
    }

    public List<String> getCitationKeys() {
        return citationKeys;
    }

    public List<Integer> getCitationNumbers() {
        return citationNumbers;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public static Optional<ReferenceMark> of(String name) {
        ReferenceMark mark = new ReferenceMark(name);
        return mark.citationKeys.isEmpty() ? Optional.empty() : Optional.of(mark);
    }
}
