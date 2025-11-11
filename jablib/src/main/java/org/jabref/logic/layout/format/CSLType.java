package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.types.StandardEntryType;

public class CSLType implements LayoutFormatter {

    @Override
    public String format(String value) {
        return switch (StandardEntryType.valueOf(value)) {
            case Article -> "article";
            case Book -> "book";
            case Conference -> "paper-conference";
            case Report, TechReport -> "report";
            case Thesis, MastersThesis, PhdThesis -> "thesis";
            case WWW, Online -> "webpage";

            default -> "no-type";
        };
    }
}
