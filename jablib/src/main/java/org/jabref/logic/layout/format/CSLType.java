package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.entry.types.StandardEntryType;

public class CSLType implements LayoutFormatter {

    @Override
    public String format(String value) {
        return switch (EntryTypeFactory.parse(value)) {
            case StandardEntryType.Article ->
                    "article";
            case StandardEntryType.Book ->
                    "book";
            case StandardEntryType.Conference ->
                    "paper-conference";
            case StandardEntryType.Report,
                 StandardEntryType.TechReport ->
                    "report";
            case StandardEntryType.Thesis,
                 StandardEntryType.MastersThesis,
                 StandardEntryType.PhdThesis ->
                    "thesis";
            case StandardEntryType.WWW,
                 StandardEntryType.Online ->
                    "webpage";

            default ->
                    "no-type";
        };
    }
}
