package org.jabref.logic.formatter.bibtexfields;

import java.util.Objects;
import java.util.regex.Pattern;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.formatter.casechanger.UnprotectTermsFormatter;
import org.jabref.logic.l10n.Localization;

/**
 * This class includes sensible defaults for consistent formatting of BibTeX page numbers.
 * <p>
 * From BibTeX manual:
 * One or more page numbers or range of numbers, such as 42--111 or 7,41,73--97 or 43+
 * (the '+' in this last example indicates pages following that don't form a simple range).
 * To make it easier to maintain Scribe-compatible databases, the standard styles convert
 * a single dash (as in 7-33) to the double dash used in TEX to denote number ranges (as in 7--33).
 * <p>
 * Examples:
 *
 * <ul>
 *     <li><code>1-2</code> <code>1--2</code></li>
 *     <li><code>1---2</code> <code>1--2</code></li>
 * </ul>
 */
public class NormalizePagesFormatter extends Formatter {

    private static final Pattern EM_EN_DASH_PATTERN = Pattern.compile("\u2013|\u2014");

    private final Formatter unprotectTermsFormatter = new UnprotectTermsFormatter();

    @Override
    public String getName() {
        return Localization.lang("Normalize page numbers");
    }

    @Override
    public String getKey() {
        return "normalize_page_numbers";
    }

    /**
     * Format page numbers, separated either by commas or double-hyphens.
     * Converts the range number format of the <code>pages</code> field to page_number--page_number.
     * Removes unwanted literals except letters, numbers and -+ signs.
     * Keeps the existing String if the resulting field does not match the expected Regex.
     *
     * <example>
     * 1-2 -> 1--2
     * 1,2,3 -> 1,2,3
     * {1}-{2} -> 1--2
     * 43+ -> 43+
     * Invalid -> Invalid
     * </example>
     */
    @Override
    public String format(String value) {
        Objects.requireNonNull(value);

        if (value.isEmpty()) {
            // nothing to do
            return value;
        }

        // Remove pages prefix
        String cleanValue = value.replace("pp.", "").replace("p.", "");
        // remove unwanted literals including en dash, em dash, and whitespace
        value = EM_EN_DASH_PATTERN.matcher(cleanValue).replaceAll("-")
                                  .replaceAll("[ ]*[-]+[ ]*", "--");
        return unprotectTermsFormatter.format(value.trim());
    }

    @Override
    public String getDescription() {
        return Localization.lang("Normalize pages to BibTeX standard.");
    }

    @Override
    public String getExampleInput() {
        return "1 - 2";
    }
}
