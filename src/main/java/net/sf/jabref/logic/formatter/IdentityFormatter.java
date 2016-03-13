package net.sf.jabref.logic.formatter;

import net.sf.jabref.logic.l10n.Localization;

import java.util.Objects;

/**
 * It may seem useless, but is needed as a fallback option
 */
public class IdentityFormatter implements Formatter {

    @Override
    public String getName() {
        return "IdentityFormatter";
    }

    @Override
    public String getKey() {
        return getName();
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
}
