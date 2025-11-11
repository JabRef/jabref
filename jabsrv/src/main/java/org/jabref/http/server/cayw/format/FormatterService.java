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
    }

    public void registerFormatter(CAYWFormatter formatter) {
        formatters.putIfAbsent(formatter.getFormatName(), formatter);
    }

    public CAYWFormatter getFormatter(CAYWQueryParams queryParams) {
        return formatters.getOrDefault(queryParams.getFormat().toLowerCase(), formatters.get(DEFAULT_FORMATTER));
    }
}
