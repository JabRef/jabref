package org.jabref.http.server.cayw.format;

import java.util.Locale;

import org.jabref.http.server.cayw.CAYWQueryParams;

import org.jvnet.hk2.annotations.Service;

@Service
public class FormatterService {

    public FormatterService() {
    }

    public CAYWFormatter getFormatter(CAYWQueryParams queryParams) {
        String format = queryParams.getFormat().toLowerCase(Locale.ROOT);

        switch (format) {
            case "natbib":
            case "latex":
            case "cite":
                return new NatbibFormatter("cite");
            case "citep":
                return new NatbibFormatter("citep");
            case "citet":
                return new NatbibFormatter("citet");
            case "mmd":
                return new MMDFormatter();
            case "pandoc":
                return new PandocFormatter();
            case "simple-json":
                return new SimpleJsonFormatter();
            case "typst":
                return new TypstFormatter();
            case "biblatex":
            default:
                return new BibLatexFormatter("autocite");
        }
    }
}
