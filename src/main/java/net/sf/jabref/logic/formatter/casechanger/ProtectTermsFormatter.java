package net.sf.jabref.logic.formatter.casechanger;

import java.util.List;
import java.util.Objects;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.protectedterms.ProtectedTermsLoader;
import net.sf.jabref.logic.util.strings.StringLengthComparator;
import net.sf.jabref.model.cleanup.Formatter;

public class ProtectTermsFormatter implements Formatter {

    private static ProtectedTermsLoader protectedTermsLoader;

    /**
     * @deprecated use ProtectTermsFormatter(ProtectedTermsLoader) instead
     */
    @Deprecated
    public ProtectTermsFormatter() {
    }

    public ProtectTermsFormatter(ProtectedTermsLoader protectedTermsLoader) {
        ProtectTermsFormatter.protectedTermsLoader = protectedTermsLoader;
    }

    /**
     * This must be called from JabRefMain
     *
     * @deprecated use ProtectTermsFormatter(ProtectedTermsLoader) instead
     */
    @Deprecated
    public static void setProtectedTermsLoader(ProtectedTermsLoader loader) {
        protectedTermsLoader = loader;
    }

    private String format(String text, List<String> listOfWords) {
        String result = text;
        listOfWords.sort(new StringLengthComparator());
        // For each word in the list
        for (String listOfWord : listOfWords) {
            // Add {} if the character before is a space, -, /, (, [, ", or } or if it is at the start of the string but not if it is followed by a }
            result = result.replaceAll("(^|[- /\\[(}\"])" + listOfWord + "($|[^a-zA-Z}])", "$1\\{" + listOfWord + "\\}$2");
        }
        return result;
    }

    @Override
    public String format(String text) {
        Objects.requireNonNull(text);
        if (text.isEmpty()) {
            return text;
        }
        Objects.requireNonNull(ProtectTermsFormatter.protectedTermsLoader);
        return this.format(text, ProtectTermsFormatter.protectedTermsLoader.getProtectedTerms());
    }

    @Override
    public String getDescription() {
        return Localization.lang(
                "Adds {} brackets around acronyms, month names and countries to preserve their case.");
    }

    @Override
    public String getExampleInput() {
        return "In CDMA";
    }

    @Override
    public String getName() {
        return Localization.lang("Protect terms");
    }

    @Override
    public String getKey() {
        return "protect_terms";
    }

}
