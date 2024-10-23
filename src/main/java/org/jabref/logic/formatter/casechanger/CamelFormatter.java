package org.jabref.logic.formatter.casechanger;

import java.util.stream.Collectors;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

/**
 * Converts all characters of the given string to upper case, but does not change words starting with "{"
 */
public class CamelFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("CAMEL");
    }

    @Override
    public String getKey() {
        return "camel";
    }

    @Override
    public String format(String input) {
        Title title = new Title(input);

        return title.getWords().stream()
                    .map(Word -> {
                        Word.toUpperFirst();
                        return Word.toString();
                    })
                    .collect(Collectors.joining(""));
    }

    @Override
    public String getDescription() {
        return Localization.lang(
                "RETURNS CAPITALIZED AND CONCATENATED TITLE.");
    }

    @Override
    public String getExampleInput() {
        return "";
    }
}
