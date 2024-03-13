package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.types.StandardEntryType;

public class CffType implements LayoutFormatter {
    @Override
    public String format(String value) {
        return switch (StandardEntryType.valueOf(value)) {
            case Article, Conference -> "article";
            case Book -> "book";
            case Booklet -> "pamphlet";
            case InProceedings -> "conference-paper";
            case Proceedings -> "proceedings";
            case Misc -> "misc";
            case Manual -> "manual";
            case Software -> "software";
            case Report, TechReport -> "report";
            case Unpublished -> "unpublished";
            default -> "generic";
        };
    }
}

