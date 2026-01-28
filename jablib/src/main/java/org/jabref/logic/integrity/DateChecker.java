package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.Date;

public class DateChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        Optional<Date> parsedDate = Date.parse(value);
        if (parsedDate.isEmpty()) {
            return Optional.of(Localization.lang("incorrect format"));
        }

        return Optional.empty();
    }
}
