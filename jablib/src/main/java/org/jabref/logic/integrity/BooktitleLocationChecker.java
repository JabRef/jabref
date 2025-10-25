package org.jabref.logic.integrity;

import java.util.Optional;
import java.util.Set;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.LocationDetector;
import org.jabref.model.strings.StringUtil;

public class BooktitleLocationChecker implements ValueChecker {
    private final LocationDetector locationDetector;

    public BooktitleLocationChecker(LocationDetector detector) {
        this.locationDetector = detector;
    }

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        Set<String> locations = locationDetector.extractLocations(value);

        if (!locations.isEmpty()) {
            return Optional.of(Localization.lang("Location(s) found in booktitle"));
        }

        return Optional.empty();
    }
}
