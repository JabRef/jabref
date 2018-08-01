package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;

public class UrlChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        if (!value.contains("://")) {
            return Optional.of(Localization.lang("should contain a protocol") + ": http[s]://, file://, ftp://, ...");
        }

        return Optional.empty();
    }
}
