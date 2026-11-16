package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.types.BiblatexNonStandardEntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.entry.types.StandardEntryType;

public class HayagrivaType implements LayoutFormatter {

    @Override
    public String format(String value) {
        return switch (EntryTypeFactory.parse(value)) {
            case StandardEntryType.Article,
                 StandardEntryType.Conference ->
                    "article";
            case StandardEntryType.Book ->
                    "book";
            case StandardEntryType.InBook ->
                    "chapter";
            case StandardEntryType.Report,
                 StandardEntryType.TechReport ->
                    "report";
            case StandardEntryType.Thesis,
                 StandardEntryType.MastersThesis,
                 StandardEntryType.PhdThesis ->
                    "thesis";
            case StandardEntryType.WWW,
                 StandardEntryType.Online ->
                    "web";
            case StandardEntryType.Proceedings ->
                    "proceedings";
            case StandardEntryType.Reference ->
                    "reference";
            case BiblatexNonStandardEntryType.Performance ->
                    "performance";

            default ->
                    "Misc";
        };
    }
}
