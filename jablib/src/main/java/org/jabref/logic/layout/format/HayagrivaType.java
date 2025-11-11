package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.types.StandardEntryType;

public class HayagrivaType implements LayoutFormatter {

    @Override
    public String format(String value) {
        return switch (StandardEntryType.valueOf(value)) {
            case Article, Conference -> "article";
            case Book -> "book";
            case InBook -> "chapter";
            case Report, TechReport -> "report";
            case Thesis, MastersThesis, PhdThesis -> "thesis";
            case WWW, Online -> "web";
            case Proceedings -> "proceedings";
            case Reference -> "reference";

            default -> "Misc";
        };
    }
}
