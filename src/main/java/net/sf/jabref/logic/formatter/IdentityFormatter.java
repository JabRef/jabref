package net.sf.jabref.logic.formatter;

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
        return value;
    }
}
