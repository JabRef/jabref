package org.jabref.logic.formatter.casechanger;

import java.util.Objects;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

/**
 * Remove {} braces around words in case they appear balanced
 *
 * Related formatter: {@link ProtectTermsFormatter}
 */
public class UnprotectTermsFormatter extends Formatter {

    @Override
    public String format(String text) {
        // similar implementation at {@link org.jabref.logic.formatter.bibtexfields.RemoveBracesFormatter.hasNegativeBraceCount}
        Objects.requireNonNull(text);
        if (text.isEmpty()) {
            return text;
        }
        StringBuilder result = new StringBuilder();
        int level = 0;
        int index = 0;
        do {
            char charAtIndex = text.charAt(index);
            if (charAtIndex == '{') {
                level++;
            } else if (charAtIndex == '}') {
                level--;
            } else {
                result.append(charAtIndex);
            }
            index++;
        } while (index < text.length() && level >= 0);
        if (level != 0) {
            // in case of unbalanced braces, the original text is returned unmodified
            return text;
        }
        return result.toString();
    }

    @Override
    public String getDescription() {
        return Localization.lang(
                "Removes all balanced {} braces around words.");
    }

    @Override
    public String getExampleInput() {
        return "{In} {CDMA}";
    }

    @Override
    public String getName() {
        return Localization.lang("Unprotect terms");
    }

    @Override
    public String getKey() {
        return "unprotect_terms";
    }
}
