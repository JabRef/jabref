package net.sf.jabref.logic.formatter.bibtexfields;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;

import java.util.Objects;

/**
 * Removes all matching braces around the string.
 */
public class RemoveBracesFormatter implements Formatter {

    @Override
    public String getName() {
        return Localization.lang("Remove enclosing braces");
    }

    @Override
    public String getKey() {
        return "remove_braces";
    }

    @Override
    public String format(String value) {
        Objects.requireNonNull(value);

        String formatted = value;
        while ((formatted.length() >= 2) && (formatted.charAt(0) == '{') && (formatted.charAt(formatted.length() - 1)
                == '}')) {
            String trimmed = formatted.substring(1, formatted.length() - 1);

            // It could be that the removed braces were not matching
            // For example, "{A} test {B}" results in "A} test {B"
            // In this case, trimmed has a closing } without an opening { before that
            if(hasNegativeBraceCount(trimmed)) {
                return formatted;
            } else {
                formatted = trimmed;
            }
        }
        return formatted;
    }

    @Override
    public String getDescription() {
        return Localization.lang("Removes braces encapsulating the complete field content.");
    }

    @Override
    public String getExampleInput() {
        return "{In CDMA}";
    }

    /**
     * Check if a string at any point has had more ending } braces than opening { ones.
     * Will e.g. return true for the string "DNA} blahblal {EPA"
     *
     * @param value The string to check.
     * @return true if at any index the brace count is negative.
     */
    private boolean hasNegativeBraceCount(String value) {
        int braceCount = 0;
        for (int index = 0; index < value.length(); index++) {
            char charAtIndex = value.charAt(index);
            if (charAtIndex == '{') {
                braceCount++;
            } else if (charAtIndex == '}') {
                braceCount--;
            }
            if (braceCount < 0) {
                return true;
            }
        }
        return false;
    }
}
