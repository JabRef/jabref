package org.jabref.logic.layout.format;

import java.util.Map;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.types.StandardEntryType;

public class CSLType implements LayoutFormatter {

    @Override
    public String format(String value) {
        Map<String, String> map = Map.of(StandardEntryType.Article.getDisplayName(), "article",
                StandardEntryType.Book.getDisplayName(), "book",
                StandardEntryType.Conference.getDisplayName(), "paper-conference",
                StandardEntryType.Report.getDisplayName(), "report",
                StandardEntryType.Thesis.getDisplayName(), "thesis",
                StandardEntryType.MastersThesis.getDisplayName(), "thesis",
                StandardEntryType.PhdThesis.getDisplayName(), "thesis",
                StandardEntryType.WWW.getDisplayName(), "webpage",
                StandardEntryType.TechReport.getDisplayName(), "report",
                StandardEntryType.Online.getDisplayName(), "webpage");

        return map.getOrDefault(value, "no-type");
    }
}
