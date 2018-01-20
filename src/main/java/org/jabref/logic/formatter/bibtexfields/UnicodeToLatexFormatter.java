package org.jabref.logic.formatter.bibtexfields;

import java.util.Map;
import java.util.Objects;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.util.strings.HTMLUnicodeConversionMaps;
import org.jabref.model.cleanup.Formatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnicodeToLatexFormatter implements LayoutFormatter, Formatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnicodeToLatexFormatter.class);

    @Override
    public String format(String text) {
        String result = Objects.requireNonNull(text);

        if (result.isEmpty()) {
            return result;
        }

        // Standard symbols
        for (Map.Entry<String, String> unicodeLatexPair : HTMLUnicodeConversionMaps.UNICODE_LATEX_CONVERSION_MAP
                .entrySet()) {
            result = result.replace(unicodeLatexPair.getKey(), unicodeLatexPair.getValue());
        }

        // Combining accents
        StringBuilder sb = new StringBuilder();
        boolean consumed = false;
        for (int i = 0; i <= (result.length() - 2); i++) {
            if (!consumed && (i < (result.length() - 1))) {
                int cpCurrent = result.codePointAt(i);
                Integer cpNext = result.codePointAt(i + 1);
                String code = HTMLUnicodeConversionMaps.ESCAPED_ACCENTS.get(cpNext);
                if (code == null) {
                    sb.append((char) cpCurrent);
                } else {
                    sb.append("{\\").append(code).append('{').append((char) cpCurrent).append("}}");
                    consumed = true;
                }
            } else {
                consumed = false;
            }
        }
        if (!consumed) {
            sb.append((char) result.codePointAt(result.length() - 1));
        }
        result = sb.toString();

        // Check if any symbols is not converted
        for (int i = 0; i <= (result.length() - 1); i++) {
            int cp = result.codePointAt(i);
            if (cp >= 129) {
                LOGGER.warn("Unicode character not converted: " + cp);
            }
        }
        return result;
    }

    @Override
    public String getDescription() {
        return Localization.lang("Converts Unicode characters to LaTeX encoding.");
    }

    @Override
    public String getExampleInput() {
        return "Mönch";
    }

    @Override
    public String getName() {
        return Localization.lang("Unicode to LaTeX");
    }

    @Override
    public String getKey() {
        return "unicode_to_latex";
    }
}
