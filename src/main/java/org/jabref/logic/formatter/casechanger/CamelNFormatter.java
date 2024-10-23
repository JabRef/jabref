package org.jabref.logic.formatter.casechanger;

import java.util.stream.Collectors;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

public class CamelNFormatter extends Formatter {
    private final int length;

    public CamelNFormatter(int length) {
        this.length = length;
    }

    @Override
    public String getName() {
        return Localization.lang("Cameln");
    }

    @Override
    public String getKey() {
        return "cameln";
    }

    @Override
    public String format(String input) {
        Title title = new Title(input);

        return title.getWords().stream()
                    .map(Word -> {
                        Word.toUpperFirst();
                        return Word.toString();
                    })
                    .limit(length)
                    .collect(Collectors.joining(""));
    }

    @Override
    public String getDescription() {
        return Localization.lang(
                "RETURNS CAPITALIZED AND CONCATENATED TITLE.");
    }

    @Override
    public String getExampleInput() {
        return "N";
    }
}
