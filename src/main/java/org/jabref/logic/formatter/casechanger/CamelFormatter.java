package org.jabref.logic.formatter.casechanger;

import java.util.stream.Collectors;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

public class CamelFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("Camel case");
    }

    @Override
    public String getKey() {
        return "camel_case";
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
                "Returns capitalized and concatenated title.");
    }

    @Override
    public String getExampleInput() {
        return "this is example input";
    }
}
