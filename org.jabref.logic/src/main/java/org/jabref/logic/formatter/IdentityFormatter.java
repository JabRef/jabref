package org.jabref.logic.formatter;

import java.util.Objects;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.cleanup.Formatter;

/**
 * It may seem useless, but is needed as a fallback option
 */
public class IdentityFormatter implements Formatter {

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

    @Override
    public int hashCode() {
        return defaultHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return defaultEquals(obj);
    }

}
