package org.jabref.http.server.cayw.format;

import java.util.HashMap;
import java.util.Map;

import org.jabref.http.server.cayw.CAYWQueryParams;

import org.jvnet.hk2.annotations.Service;

@Service
public class FormatterService {

    private final Map<String, CAYWFormatter> formatters;

    public FormatterService() {
        this.formatters = new HashMap<>();
        registerFormatter(new SimpleJsonFormatter());
        registerFormatter(new BibLatexFormatter());
    }

    public void registerFormatter(CAYWFormatter formatter) {
        formatters.putIfAbsent(formatter.getFormatName(), formatter);
    }

    public CAYWFormatter getFormatter(CAYWQueryParams queryParams) throws IllegalArgumentException {
        return formatters.get(queryParams.getFormat().toLowerCase());
    }
}
