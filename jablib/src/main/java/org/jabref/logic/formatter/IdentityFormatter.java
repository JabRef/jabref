package org.jabref.logic.formatter;

import java.util.Objects;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

/**
 * It may seem useless, but is needed as a fallback option
 */
public class IdentityFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("Identity");
    }

    @Override
    public String getKey() {
        return "identity";
    }

    @Override
    public String format(String value) {
        Objects.requireNonNull(value);
        return value;
    }

    @Override
    public String getDescription() {
        return Localization.lang("Does nothing.");
    }

    @Override
    public String getExampleInput() {
        return "JabRef";
    }
}
