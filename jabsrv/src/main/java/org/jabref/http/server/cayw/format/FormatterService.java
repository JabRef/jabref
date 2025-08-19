package org.jabref.http.server.cayw.format;

import java.util.HashMap;
import java.util.Map;

import org.jabref.http.server.cayw.CAYWQueryParams;

import org.jvnet.hk2.annotations.Service;

@Service
public class FormatterService {
    private static final String DEFAULT_FORMATTER = "biblatex";
    private final Map<String, CAYWFormatter> formatters;

    public FormatterService() {
        this.formatters = new HashMap<>();
        registerFormatter(new SimpleJsonFormatter());
        registerFormatter(new BibLatexFormatter());
        registerFormatter(new NatbibFormatter());
        registerFormatter(new LatexFormatter());
        registerFormatter(new CitepFormatter());
        registerFormatter(new MmdFormatter());
        registerFormatter(new PandocFormatter());
        registerFormatter(new TypstFormatter());
    }

    public void registerFormatter(CAYWFormatter formatter) {
        for (String name : formatter.getFormatNames()) {
            formatters.putIfAbsent(name.toLowerCase(), formatter);
        }
    }

    public CAYWFormatter getFormatter(CAYWQueryParams queryParams) {
        String format = queryParams.getFormat();
        if (format == null) {
            return formatters.get(DEFAULT_FORMATTER);
        }
        return formatters.getOrDefault(format.toLowerCase(), formatters.get(DEFAULT_FORMATTER));
    }
}
