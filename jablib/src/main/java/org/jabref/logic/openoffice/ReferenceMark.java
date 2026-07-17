package org.jabref.logic.openoffice;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.openoffice.oocsltext.CSLCitationType;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public sealed interface ReferenceMark permits JabRefReferenceMark, ZoteroReferenceMark {
    Logger LOGGER = LoggerFactory.getLogger(ReferenceMark.class);

    String name();

    List<String> citationKeys();

    List<Integer> citationNumbers();

    String uniqueId();

    CSLCitationType citationType();

    default String getName() {
        return name();
    }

    /// The BibTeX citation keys
    default List<String> getCitationKeys() {
        return citationKeys();
    }

    default List<Integer> getCitationNumbers() {
        return citationNumbers();
    }

    default String getUniqueId() {
        return uniqueId();
    }

    default CSLCitationType getCitationType() {
        return citationType();
    }

    static boolean isReferenceMarkName(String name) {
        return isJabRefReferenceMarkName(name) || isZoteroReferenceMarkName(name);
    }

    static boolean isJabRefReferenceMarkName(String name) {
        return JabRefReferenceMark.isJabRefReferenceMarkName(name);
    }

    static boolean isZoteroReferenceMarkName(String name) {
        return ZoteroReferenceMark.isZoteroReferenceMarkName(name);
    }

    static Optional<ReferenceMark> parse(String name) {
        if (isZoteroReferenceMarkName(name)) {
            Optional<ZoteroReferenceMark> referenceMark = ZoteroReferenceMark.parse(name);
            if (referenceMark.isEmpty()) {
                LOGGER.warn("Can not parse Zotero ReferenceMark: name={}", name);
                return Optional.empty();
            }
            return Optional.of(referenceMark.get());
        } else {
            Optional<JabRefReferenceMark> referenceMark = JabRefReferenceMark.parse(name);
            if (referenceMark.isEmpty()) {
                LOGGER.warn("Can not parse JabRef ReferenceMark: name={}", name);
                return Optional.empty();
            }
            return Optional.of(referenceMark.get());
        }
    }

    static ReferenceMark of(String name, List<String> citationKeys, List<Integer> citationNumbers, String uniqueId, CSLCitationType citationType) {
        if (isZoteroReferenceMarkName(name)) {
            return new ZoteroReferenceMark(name, citationKeys, citationNumbers, uniqueId, citationType);
        }
        return new JabRefReferenceMark(name, citationKeys, citationNumbers, uniqueId, citationType);
    }
}
