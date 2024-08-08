package org.jabref.logic.openoffice;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.thibaultmeyer.cuid.CUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceMark {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceMark.class);

    private static final Pattern REFERENCE_MARK_FORMAT = Pattern.compile("^JABREF_(\\w+) CID_(\\w+) (\\w+)$");
    private final String name;

    private String citationKey;
    private Integer citationNumber;
    private String uniqueId;

    /**
     * @param name Format: <code>JABREF_{citationKey} CID_{citationNumber} {uniqueId}</code>
     */
    public ReferenceMark(String name) {
        this.name = name;

        Matcher matcher = getMatcher(name);
        if (!matcher.matches()) {
            LOGGER.warn("CSLReferenceMark: name={} does not match pattern. Assuming random values", name);
            this.citationKey = CUID.randomCUID2(8).toString();
            this.citationNumber = 0;
            this.uniqueId = this.citationKey;
            return;
        }

        this.citationKey = matcher.group(1);
        this.citationNumber = Integer.parseInt(matcher.group(2));
        this.uniqueId = matcher.group(3);

        LOGGER.debug("CSLReferenceMark: citationKey={} citationNumber={} uniqueId={}", getCitationKey(), getCitationNumber(), getUniqueId());
    }

    public ReferenceMark(String name, String citationKey, Integer citationNumber, String uniqueId) {
        this.name = name;
        this.citationKey = citationKey;
        this.citationNumber = citationNumber;
        this.uniqueId = uniqueId;
    }

    private ReferenceMark(String name, String citationKey, String citationNumber, String uniqueId) {
        this(name, citationKey, Integer.parseInt(citationNumber), uniqueId);
    }

    private static Matcher getMatcher(String name) {
        return REFERENCE_MARK_FORMAT.matcher(name);
    }

    public String getName() {
        return name;
    }

    /**
     * The BibTeX citation key
     */
    public String getCitationKey() {
        return citationKey;
    }

    public Integer getCitationNumber() {
        return citationNumber;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public static Optional<ReferenceMark> of(String name) {
        Matcher matcher = getMatcher(name);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        return Optional.of(new ReferenceMark(name, matcher.group(1), matcher.group(2), matcher.group(3)));
    }
}
