package org.jabref.logic.formatter.bibtexfields;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

import com.google.common.base.Strings;

/**
 * This class includes sensible defaults for consistent formatting of BibTeX page numbers.
 *
 * From BibTeX manual:
 * One or more page numbers or range of numbers, such as 42--111 or 7,41,73--97 or 43+
 * (the '+' in this last example indicates pages following that don't form a simple range).
 * To make it easier to maintain Scribe-compatible databases, the standard styles convert
 * a single dash (as in 7-33) to the double dash used in TEX to denote number ranges (as in 7--33).
 */
public class NormalizePagesFormatter extends Formatter {

    // "startpage" and "endpage" are named groups. See http://stackoverflow.com/a/415635/873282 for a documentation
    private static final Pattern PAGES_DETECT_PATTERN = Pattern.compile("\\A(?<startpage>(\\d+:)?\\d+)(?:-{1,2}(?<endpage>(\\d+:)?\\d+))?\\Z");

    private static final String REJECT_LITERALS = "[^a-zA-Z0-9,\\-\\+,:]";
    private static final String PAGES_REPLACE_PATTERN = "${startpage}--${endpage}";
    private static final String SINGLE_PAGE_REPLACE_PATTERN = "$1";

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
     *     1-2 -> 1--2
     *     1,2,3 -> 1,2,3
     *     {1}-{2} -> 1--2
     *     43+ -> 43+
     *     Invalid -> Invalid
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
        // remove unwanted literals incl. whitespace
        cleanValue = cleanValue.replaceAll("\u2013|\u2014", "-").replaceAll(REJECT_LITERALS, "");
        // try to find pages pattern
        Matcher matcher = PAGES_DETECT_PATTERN.matcher(cleanValue);
        if (matcher.matches()) {
            // replace
            if (Strings.isNullOrEmpty(matcher.group("endpage"))) {
                return matcher.replaceFirst(SINGLE_PAGE_REPLACE_PATTERN);
            } else {
                return matcher.replaceFirst(PAGES_REPLACE_PATTERN);
            }
        }
        // no replacement
        return value;
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
