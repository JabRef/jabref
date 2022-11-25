package org.jabref.logic.formatter.bibtexfields;

import java.util.Objects;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

public class AddBracesFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("Add enclosing braces");
    }

    @Override
    public String getKey() {
        return "add_braces";
    }

    @Override
    public String format(String value) {
        Objects.requireNonNull(value);

        if ((value.length() >= 2) && (value.charAt(0) != '{') && (value.charAt(value.length() - 1) != '}')) {
            // Title does not start with { and does not end with }, then this formatter can be applied
            return "{" + value + "}";
        } else {
            return value;
        }
    }

    @Override
    public String getDescription() {
        return Localization.lang("Add braces encapsulating the complete field content.");
    }

    @Override
    public String getExampleInput() {
        return "In CDMA";
    }
}
