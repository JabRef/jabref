package org.jabref.logic.formatter.casechanger;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.cleanup.ProtectedTermsLoaderAware;
import org.jabref.logic.formatter.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;
import org.jabref.logic.util.strings.StringLengthComparator;

import org.jspecify.annotations.NullMarked;

/// Adds {} brackets around acronyms, month names and countries to preserve their case.
/// Related formatter: [org.jabref.logic.formatter.bibtexfields.RemoveEnclosingBracesFormatter]
@NullMarked
public class ProtectTermsFormatter extends Formatter implements ProtectedTermsLoaderAware {

    private final Optional<ProtectedTermsLoader> protectedTermsLoader;

    /// Unbound: uses the default built-in protected terms lists lazily when formatting.
    public ProtectTermsFormatter() {
        this.protectedTermsLoader = Optional.empty();
    }

    public ProtectTermsFormatter(ProtectedTermsLoader protectedTermsLoader) {
        this.protectedTermsLoader = Optional.of(protectedTermsLoader);
    }

    @Override
    public Formatter withProtectedTermsLoader(ProtectedTermsLoader protectedTermsLoader) {
        return new ProtectTermsFormatter(protectedTermsLoader);
    }

    private String format(String text, List<String> listOfWords) {
        String result = text;
        // Treat longer terms first to avoid substring issues
        listOfWords.sort(new StringLengthComparator());
        // For each word in the list
        for (String listOfWord : listOfWords) {
            // Add {} if the character before is a space, -, /, (, [, ", or } or if it is at the start of the string but not if it is followed by a }
            result = result.replaceAll("(^|[- /\\[(}\"])" + listOfWord + "($|[^a-zA-Z}])", "$1\\{" + listOfWord + "\\}$2");
        }
        // Remove the empty brackets
        result = result.replace("{}", "");
        return result;
    }

    @Override
    public String format(String text) {
        if (text.isEmpty()) {
            return text;
        }
        ProtectedTermsLoader loader = protectedTermsLoader
                .orElseGet(() -> new ProtectedTermsLoader(ProtectedTermsPreferences.getDefault()));
        return this.format(text, loader.getProtectedTerms());
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
