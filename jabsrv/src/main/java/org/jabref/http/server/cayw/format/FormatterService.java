package org.jabref.http.server.cayw.format;

import java.util.Locale;

import org.jabref.http.server.cayw.CAYWQueryParams;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.jvnet.hk2.annotations.Service;

@Service
public class FormatterService {

    public CAYWFormatter getFormatter(CAYWQueryParams queryParams, CliPreferences preferences, BibDatabaseContext databaseContext) {
        String format = queryParams.getFormat().toLowerCase(Locale.ROOT);

        return switch (format) {
            case "natbib",
                 "latex",
                 "cite" ->
                    new NatbibFormatter("cite");
            case "citep" ->
                    new NatbibFormatter("citep");
            case "citet" ->
                    new NatbibFormatter("citet");
            case "mmd" ->
                    new MMDFormatter();
            case "pandoc" ->
                    new PandocFormatter();
            case "simple-json" ->
                    new SimpleJsonFormatter();
            case "typst" ->
                    new TypstFormatter();
            case "formatted-bibliography" ->
                    new FormattedBibliographyFormatter(preferences.getPreviewPreferences(), databaseContext);
            default ->
                    new BibLatexFormatter("autocite");
        };
    }
}
