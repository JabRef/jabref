package org.jabref.http.server.cayw.format;

import java.util.HashMap;
import java.util.Map;

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

    public CAYWFormatter getFormatter(String formatName) throws IllegalArgumentException {
        CAYWFormatter formatter = formatters.get(formatName.toLowerCase());
        if (formatter == null) {
            throw new IllegalArgumentException("Unknown format: " + formatName);
        }
        return formatter;
    }
}
