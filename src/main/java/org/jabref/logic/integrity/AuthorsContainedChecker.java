package org.jabref.logic.integrity;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;

public class AuthorsContainedChecker implements ValueChecker {

    // AUTHOR_CHECK is used to identify whether there is a pattern that matches '{author}' which represents how an Author is included in a booktitle
    private static final Predicate<String> AUTHOR_CHECK = Pattern.compile("\\{([^}]+)\\}").asPredicate();

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        if (AUTHOR_CHECK.test(value.trim())) {
            return Optional.of(Localization.lang("Author is contained in the booktitle"));
        }

        return Optional.empty();
    }
}
